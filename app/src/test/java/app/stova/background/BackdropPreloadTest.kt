package app.stova.background

import app.stova.home.*
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import org.junit.Assert.*
import org.junit.Test

class BackdropPreloadTest {
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private fun row(id: String, vararg urls: String?) = HomeRowUi.Media(HomeRowKey(id), account, MediaRowKind.CONTINUE,
        urls.mapIndexed { i, url -> MediaItem("$id-$i", "a", ItemType.MOVIE, "Demo", art = ItemArt(backdrop = url)) }, false)

    @Test fun onlyFirstThreeTitlesInEachRowAreCandidates() {
        assertEquals(listOf("a", "b", "c", "e", "f", "g"), backdropPreloadUrls(listOf(
            row("1", "a", "b", "c", "d"), row("2", "e", "f", "g", "h"))))
    }
    @Test fun duplicatesAndMissingArtDoNotPullLaterTitlesIntoTheSelection() {
        assertEquals(listOf("a", "b"), backdropPreloadUrls(listOf(
            row("1", "a", "a", null, "outside-first-three"), row("2", "a", "", "b"))))
    }
    @Test fun cappedAtTwelveAcrossAllRowsInDisplayOrder() {
        val rows = (0..5).map { n -> row("$n", "$n-a", "$n-b", "$n-c", "$n-d") }
        assertEquals((0..3).flatMap { n -> listOf("$n-a", "$n-b", "$n-c") }, backdropPreloadUrls(rows))
    }
    @Test fun emptyAndNonMediaRowsAreIgnored() {
        assertTrue(backdropPreloadUrls(emptyList()).isEmpty())
        assertEquals(listOf("a"), backdropPreloadUrls(listOf(HomeRowUi.Apps(HomeRowKey.APPS, emptyList()),
            HomeRowUi.WatchNext(entries = emptyList()), row("empty"), row("media", "a"))))
    }
}
