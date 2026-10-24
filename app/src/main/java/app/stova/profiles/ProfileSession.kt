package app.stova.profiles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Who is watching. `active == null` means the profile gate is showing. */
class ProfileSession(private val repo: ProfileRepository, scope: CoroutineScope) {
    private var exitAuthorizedAdultId: String? = null
    private val activeId = MutableStateFlow<String?>(null)
    private val _start = MutableStateFlow<StartDecision?>(null)

    /** Follows the file, so an edited profile updates everywhere at once. */
    val active: StateFlow<Profile?> = combine(repo.file, activeId) { file, id ->
        id?.let { wanted -> file.profiles.firstOrNull { it.id == wanted } }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Worked out once per cold start; [leave] turns it into the picker. */
    val start: StateFlow<StartDecision?> = _start.asStateFlow()

    init {
        scope.launch { _start.value = decideStart(repo.current()) }
    }

    /** Only the adult whose PIN unlocked the kid can skip a second prompt in this picker. */
    fun needsPin(profile: Profile): Boolean = profile.pin != null && profile.id != exitAuthorizedAdultId

    suspend fun enter(profileId: String) {
        exitAuthorizedAdultId = null
        activeId.value = profileId
        repo.rememberLast(profileId)
    }

    fun leave(authorizedAdultId: String? = null) {
        exitAuthorizedAdultId = authorizedAdultId
        activeId.value = null
        _start.value = StartDecision.Pick
    }
}
