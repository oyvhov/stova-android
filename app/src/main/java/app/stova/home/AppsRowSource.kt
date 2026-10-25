package app.stova.home

import app.stova.apps.AppRepository
import app.stova.profiles.Profile
import app.stova.profiles.ProfileSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The apps row: the profile's favourites, or, until any are chosen, the installed apps with TV apps
 * first. Home is never without apps; choosing favourites narrows the row to them.
 */
class AppsRowSource(private val apps: AppRepository, private val session: ProfileSession) : HomeRowSource {
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> =
        apps.arranged(session.active).map { arranged ->
            when {
                arranged.favorites.isNotEmpty() -> listOf(HomeRowUi.Apps(HomeRowKey.APPS, arranged.favorites))
                arranged.all.isEmpty() -> emptyList()
                else -> listOf(HomeRowUi.Apps(HomeRowKey.APPS, arranged.all.sortedBy { !it.tv }, favorites = false))
            }
        }
}
