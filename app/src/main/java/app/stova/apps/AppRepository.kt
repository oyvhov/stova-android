package app.stova.apps

import app.stova.profiles.AppPrefs
import app.stova.profiles.Profile
import app.stova.profiles.ProfileRepository
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AppRepository(
    private val catalog: AppCatalog,
    private val profiles: ProfileRepository,
    private val scope: CoroutineScope,
) {
    /** Re-scans whenever a package is added, removed or changed. */
    private val installed: Flow<List<InstalledApp>> = catalog.changes().onStart { emit(Unit) }.map { catalog.installed() }

    fun arranged(profile: Flow<Profile?>): Flow<ArrangedApps> =
        combine(profile, installed) { p, apps -> p to apps }
            .onEach { (p, apps) -> if (p != null) pruneIfMissing(p, apps) }
            .map { (p, apps) ->
                if (p == null) ArrangedApps()
                else arrangeApps(apps, p.apps, p.kid, Collator.getInstance(Locale.getDefault()))
            }

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

    /** Drops keys of apps that are gone from favourites, hidden and the kid allow-list. */
    suspend fun pruneMissing(profileId: String, installedKeys: Set<String>) = updatePrefs(profileId) { prefs ->
        prefs.copy(
            favorites = prefs.favorites.filter { it in installedKeys },
            hidden = prefs.hidden.filterTo(mutableSetOf()) { it in installedKeys },
            kidAllowed = prefs.kidAllowed.filterTo(mutableSetOf()) { it in installedKeys },
        )
    }

    /**
     * An empty scan means the package manager was not ready (early boot), not that every app was
     * uninstalled; pruning then would wipe every profile's favourites.
     */
    private fun pruneIfMissing(profile: Profile, apps: List<InstalledApp>) {
        if (apps.isEmpty()) return
        val keys = apps.mapTo(mutableSetOf()) { it.key }
        val referenced = profile.apps.favorites + profile.apps.hidden + profile.apps.kidAllowed
        if (referenced.all { it in keys }) return
        scope.launch { pruneMissing(profile.id, keys) }
    }

    private suspend fun updatePrefs(profileId: String, change: (AppPrefs) -> AppPrefs) {
        profiles.update(profileId) { it.copy(apps = change(it.apps)) }
    }
}
