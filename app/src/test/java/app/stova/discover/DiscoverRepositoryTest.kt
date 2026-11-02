package app.stova.discover

import app.stova.core.theme.AvatarColor
import app.stova.media.model.MediaAccount
import app.stova.media.model.ServerKind
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Discover's answers kept in memory: asked again only when old, kept when Spole trips, forgotten when a request changes a title. */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverRepositoryTest {
    private val link = SpoleLink(ServerKind.EMBY, "eilev")
    private fun title(tmdb: Int, type: DiscoverType = DiscoverType.MOVIE) =
        DiscoverTitle(type, tmdb, "T$tmdb", 2021, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    private val trending = listOf(DiscoverRow(DiscoverRowKind.TRENDING, listOf(title(603))))
    private val upcoming = listOf(DiscoverRow(DiscoverRowKind.UPCOMING_MOVIES, listOf(title(11))))
    private val matrix = DiscoverDetails(title(603), null, 136, 83, canRequest = true, requestId = null, requestStage = null, trailer = null,
        seasons = emptyList())

    /**
     * Spole as the repository sees it: [rows] and [requests] answer each ask, a title's questions throw while [trips],
     * and every ask is counted; [requestsAsked] holds whether each ask for the requests had Spole ask Seerr first.
     */
    private class FakeFetch : DiscoverFetch {
        var rows: suspend () -> DiscoverAnswer? = { null }
        var requests: suspend () -> List<DiscoverRequest>? = { emptyList() }
        var trips = false
        var result = DiscoverActionResult.DONE
        val details = mutableMapOf<Pair<DiscoverType, Int>, DiscoverDetails>()
        var rowsAsked = 0
        val requestsAsked = mutableListOf<Boolean>()
        var detailsAsked = 0
        var relatedAsked = 0
        override suspend fun rows(link: SpoleLink): DiscoverAnswer? { rowsAsked++; return rows() }
        override suspend fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>? { requestsAsked += refresh; return requests() }
        private fun trip() = check(!trips) { "Spole tripped" }
        override suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? {
            detailsAsked++; trip()
            return details[type to tmdbId]
        }
        override suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> {
            relatedAsked++; trip()
            return listOf(DiscoverRow(DiscoverRowKind.SIMILAR, listOf(DiscoverTitle(type, tmdbId + 1, "Like it", null, null, null, null,
                SeerrStatus.UNKNOWN, null, emptyList()))))
        }
        override suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? { trip(); return DiscoverPage(emptyList(), page, false) }
        override suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>) = result
        override suspend fun withdraw(link: SpoleLink, requestId: Int) = result
    }

    @Test fun refreshIsSkippedWhileFresh() = runTest {
        var time = 0L
        val gate = CompletableDeferred<Unit>()
        val fetch = FakeFetch().apply { rows = { gate.await(); DiscoverAnswer(SeerrAccess.PERSONAL, trending) } }
        val repository = DiscoverRepository(fetch, now = { time })
        // Home and the page asking at once are one question to Spole, and the state says it is under way.
        launch { repository.refresh("p1", link) }
        launch { repository.refresh("p1", link) }
        runCurrent()
        assertEquals(DiscoverState(SeerrAccess.NONE, emptyList(), emptyList(), loading = true, failed = false), repository.state("p1").value)
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, fetch.rowsAsked)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, trending, emptyList(), loading = false, failed = false), repository.state("p1").value)
        // Within ten minutes the last answer stands; forced, Spole is asked again.
        time = 9 * 60_000L
        repository.refresh("p1", link)
        assertEquals(1, fetch.rowsAsked)
        repository.refresh("p1", link, force = true)
        assertEquals(2, fetch.rowsAsked)
        // Ten minutes after the last answer, and for another profile, Spole is asked.
        time += 10 * 60_000L
        repository.refresh("p1", link)
        assertEquals(3, fetch.rowsAsked)
        repository.refresh("p2", link)
        assertEquals(4, fetch.rowsAsked)
    }

    @Test fun noAnswerMeansNoAccessAndNoRows() = runTest {
        val answers = ArrayDeque(listOf(DiscoverAnswer(SeerrAccess.PERSONAL, trending), null))
        val repository = DiscoverRepository(FakeFetch().apply { rows = { answers.removeFirst() } })
        assertNull(repository.state("p1").value)
        repository.refresh("p1", link)
        assertEquals(trending, repository.state("p1").value?.rows)
        // Spole gone, or one too old to answer: nothing to show, and nothing has gone wrong.
        repository.refresh("p1", link, force = true)
        assertEquals(DiscoverState(SeerrAccess.NONE, emptyList(), emptyList(), loading = false, failed = false), repository.state("p1").value)
    }

    @Test fun failureKeepsOldRowsAndFlags() = runTest {
        val fetch = FakeFetch().apply { rows = { DiscoverAnswer(SeerrAccess.PERSONAL, trending) } }
        val repository = DiscoverRepository(fetch)
        repository.refresh("p1", link)
        // Spole trips: the rows shown stay.
        fetch.rows = { throw IllegalStateException("Spole tripped") }
        repository.refresh("p1", link, force = true)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, trending, emptyList(), loading = false, failed = true), repository.state("p1").value)
        // A failed answer is not a fresh one: the next return asks again, without force. Seerr set up but giving no rows fails the same way.
        fetch.rows = { DiscoverAnswer(SeerrAccess.BROWSE, emptyList()) }
        repository.refresh("p1", link)
        assertEquals(3, fetch.rowsAsked)
        assertEquals(DiscoverState(SeerrAccess.BROWSE, trending, emptyList(), loading = false, failed = true), repository.state("p1").value)
        fetch.rows = { DiscoverAnswer(SeerrAccess.PERSONAL, upcoming) }
        repository.refresh("p1", link)
        assertEquals(4, fetch.rowsAsked)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, upcoming, emptyList(), loading = false, failed = false), repository.state("p1").value)
    }

    // The person's requests come with the rows. Spole asks Seerr for their state first on the first look, when
    // forced, and when the last look is ten minutes old; Spole tripping on them alone hides nothing.
    @Test fun requestsAreFetchedWithRowsAndRefreshedOnForce() = runTest {
        var time = 0L
        val gorge = DiscoverRequest(DiscoverType.MOVIE, 950396, "The Gorge", null, null, RequestStage.DOWNLOADING, 42, emptyList(), 7, is4k = false)
        val fetch = FakeFetch().apply { rows = { DiscoverAnswer(SeerrAccess.PERSONAL, trending) }; requests = { listOf(gorge) } }
        val repository = DiscoverRepository(fetch, now = { time })
        repository.refresh("p1", link)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, trending, listOf(gorge), loading = false, failed = false), repository.state("p1").value)
        assertEquals(listOf(true), fetch.requestsAsked)
        // While the rows are fresh nothing is asked; forced, Spole asks Seerr again and the new state replaces the old.
        time = 60_000L
        repository.refresh("p1", link)
        assertEquals(listOf(true), fetch.requestsAsked)
        val done = gorge.copy(stage = RequestStage.AVAILABLE, percent = null)
        fetch.requests = { listOf(done) }
        repository.refresh("p1", link, force = true)
        assertEquals(listOf(true, true), fetch.requestsAsked)
        assertEquals(listOf(done), repository.state("p1").value?.requests)
        // Spole trips on the rows: the requests shown stay, and a Spole that just failed is not asked for them.
        fetch.rows = { throw IllegalStateException("Spole tripped") }
        repository.refresh("p1", link, force = true)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, trending, listOf(done), loading = false, failed = true), repository.state("p1").value)
        assertEquals(listOf(true, true), fetch.requestsAsked)
        // The next return asks again; the requests were looked at a minute ago, so Spole answers with what it has.
        fetch.rows = { DiscoverAnswer(SeerrAccess.PERSONAL, trending) }
        time = 2 * 60_000L
        repository.refresh("p1", link)
        assertEquals(listOf(true, true, false), fetch.requestsAsked)
        // Spole trips on the requests alone: the rows come, nothing is said to have failed, and the requests shown stay.
        fetch.requests = { throw IllegalStateException("Spole tripped") }
        repository.refresh("p1", link, force = true)
        assertEquals(DiscoverState(SeerrAccess.PERSONAL, trending, listOf(done), loading = false, failed = false), repository.state("p1").value)
        assertEquals(listOf(true, true, false, true), fetch.requestsAsked)
        // Ten minutes after the last look that answered, Spole asks Seerr again without being forced.
        fetch.requests = { listOf(done) }
        time += 10 * 60_000L
        repository.refresh("p1", link)
        assertEquals(listOf(true, true, false, true, true), fetch.requestsAsked)
        // Seerr is not there for the person any more: nothing to show, and the requests are not asked for.
        fetch.rows = { DiscoverAnswer(SeerrAccess.NONE, emptyList()) }
        repository.refresh("p1", link, force = true)
        assertEquals(DiscoverState(SeerrAccess.NONE, emptyList(), emptyList(), loading = false, failed = false), repository.state("p1").value)
        assertEquals(5, fetch.requestsAsked.size)
    }

    @Test fun identityPrefersTheSpoleLinkAndSkipsKids() {
        val repository = DiscoverRepository(FakeFetch())
        val accounts = listOf(MediaAccount("a1", "p1", ServerKind.JELLYFIN, "s1", "Heime", "http://jf", "u1", "Ola"),
            MediaAccount("a2", "p1", ServerKind.EMBY, "s2", "Hytta", "http://emby", "u2", "Ola"))
        val chosen = SpoleLink(ServerKind.EMBY, "eilev")
        val ola = Profile("p1", "Ola", AvatarColor.LIME)
        assertNull(repository.identity(ola.copy(kid = true, spole = chosen), accounts))
        assertNull(repository.identity(ola.copy(kid = true), accounts))
        assertEquals(chosen, repository.identity(ola.copy(spole = chosen), accounts))
        assertEquals(SpoleLink(ServerKind.JELLYFIN, "u1"), repository.identity(ola, accounts))
        assertNull(repository.identity(ola, emptyList()))
    }

    @Test fun requestInvalidatesTheTitle() = runTest {
        var time = 0L
        val thrones = DiscoverDetails(title(1399, DiscoverType.SERIES), null, null, null, canRequest = false, requestId = 42,
            requestStage = RequestStage.REQUESTED, trailer = null, seasons = emptyList())
        val fetch = FakeFetch().apply { details[DiscoverType.MOVIE to 603] = matrix; details[DiscoverType.SERIES to 1399] = thrones }
        val repository = DiscoverRepository(fetch, now = { time })
        assertEquals(matrix, repository.details(link, DiscoverType.MOVIE, 603))
        assertEquals(matrix, repository.details(link, DiscoverType.MOVIE, 603))
        repository.related(link, DiscoverType.MOVIE, 603)
        assertEquals(1, repository.related(link, DiscoverType.MOVIE, 603).single().items.size)
        assertEquals(1, fetch.detailsAsked)
        assertEquals(1, fetch.relatedAsked)
        // A request that did not go through changes nothing.
        fetch.result = DiscoverActionResult.FAILED
        assertEquals(DiscoverActionResult.FAILED, repository.request(link, DiscoverType.MOVIE, 603, emptyList()))
        repository.details(link, DiscoverType.MOVIE, 603)
        assertEquals(1, fetch.detailsAsked)
        // One that did makes the title's page and rows be asked again.
        fetch.result = DiscoverActionResult.DONE
        assertEquals(DiscoverActionResult.DONE, repository.request(link, DiscoverType.MOVIE, 603, emptyList()))
        assertEquals(matrix, repository.details(link, DiscoverType.MOVIE, 603))
        repository.related(link, DiscoverType.MOVIE, 603)
        assertEquals(2, fetch.detailsAsked)
        assertEquals(2, fetch.relatedAsked)
        // A withdrawn request forgets the title it was for.
        repository.details(link, DiscoverType.SERIES, 1399)
        repository.details(link, DiscoverType.SERIES, 1399)
        assertEquals(3, fetch.detailsAsked)
        assertEquals(DiscoverActionResult.DONE, repository.withdraw(link, 42))
        repository.details(link, DiscoverType.SERIES, 1399)
        assertEquals(4, fetch.detailsAsked)
        // The page is the person's own (their request), so another person's look asks Spole; and five minutes on, everyone's does.
        repository.details(SpoleLink(ServerKind.JELLYFIN, "u1"), DiscoverType.MOVIE, 603)
        assertEquals(5, fetch.detailsAsked)
        time = 5 * 60_000L
        repository.details(link, DiscoverType.MOVIE, 603)
        assertEquals(6, fetch.detailsAsked)
    }

    // Spole tripping on a title's page, its rows or a search gives nothing to show, and the next look asks again.
    @Test fun aTrippedLookShowsNothingAndAsksAgain() = runTest {
        val fetch = FakeFetch().apply { details[DiscoverType.MOVIE to 603] = matrix; trips = true }
        val repository = DiscoverRepository(fetch)
        assertNull(repository.details(link, DiscoverType.MOVIE, 603))
        assertEquals(emptyList<DiscoverRow>(), repository.related(link, DiscoverType.MOVIE, 603))
        assertNull(repository.search(link, "dune", 1))
        fetch.trips = false
        assertEquals(matrix, repository.details(link, DiscoverType.MOVIE, 603))
        assertEquals(1, repository.related(link, DiscoverType.MOVIE, 603).size)
        assertEquals(DiscoverPage(emptyList(), 2, false), repository.search(link, "dune", 2))
        assertEquals(2, fetch.detailsAsked)
        assertEquals(2, fetch.relatedAsked)
    }
}
