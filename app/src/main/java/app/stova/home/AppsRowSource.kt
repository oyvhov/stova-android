package app.stova.home

import app.stova.apps.AppRepository
import app.stova.apps.favoriteTiles
import app.stova.media.model.MediaAccount
import app.stova.profiles.Profile
import app.stova.profiles.ProfileSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * The apps row: the profile's favourites, or, until any are chosen, the installed apps with TV apps
 * first. Home is never without apps; choosing favourites narrows the row to them. Favourite
 * libraries and scenes sit among the apps; they need only the saved profile, the profile's
 * accounts and whether Home Assistant is set up, never the network.
 */
class AppsRowSource(
    private val apps: AppRepository,
    private val session: ProfileSession,
    private val accounts: (String) -> Flow<List<MediaAccount>> = { flowOf(emptyList()) },
    private val haReady: Flow<Boolean> = flowOf(false),
) : HomeRowSource {
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> =
        combine(apps.arranged(session.active), session.active.map { it?.apps to (it?.kid ?: false) }, accounts(profile.id), haReady) {
                arranged, (prefs, kid), owned, ha ->
            val tiles = prefs?.let { favoriteTiles(it, arranged.favorites, owned, ha, kid) }.orEmpty()
            when {
                tiles.isNotEmpty() -> listOf(HomeRowUi.Apps(HomeRowKey.APPS, arranged.favorites, tiles = tiles))
                arranged.all.isEmpty() -> emptyList()
                else -> listOf(HomeRowUi.Apps(HomeRowKey.APPS, arranged.all.sortedBy { !it.tv }, favorites = false))
            }
        }
}
