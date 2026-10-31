package app.stova.apps

import app.stova.profiles.AppPrefs

/** Favourites with one saved shortcut, for tests that need no profile file. */
object AppPrefsFixture {
    fun withShortcut(key: String, shortcut: Shortcut) = AppPrefs(favorites = listOf(key), shortcuts = mapOf(key to shortcut))
}
