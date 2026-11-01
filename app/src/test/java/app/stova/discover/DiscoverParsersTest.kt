package app.stova.discover

import android.database.MatrixCursor
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.media.SpoleContract
import app.stova.media.model.ServerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** What Spole's launcher provider answers for Discover, read into Stova's models. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class DiscoverParsersTest {
    private fun items(vararg rows: Array<Any?>) = MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop",
        "status", "community", "genres")).apply { rows.forEach(::addRow) }
    private fun card(row: String, type: String, tmdb: Any?, title: String, status: Any? = 1, community: Any? = null) = arrayOf<Any?>(row, type,
        tmdb, title, 2021, "Om $title.", "http://p/$tmdb", "http://b/$tmdb", status, community, "Action, Drama")
    private fun withExtras(cursor: MatrixCursor, build: Bundle.() -> Unit) = cursor.apply { setExtras(Bundle().apply(build)) }

    @Test fun rowsAreParsedInFixedOrderAndDeduped() {
        val rows = parseDiscoverRows(items(
            card("recommended", "tv", 1399, "Game of Thrones"),
            card("upcoming_series", "tv", 7, "Seven"),
            // Numbers can arrive as text; a duplicate, a zero, a non-number and an unknown type are left out.
            card("trending", "movie", "603", "The Matrix", status = "5", community = "7.8"),
            card("trending", "movie", 603, "The Matrix again"),
            card("trending", "movie", 0, "No id"),
            card("trending", "movie", "abc", "Not a number"),
            card("trending", "tv", 603, "Same id, other type"),
            card("trending", "short", 8, "Unknown type"),
            card("upcoming_movies", "movie", 11, "Star Wars"),
            // The same title may stand in another row, a row with nothing valid is no row, and a row Stova does not know is skipped.
            card("recommended", "movie", 603, "The Matrix"),
            card("similar", "movie", 0, "Zero only"),
            card("bogus", "movie", 5, "Unknown row")))
        assertEquals(listOf(DiscoverRowKind.TRENDING, DiscoverRowKind.UPCOMING_MOVIES, DiscoverRowKind.UPCOMING_SERIES, DiscoverRowKind.RECOMMENDED),
            rows.map { it.kind })
        val trending = rows.first().items
        assertEquals(listOf("movie-603", "tv-603"), trending.map { it.key })
        val matrix = trending.first()
        assertEquals("The Matrix", matrix.title)
        assertEquals(DiscoverType.MOVIE, matrix.type)
        assertEquals(603, matrix.tmdbId)
        assertEquals(2021, matrix.year)
        assertEquals("Om The Matrix.", matrix.overview)
        assertEquals("http://p/603", matrix.poster)
        assertEquals("http://b/603", matrix.backdrop)
        assertEquals(SeerrStatus.AVAILABLE, matrix.status)
        assertEquals(7.8, matrix.community!!, .001)
        assertEquals(listOf("Action", "Drama"), matrix.genres)
        assertEquals(SeerrStatus.UNKNOWN, trending.last().status)
        assertNull(trending.last().community)
        assertEquals(listOf("tv-1399", "movie-603"), rows.last().items.map { it.key })
        assertEquals(emptyList<DiscoverRow>(), parseDiscoverRows(items()))
    }

    @Test fun missingSeerrExtraMeansNoAnswer() {
        // An older Spole answers nothing of its own: no extra is «not answered», never «no access».
        assertNull(parseSeerrAccess(items()))
        assertEquals(SeerrAccess.BROWSE, parseSeerrAccess(withExtras(items()) { putString("seerr", "browse") }))
        assertEquals(SeerrAccess.PERSONAL, parseSeerrAccess(withExtras(items()) { putString("seerr", "personal") }))
        assertEquals(SeerrAccess.NONE, parseSeerrAccess(withExtras(items()) { putString("seerr", "none") }))
        assertNull(parseSeerrAccess(withExtras(items()) { putString("seerr", "everything") }))
    }

    @Test fun titleCarriesRequestStateAndSeasons() {
        val seasons = parseDiscoverSeasons(MatrixCursor(arrayOf("number", "name", "episodes", "status", "air_date")).apply {
            addRow(arrayOf<Any?>(0, "Specials", 3, 1, null))
            addRow(arrayOf<Any?>(1, "Season 1", 10, 5, "2011-04-17"))
            addRow(arrayOf<Any?>("2", "Season 2", "8", "1", null))
            addRow(arrayOf<Any?>("x", "Not a season", 1, 1, null))
        })
        assertEquals(listOf(0, 1, 2), seasons.map { it.number })
        assertEquals(listOf("Specials", "Season 1", "Season 2"), seasons.map { it.name })
        assertEquals(listOf(3, 10, 8), seasons.map { it.episodes })
        assertEquals(listOf(SeerrStatus.UNKNOWN, SeerrStatus.AVAILABLE, SeerrStatus.UNKNOWN), seasons.map { it.status })
        assertEquals("2011-04-17", seasons[1].airDate)
        assertNull(seasons[0].airDate)
        // A season already in the library is not asked for again.
        assertFalse(seasons[1].requestable)
        assertTrue(seasons[2].requestable)

        val titleColumns = arrayOf("type", "tmdb", "title", "year", "tagline", "overview", "poster", "backdrop", "runtime", "genres", "community",
            "critic", "status", "can_request", "request_id", "request_status", "trailer")
        fun title(vararg row: Any?) = MatrixCursor(titleColumns).apply { addRow(row) }
        val details = parseDiscoverTitle(title("tv", 1399, "Game of Thrones", 2011, "Winter is coming", "Nine noble families.", "http://p", "http://b",
            57, "Drama, Fantasy", 8.4, 89, 3, 1, 42, "downloading", "http://trailer"), seasons)!!
        assertEquals("tv-1399", details.title.key)
        assertEquals(DiscoverType.SERIES, details.title.type)
        assertEquals("Game of Thrones", details.title.title)
        assertEquals(SeerrStatus.PROCESSING, details.title.status)
        assertEquals(listOf("Drama", "Fantasy"), details.title.genres)
        assertEquals(8.4, details.title.community!!, .001)
        assertEquals("Winter is coming", details.tagline)
        assertEquals(57, details.runtimeMinutes)
        assertEquals(89, details.critic)
        assertEquals("http://trailer", details.trailer)
        assertTrue(details.canRequest)
        assertEquals(42, details.requestId)
        assertEquals(RequestStage.DOWNLOADING, details.requestStage)
        assertTrue(details.withdrawable)
        // The Specials (season 0) and the season in the library are not offered; season 2 is.
        assertEquals(listOf(2), details.requestableSeasons.map { it.number })

        // No request of the person's own: nothing to withdraw, and a runtime Spole does not send is none.
        val bare = parseDiscoverTitle(title("movie", 603, "The Matrix", null, null, null, null, null, null, "", null, null, 1, 0, null, null, null),
            emptyList())!!
        assertFalse(bare.canRequest)
        assertNull(bare.requestId)
        assertNull(bare.requestStage)
        assertFalse(bare.withdrawable)
        assertNull(bare.runtimeMinutes)
        assertEquals(emptyList<String>(), bare.title.genres)
        // A request already declined cannot be withdrawn.
        val declined = parseDiscoverTitle(title("movie", 603, "The Matrix", null, null, null, null, null, null, "", null, null, 1, 1, 5, "declined", null),
            emptyList())!!
        assertEquals(RequestStage.DECLINED, declined.requestStage)
        assertFalse(declined.withdrawable)
        // No row, or a row with no title Stova could open, is no title.
        assertNull(parseDiscoverTitle(MatrixCursor(titleColumns), emptyList()))
        assertNull(parseDiscoverTitle(title("movie", 0, "No id", null, null, null, null, null, null, "", null, null, 1, 0, null, null, null), emptyList()))
    }

    @Test fun requestsAndPagesAreParsed() {
        val requests = parseDiscoverRequests(MatrixCursor(arrayOf("type", "tmdb", "title", "poster", "backdrop", "stage", "percent", "seasons",
            "request_id", "is4k")).apply {
            addRow(arrayOf<Any?>("tv", 1399, "Game of Thrones", "http://p/1399", null, "importing", 40, "1,2", 9, 1))
            addRow(arrayOf<Any?>("movie", "603", "The Matrix", null, "http://b/603", "requested", null, "", null, 0))
            addRow(arrayOf<Any?>("movie", 0, "No id", null, null, "requested", null, "", 3, 0))
            addRow(arrayOf<Any?>("movie", 11, "Star Wars", null, null, "somethingnew", null, null, 4, 0))
        })
        assertEquals(listOf("Game of Thrones", "The Matrix", "Star Wars"), requests.map { it.title })
        val series = requests.first()
        assertEquals(DiscoverType.SERIES, series.type)
        assertEquals(1399, series.tmdbId)
        assertEquals("http://p/1399", series.poster)
        assertEquals(RequestStage.IMPORTING, series.stage)
        assertEquals(40, series.percent)
        assertEquals(listOf(1, 2), series.seasons)
        assertEquals(9, series.requestId)
        assertTrue(series.is4k)
        val film = requests[1]
        assertEquals(603, film.tmdbId)
        assertEquals("http://b/603", film.backdrop)
        assertEquals(RequestStage.REQUESTED, film.stage)
        assertNull(film.percent)
        assertEquals(emptyList<Int>(), film.seasons)
        assertNull(film.requestId)
        assertFalse(film.is4k)
        // A stage Stova does not know is «unknown», not a lost request.
        assertEquals(RequestStage.UNKNOWN, requests.last().stage)
        assertEquals(emptyList<Int>(), requests.last().seasons)

        val page = parseDiscoverPage(withExtras(items(card("search", "movie", 603, "The Matrix"), card("search", "tv", 603, "The Matrix"),
            card("search", "movie", 603, "The Matrix"))) { putInt("page", 2); putBoolean("has_more", true) })
        assertEquals(2, page.page)
        assertTrue(page.hasMore)
        assertEquals(listOf("movie-603", "tv-603"), page.items.map { it.key })
        // An answer without extras is the first page and the last.
        val plain = parseDiscoverPage(items(card("search", "movie", 603, "The Matrix")))
        assertEquals(1, plain.page)
        assertFalse(plain.hasMore)
        assertEquals(1, plain.items.size)

        assertEquals(DiscoverActionResult.DONE, discoverActionResult("ok"))
        assertEquals(DiscoverActionResult.ALREADY, discoverActionResult("already"))
        assertEquals(DiscoverActionResult.NOT_PERSONAL, discoverActionResult("not_personal"))
        assertEquals(DiscoverActionResult.FORBIDDEN, discoverActionResult("forbidden"))
        assertEquals(DiscoverActionResult.FAILED, discoverActionResult("failed"))
        assertEquals(DiscoverActionResult.UNKNOWN, discoverActionResult("unknown"))
        // No answer at all, or a word from a newer Spole, is a Spole that cannot do it.
        assertEquals(DiscoverActionResult.NOT_SUPPORTED, discoverActionResult(null))
        assertEquals(DiscoverActionResult.NOT_SUPPORTED, discoverActionResult("later"))
    }

    @Test fun discoverUriHasTypeAndTmdb() {
        assertEquals("spole://discover?kind=jellyfin&user=u1&type=movie&tmdb=603",
            SpoleContract.discoverUri(ServerKind.JELLYFIN, "u1", DiscoverType.MOVIE, 603).toString())
        assertEquals("spole://discover?kind=emby&user=eilev&type=tv&tmdb=1399",
            SpoleContract.discoverUri(ServerKind.EMBY, "eilev", DiscoverType.SERIES, 1399).toString())
        assertEquals("seerr", SpoleContract.SEERR_SERVER)
    }
}
