package app.stova.media

import app.stova.media.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class SpotlightTest {
    private val acc = MediaAccount("a1", "p", ServerKind.JELLYFIN, "srv", "Heim", "http://jf:8096", "u1", "oyvind")
    private val today = LocalDate.of(2026, 10, 25)
    private fun movie(id: String, backdrop: String? = "b-$id", logo: String? = "l-$id") =
        MediaItem(id, "a1", ItemType.MOVIE, "Film $id", year = 2024, art = ItemArt(backdrop = backdrop, logo = logo))
    private fun season(series: String, n: Int, premiered: String) = MediaItem("se-$series-$n", "a1", ItemType.SEASON, "Sesong $n",
        seriesName = "Serie $series", seriesId = series, season = n, premiered = premiered, art = ItemArt(backdrop = "b-$series", logo = "l-$series"))

    @Test fun newSeasonsLeadAndShowTheSeriesNotTheSeason() {
        val entries = spotlightEntries(acc, listOf(movie("m1")), listOf(season("s1", 3, "2026-10-10")), today)
        assertEquals(listOf("s1", "m1"), entries.map { it.item.id })
        assertTrue(entries[0].newSeason); assertFalse(entries[1].newSeason)
        assertEquals("Serie s1", entries[0].item.title); assertEquals(ItemType.SERIES, entries[0].item.type)
        assertNull(entries[0].item.season)
    }

    @Test fun firstSeasonsOldSeasonsAndUnairedSeasonsAreNotNew() {
        val seasons = listOf(season("s1", 1, "2026-10-10"), season("s2", 2, "2026-08-01"), season("s3", 4, "2026-11-20"))
        assertTrue(spotlightEntries(acc, emptyList(), seasons, today).isEmpty())
    }

    @Test fun onlyFilmsAndSeriesWithABackdropAndThoseWithALogoFirst() {
        val pool = listOf(movie("noart", backdrop = null), movie("nologo", logo = null), movie("full"),
            MediaItem("ep", "a1", ItemType.EPISODE, "Ep", art = ItemArt(backdrop = "b", logo = "l")))
        assertEquals(listOf("full", "nologo"), spotlightEntries(acc, pool, emptyList(), today).map { it.item.id })
    }

    @Test fun aSeriesWithANewSeasonIsNotRepeatedFromThePool() {
        val series = MediaItem("s1", "a1", ItemType.SERIES, "Serie s1", art = ItemArt(backdrop = "b-s1", logo = "l-s1"))
        assertEquals(listOf("s1"), spotlightEntries(acc, listOf(series), listOf(season("s1", 2, "2026-10-01")), today).map { it.item.id })
    }

    @Test fun keepsAtMostThirty() = assertEquals(30, spotlightEntries(acc, (1..40).map { movie("m$it") }, emptyList(), today).size)
}
