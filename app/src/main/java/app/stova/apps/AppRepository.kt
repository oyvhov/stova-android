package app.stova.apps

import app.stova.profiles.AppPrefs
import app.stova.profiles.Profile
import app.stova.profiles.ProfileRepository
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class AppRepository(
    private val catalog: AppCatalog,
    private val profiles: ProfileRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * One scan, shared: the Home row, every Apps page and anyone else collect the same list and the
     * same package callback, and a page that opens again gets the last scan at once. Re-scans
     * whenever a package is added, removed or changed.
     */
    val packageChanges = catalog.changes().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 0)
    private val installed: Flow<List<InstalledApp>> = packageChanges.onStart { emit(Unit) }.map { catalog.installed() }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** All an arrangement depends on; an appearance edit or any other profile change never re-sorts. */
    private data class Arrangement(val id: String, val prefs: AppPrefs, val kid: Boolean)

    fun arranged(profile: Flow<Profile?>): Flow<ArrangedApps> =
        combine(profile.map { p -> p?.let { Arrangement(it.id, it.apps, it.kid) } }.distinctUntilChanged(), installed) { p, apps -> p to apps }
            .onEach { (p, apps) -> if (p != null) reconcileIfNeeded(p, apps) }
            .map { (p, apps) ->
                if (p == null) ArrangedApps()
                else arrangeApps(apps, p.prefs, p.kid, Collator.getInstance(Locale.getDefault()))
            }

    /**
     * For a menu that closes as it acts: the change runs in the app scope, because the menu's own
     * scope ends with the frame that removes it, often while the write is still on disk.
     */
    fun launchChange(change: suspend AppRepository.() -> Unit) { scope.launch { change() } }

    suspend fun toggleFavorite(profileId: String, key: String) = updatePrefs(profileId) { prefs ->
        prefs.copy(favorites = if (key in prefs.favorites) prefs.favorites - key else prefs.favorites + key)
    }

    suspend fun moveFavorite(profileId: String, key: String, delta: Int) = updatePrefs(profileId) { prefs ->
        prefs.copy(favorites = moveFavorite(prefs.favorites, key, delta))
    }

    suspend fun setHidden(profileId: String, key: String, hidden: Boolean) = updatePrefs(profileId) { prefs ->
        prefs.copy(hidden = if (hidden) prefs.hidden + key else prefs.hidden - key)
    }

    suspend fun setKidAllowed(profileId: String, key: String, allowed: Boolean) = updatePrefs(profileId) { prefs ->
        prefs.copy(kidAllowed = if (allowed) prefs.kidAllowed + key else prefs.kidAllowed - key)
    }

    suspend fun setIcon(profileId: String, key: String, icon: IconOverride?) = updatePrefs(profileId) { prefs ->
        prefs.copy(iconOverrides = if (icon == null) prefs.iconOverrides - key else prefs.iconOverrides + (key to icon))
    }

    /** Drops keys of apps that are gone from favourites, hidden and the kid allow-list. */
    suspend fun pruneMissing(profileId: String, installedKeys: Set<String>) = updatePrefs(profileId) { prefs ->
        prefs.copy(
            favorites = prefs.favorites.filter { it in installedKeys },
            hidden = prefs.hidden.filterTo(mutableSetOf()) { it in installedKeys },
            kidAllowed = prefs.kidAllowed.filterTo(mutableSetOf()) { it in installedKeys },
            iconOverrides = prefs.iconOverrides.filterKeys { it in installedKeys },
        )
    }

    /**
     * An empty scan means the package manager was not ready (early boot), not that every app was
     * uninstalled. Otherwise each stored key a scan did not return is followed (renamed activity),
     * kept (package installed but unavailable) or dropped (package uninstalled); see [reconcileKeys].
     */
    private fun reconcileIfNeeded(profile: Arrangement, apps: List<InstalledApp>) {
        if (apps.isEmpty()) return
        val keys = apps.mapTo(mutableSetOf()) { it.key }
        val referenced = profile.prefs.favorites + profile.prefs.hidden + profile.prefs.kidAllowed + profile.prefs.iconOverrides.keys
        if (referenced.all { it in keys }) return
        scope.launch {
            val mapping = withContext(io) { reconcileKeys(referenced.toSet(), apps, catalog::isInstalled) }
            if (mapping.all { (old, new) -> old == new }) return@launch
            fun List<String>.mapped() = mapNotNull { if (it in mapping) mapping[it] else it }.distinct()
            updatePrefs(profile.id) { prefs ->
                prefs.copy(
                    favorites = prefs.favorites.mapped(),
                    hidden = prefs.hidden.toList().mapped().toSet(),
                    kidAllowed = prefs.kidAllowed.toList().mapped().toSet(),
                    iconOverrides = prefs.iconOverrides.entries.mapNotNull { (old, icon) ->
                        (if (old in mapping) mapping[old] else old)?.let { it to icon }
                    }.toMap(),
                )
            }
        }
    }

    private suspend fun updatePrefs(profileId: String, change: (AppPrefs) -> AppPrefs) {
        profiles.update(profileId) { it.copy(apps = change(it.apps)) }
    }
}
