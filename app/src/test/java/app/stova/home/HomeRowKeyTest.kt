package app.stova.home

import app.stova.profiles.HomePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRowKeyTest {
    private val j = HomeRowKey.continueWatching("j1")
    private val m = HomeRowKey.latestMovies("j1")

    @Test fun keysHaveStableWireNames() {
        assertEquals("continue:j1", j.value); assertEquals("latest_movies:j1", m.value)
        assertEquals("latest_series:j1", HomeRowKey.latestSeries("j1").value)
        assertEquals("favorites:j1", HomeRowKey.favorites("j1").value)
        assertEquals("most_played:j1", HomeRowKey.mostPlayed("j1").value)
        assertEquals("apps", HomeRowKey.APPS.value); assertEquals("watch_next", HomeRowKey.WATCH_NEXT.value)
    }

    @Test fun preferredOrderFirstThenDefaultsMinusHidden() = assertEquals(
        listOf(m, HomeRowKey.APPS),
        arrangeRows(listOf(HomeRowKey.APPS, j, m), HomePrefs(rowOrder = listOf(m, HomeRowKey("gone")), hiddenRows = setOf(j))),
    )
}
