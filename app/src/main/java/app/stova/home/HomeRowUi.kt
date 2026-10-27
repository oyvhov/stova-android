package app.stova.home

import app.stova.apps.InstalledApp
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.Flow

/** One row on the home screen. Later plans add media, Watch Next and smart-home variants. */
sealed interface HomeRowUi {
    val key: HomeRowKey

    data class WatchNext(override val key: HomeRowKey = HomeRowKey.WATCH_NEXT, val entries: List<app.stova.watchnext.WatchNextEntry>) : HomeRowUi

    data class Media(override val key: HomeRowKey, val account: app.stova.media.model.MediaAccount, val kind: app.stova.media.MediaRowKind, val items: List<app.stova.media.model.MediaItem>, val offline: Boolean) : HomeRowUi

    /**
     * [favorites] false: no favourites chosen yet, so the row shows the installed TV apps. [tiles] is
     * what the row draws, in order: the apps, and any favourite libraries and scenes among them.
     */
    data class Apps(override val key: HomeRowKey, val apps: List<InstalledApp>, val favorites: Boolean = true,
        val tiles: List<app.stova.apps.FavoriteTile> = apps.map { app.stova.apps.FavoriteTile.App(it) }) : HomeRowUi
}

/** Anything that contributes rows. Called again only when the active profile changes. */
fun interface HomeRowSource {
    fun rows(profile: Profile): Flow<List<HomeRowUi>>
    /** True only for sources whose rows depend on the season (media); the rest start once per profile. */
    val followsSeason: Boolean get() = false
}
