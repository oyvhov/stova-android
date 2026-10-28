package app.stova.apps

import app.stova.media.model.MediaAccount
import app.stova.profiles.AppPrefs

/** One tile in the favourites row: an app, a media library that opens in Spole, or a Home Assistant scene. */
sealed interface FavoriteTile {
    val key: String
    val label: String

    data class App(val app: InstalledApp) : FavoriteTile {
        override val key get() = app.key
        override val label get() = app.label
    }

    data class Library(override val key: String, override val label: String, val image: String?,
        val account: MediaAccount, val libraryId: String) : FavoriteTile

    /** [icon] is Home Assistant's own, such as `mdi:movie-open`. */
    data class Scene(override val key: String, override val label: String, val entityId: String, val icon: String? = null) : FavoriteTile
}

/**
 * The favourites in the profile's order. [apps] are the favourite apps this profile may see. A
 * library shows while its account is on the profile, a scene while Home Assistant is set up, and
 * neither for a kid.
 */
fun favoriteTiles(prefs: AppPrefs, apps: List<InstalledApp>, accounts: List<MediaAccount>, haReady: Boolean, kid: Boolean): List<FavoriteTile> {
    val appsByKey = apps.associateBy { it.key }
    val accountsById = accounts.associateBy { it.id }
    return prefs.favorites.mapNotNull { key ->
        appsByKey[key]?.let { return@mapNotNull FavoriteTile.App(it) }
        val saved = prefs.shortcuts[key]?.takeIf { !kid } ?: return@mapNotNull null
        when (val shortcut = ShortcutKey.parse(key)) {
            is ShortcutKey.Library -> accountsById[shortcut.accountId]?.let { FavoriteTile.Library(key, saved.label, saved.image, it, shortcut.libraryId) }
            is ShortcutKey.Ha -> if (haReady) FavoriteTile.Scene(key, saved.label, shortcut.entityId, saved.chosenIcon ?: saved.icon) else null
            null -> null
        }
    }
}
