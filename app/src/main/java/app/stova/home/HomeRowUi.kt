package app.stova.home

import app.stova.apps.InstalledApp
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.Flow

/** One row on the home screen. Later plans add media, Watch Next and smart-home variants. */
sealed interface HomeRowUi {
    val key: HomeRowKey

    data class Media(override val key: HomeRowKey, val account: app.stova.media.model.MediaAccount, val kind: app.stova.media.MediaRowKind, val items: List<app.stova.media.model.MediaItem>, val offline: Boolean) : HomeRowUi

    data class Apps(override val key: HomeRowKey, val apps: List<InstalledApp>) : HomeRowUi
}

/** Anything that contributes rows. Called again only when the active profile changes. */
fun interface HomeRowSource {
    fun rows(profile: Profile): Flow<List<HomeRowUi>>
}
