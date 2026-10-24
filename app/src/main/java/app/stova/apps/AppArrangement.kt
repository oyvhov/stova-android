package app.stova.apps

import app.stova.profiles.AppPrefs
import java.text.Collator

data class ArrangedApps(
    val favorites: List<InstalledApp> = emptyList(),
    val all: List<InstalledApp> = emptyList(),
    val hidden: List<InstalledApp> = emptyList(),
)

/**
 * Favourites in the profile's order, everything visible in the language's alphabetical order, and
 * hidden apps on their own. A kid sees only what an adult allowed, and has no hidden group.
 */
fun arrangeApps(installed: List<InstalledApp>, prefs: AppPrefs, kid: Boolean, collator: Collator): ArrangedApps {
    val visible = if (kid) installed.filter { it.key in prefs.kidAllowed } else installed
    val byKey = visible.associateBy { it.key }
    val sorted = visible.sortedWith { x, y -> collator.compare(x.label, y.label) }
    val favorites = prefs.favorites.mapNotNull { byKey[it] }
    return if (kid) {
        ArrangedApps(favorites, sorted, emptyList())
    } else {
        ArrangedApps(favorites, sorted.filterNot { it.key in prefs.hidden }, sorted.filter { it.key in prefs.hidden })
    }
}

/** Moves [key] by [delta] places, stopping at either end. */
fun moveFavorite(favorites: List<String>, key: String, delta: Int): List<String> {
    val from = favorites.indexOf(key)
    if (from < 0) return favorites
    val to = (from + delta).coerceIn(0, favorites.lastIndex)
    return favorites.toMutableList().apply { add(to, removeAt(from)) }
}
