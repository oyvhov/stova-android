package app.stova.discover

import app.stova.core.theme.AvatarColor
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.seerrAccount
import app.stova.media.viaSeerr
import app.stova.media.model.ItemType
import app.stova.media.model.MediaAccount
import app.stova.media.model.MediaItem
import app.stova.media.model.ServerKind
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home's Oppdag row: Seerr's popular titles through Spole, for a grown-up with a Spole person, and nothing asked for a child. */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverRowSourceTest {
    private val link = SpoleLink(ServerKind.EMBY, "eilev")
    private val eilev = Profile("p1", "Eilev", AvatarColor.LIME, spole = link)
    private fun title(tmdb: Int, type: DiscoverType = DiscoverType.MOVIE) = DiscoverTitle(type, tmdb, "T$tmdb", 1999, "Neo wakes.",
        "https://tmdb/p/$tmdb", "https://tmdb/b/$tmdb", SeerrStatus.AVAILABLE, 7.9, listOf("Action"))

    /** Spole as the repository sees it: [answer] answers Discover's rows, every ask is counted with its person, and nothing else is asked. */
    private class RowsFetch(private val answer: suspend () -> DiscoverAnswer?) : DiscoverFetch {
        val asked = mutableListOf<SpoleLink>()
        val rowsAsked get() = asked.size
        override suspend fun rows(link: SpoleLink): DiscoverAnswer? { asked += link; return answer() }
        override suspend fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>? = error("not asked")
        override suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? = error("not asked")
        override suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> = error("not asked")
        override suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? = error("not asked")
        override suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>) = error("not asked")
        override suspend fun withdraw(link: SpoleLink, requestId: Int) = error("not asked")
    }

    @Test fun kidProfileGetsNoRowAndNoFetch() = runTest(UnconfinedTestDispatcher()) {
        val fetch = RowsFetch { DiscoverAnswer(SeerrAccess.PERSONAL, listOf(DiscoverRow(DiscoverRowKind.TRENDING, listOf(title(603))))) }
        val repository = DiscoverRepository(fetch)
        val accountsAsked = mutableListOf<String>()
        val kid = eilev.copy(kid = true)
        val active = MutableStateFlow<Profile?>(kid)
        val source = DiscoverRowSource(repository, active, { id -> accountsAsked += id; MutableStateFlow(emptyList<MediaAccount>()) }, backgroundScope)
        // A child has no Discover: no row, and neither its accounts nor Spole are asked.
        assertEquals(listOf(emptyList<HomeRowUi>()), source.rows(kid).toList())
        assertEquals(0, fetch.rowsAsked)
        assertEquals(emptyList<String>(), accountsAsked)
        assertNull(repository.state("p1").value)
        // A grown-up who follows nobody in Spole and has no account here has nobody to ask for either.
        val nobody = Profile("p2", "Gjest", AvatarColor.LIME)
        active.value = nobody
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(nobody).toList(seen) }
        assertEquals(listOf(emptyList<HomeRowUi>()), seen)
        assertEquals(0, fetch.rowsAsked)
        job.cancel()
    }

    @Test fun emitsEmptyAtOnceThenTwelveTrendingCards() = runTest(UnconfinedTestDispatcher()) {
        val gate = CompletableDeferred<Unit>()
        val trending = listOf(title(603)) + (1..19).map { title(1000 + it, if (it % 2 == 0) DiscoverType.SERIES else DiscoverType.MOVIE) }
        val fetch = RowsFetch {
            gate.await()
            DiscoverAnswer(SeerrAccess.PERSONAL, listOf(DiscoverRow(DiscoverRowKind.TRENDING, trending),
                DiscoverRow(DiscoverRowKind.UPCOMING_MOVIES, listOf(title(11)))))
        }
        val source = DiscoverRowSource(DiscoverRepository(fetch), MutableStateFlow(eilev), { MutableStateFlow(emptyList<MediaAccount>()) },
            backgroundScope)
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(eilev).toList(seen) }
        // Home is not held up while Spole asks Seerr: nothing yet, and the ask is under way.
        assertEquals(listOf(emptyList<HomeRowUi>()), seen)
        assertEquals(1, fetch.rowsAsked)
        gate.complete(Unit)
        val row = seen.last().single() as HomeRowUi.Media
        assertEquals(HomeRowKey.DISCOVER, row.key)
        assertEquals(MediaRowKind.DISCOVER, row.kind)
        assertEquals(seerrAccount("p1", link), row.account)
        assertTrue(row.account.viaSeerr)
        assertEquals("seerr-emby-eilev", row.account.id)
        // The first twelve popular titles, as cards the page can open again.
        assertEquals(12, row.items.size)
        val matrix = row.items.first()
        assertEquals("movie-603", matrix.id)
        assertEquals(row.account.id, matrix.accountId)
        assertEquals(ItemType.MOVIE, matrix.type)
        assertEquals("https://tmdb/p/603", matrix.art.poster)
        assertEquals("https://tmdb/b/603", matrix.art.wide)
        assertEquals(7.9, matrix.ratings.community!!, .001)
        assertEquals(ItemType.SERIES, row.items[2].type)
        assertEquals(DiscoverType.SERIES to 1002, row.items[2].discoverRef())
        // Opened from the card, the title knows what the card showed; what Seerr says of it the page asks again.
        assertEquals(title(603).copy(status = SeerrStatus.UNKNOWN), matrix.asDiscoverTitle())
        // A title from the person's own library is no Discover title.
        assertNull(MediaItem("4f2a9c", "a", ItemType.MOVIE, "Heat").asDiscoverTitle())
        assertEquals(1, fetch.rowsAsked)
        job.cancel()
    }

    private val trending = DiscoverAnswer(SeerrAccess.PERSONAL, listOf(DiscoverRow(DiscoverRowKind.TRENDING, listOf(title(603)))))

    // Home starts a source once per profile; the person picked in Settings while it runs still counts.
    @Test fun aSpolePersonChosenWhileHomeRunsBringsTheRow() = runTest(UnconfinedTestDispatcher()) {
        val fetch = RowsFetch { trending }
        val plain = eilev.copy(spole = null)
        val active = MutableStateFlow<Profile?>(plain)
        val source = DiscoverRowSource(DiscoverRepository(fetch), active, { MutableStateFlow(emptyList<MediaAccount>()) }, backgroundScope)
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(plain).toList(seen) }
        assertEquals(listOf(emptyList<HomeRowUi>()), seen)
        assertEquals(0, fetch.rowsAsked)
        active.value = eilev
        assertEquals(listOf(link), fetch.asked)
        val row = seen.last().single() as HomeRowUi.Media
        assertEquals("seerr-emby-eilev", row.account.id)
        assertEquals(listOf("movie-603"), row.items.map { it.id })
        job.cancel()
    }

    @Test fun anotherSpolePersonSwapsTheAccountAndAsksAgain() = runTest(UnconfinedTestDispatcher()) {
        val fetch = RowsFetch { trending }
        val active = MutableStateFlow<Profile?>(eilev)
        val source = DiscoverRowSource(DiscoverRepository(fetch), active, { MutableStateFlow(emptyList<MediaAccount>()) }, backgroundScope)
        val seen = mutableListOf<List<HomeRowUi>>()
        val job = launch { source.rows(eilev).toList(seen) }
        assertEquals("seerr-emby-eilev", (seen.last().single() as HomeRowUi.Media).account.id)
        // The last answer was the other person's, so Spole is asked at once, as the new person.
        val oyvind = SpoleLink(ServerKind.JELLYFIN, "oyvind")
        active.value = eilev.copy(spole = oyvind)
        assertEquals(listOf(link, oyvind), fetch.asked)
        assertEquals("seerr-jellyfin-oyvind", (seen.last().single() as HomeRowUi.Media).account.id)
        // A change elsewhere in the profile is no new person, and asks nothing.
        active.value = eilev.copy(name = "Eilev H", spole = oyvind)
        assertEquals(2, fetch.rowsAsked)
        job.cancel()
    }
}
