package app.stova.home

import app.stova.profiles.HomePrefs
import kotlinx.serialization.Serializable

/** Stable identity of a home row, stored in a profile's row order and hidden set. */
@Serializable
@JvmInline
value class HomeRowKey(val value: String) {
    companion object {
        val APPS = HomeRowKey("apps")
        val WATCH_NEXT = HomeRowKey("watch_next")
        fun continueWatching(accountId: String) = HomeRowKey("continue:$accountId")
        fun latestMovies(accountId: String) = HomeRowKey("latest_movies:$accountId")
        fun latestSeries(accountId: String) = HomeRowKey("latest_series:$accountId")
    }
}

/** The profile's own order first, then the rest in their default order; hidden rows are left out. */
fun arrangeRows(available: List<HomeRowKey>, prefs: HomePrefs): List<HomeRowKey> {
    val present = available.toSet()
    val preferred = prefs.rowOrder.filter { it in present }
    val rest = available.filterNot { it in preferred.toSet() }
    return (preferred + rest).filterNot { it in prefs.hiddenRows }
}
