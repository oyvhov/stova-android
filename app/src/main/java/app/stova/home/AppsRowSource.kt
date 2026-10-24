package app.stova.home

import app.stova.apps.AppRepository
import app.stova.profiles.Profile
import app.stova.profiles.ProfileSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The favourite apps row; absent while the profile has no favourites. */
class AppsRowSource(private val apps: AppRepository, private val session: ProfileSession) : HomeRowSource {
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> =
        apps.arranged(session.active).map { arranged ->
            if (arranged.favorites.isEmpty()) emptyList() else listOf(HomeRowUi.Apps(HomeRowKey.APPS, arranged.favorites))
        }
}
