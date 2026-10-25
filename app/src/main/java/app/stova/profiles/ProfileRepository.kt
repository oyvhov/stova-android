package app.stova.profiles

import app.stova.core.security.PinHasher
import app.stova.core.security.PinPolicy
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed interface PinCheck {
    data object Accepted : PinCheck
    data class Rejected(val waitMillis: Long) : PinCheck
    data class LockedOut(val remainingMillis: Long) : PinCheck
}

class ProfileRepository(
    private val store: JsonFileStore<ProfilesFile>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    /** 100 000 rounds of PBKDF2 take a noticeable moment on a TV CPU; never on the UI thread. */
    private val cpu: CoroutineDispatcher = Dispatchers.Default,
) {
    val file: Flow<ProfilesFile> = store.data

    suspend fun current(): ProfilesFile = store.current()

    suspend fun create(name: String, avatar: AvatarColor, kid: Boolean): Profile {
        val clean = name.trim()
        require(clean.length in 1..MAX_NAME) { "name must be 1–$MAX_NAME characters" }
        lateinit var created: Profile
        store.update { file ->
            require(!(kid && file.profiles.isEmpty())) { "the first profile must be an adult" }
            created = Profile(newId(), clean, avatar, kid = kid, kidSmartHomeControl = false)
            file.copy(profiles = file.profiles + created)
        }
        return created
    }

    suspend fun update(id: String, transform: (Profile) -> Profile): Profile {
        lateinit var updated: Profile
        store.update { file ->
            val existing = file.profiles.firstOrNull { it.id == id } ?: throw NoSuchElementException(id)
            val changed = transform(existing)
            updated = changed.copy(id = id, appearance = changed.appearance.withoutCalendarSeason())
            file.copy(profiles = file.profiles.map { if (it.id == id) updated else it })
        }
        return updated
    }

    suspend fun delete(id: String) {
        store.update { file ->
            val remaining = file.profiles.filterNot { it.id == id }
            check(remaining.any { !it.kid }) { "at least one adult profile must remain" }
            file.copy(
                profiles = remaining,
                lastProfileId = file.lastProfileId.takeUnless { it == id },
                pinFailures = file.pinFailures - id,
            )
        }
    }

    suspend fun setPin(id: String, pin: String?) {
        require(pin == null || PinHasher.isValid(pin)) { "PIN must be 4–8 digits" }
        val hash = pin?.let { withContext(cpu) { PinHasher.hash(it) } }
        store.update { file ->
            check(file.profiles.any { it.id == id }) { "no profile $id" }
            file.copy(
                profiles = file.profiles.map { if (it.id == id) it.copy(pin = hash) else it },
                pinFailures = file.pinFailures - id,
            )
        }
    }

    /** Counts a wrong PIN, refuses during a lockout without counting, and clears the count on success. */
    suspend fun verifyPin(id: String, pin: String): PinCheck {
        val profile = store.current().profiles.firstOrNull { it.id == id } ?: throw NoSuchElementException(id)
        val stored = profile.pin ?: return PinCheck.Accepted
        return check(id, listOf(stored), pin)
    }

    /**
     * Leaving a kid profile: any adult's PIN will do, and wrong guesses count against the kid exit
     * itself, never against one adult's own profile.
     */
    suspend fun verifyAnyAdultPin(pin: String, onVerified: (String) -> Unit = {}): PinCheck {
        val adults = store.current().profiles.filter { !it.kid && it.pin != null }
        val hashes = adults.mapNotNull { it.pin }
        if (hashes.isEmpty()) return PinCheck.Accepted
        return check(KID_EXIT, hashes, pin) { hash -> onVerified(adults.first { it.pin == hash }.id) }
    }

    /** Counts a wrong PIN under [key], refuses during a lockout without counting, clears on success. */
    private suspend fun check(key: String, hashes: List<app.stova.core.security.PinHash>, pin: String, onVerified: (app.stova.core.security.PinHash) -> Unit = {}): PinCheck {
        val now = clock()
        val failures = store.current().pinFailures[key] ?: PinFailures()
        // Persist wall time across restarts, but rebase a future timestamp after a clock reset.
        if (failures.lastFailureAt > now) {
            store.update { it.copy(pinFailures = it.pinFailures + (key to failures.copy(lastFailureAt = now))) }
        }
        val elapsed = (now - failures.lastFailureAt).coerceAtLeast(0)
        val lockout = PinPolicy.lockoutMillis(failures.count)
        if (failures.count > 0 && elapsed < lockout) return PinCheck.LockedOut(lockout - elapsed)
        val matched = withContext(cpu) { hashes.firstOrNull { PinHasher.verify(pin, it) } }
        return if (matched != null) {
            store.update { it.copy(pinFailures = it.pinFailures - key) }
            onVerified(matched)
            PinCheck.Accepted
        } else {
            val next = PinFailures(failures.count + 1, now)
            store.update { it.copy(pinFailures = it.pinFailures + (key to next)) }
            PinCheck.Rejected(PinPolicy.lockoutMillis(next.count))
        }
    }

    suspend fun rememberLast(id: String) {
        store.update { it.copy(lastProfileId = id) }
    }

    suspend fun setStartInLastProfile(enabled: Boolean) {
        store.update { it.copy(startInLastProfile = enabled) }
    }

    private companion object {
        const val MAX_NAME = 24
        const val KID_EXIT = "kid-exit"
    }
}
