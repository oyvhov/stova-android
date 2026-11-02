package app.stova.discover

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.media.model.ServerKind
import app.stova.profiles.SpoleLink
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/** Stova asks Spole's launcher provider for Discover; Spole asks Seerr. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SpoleDiscoverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val link = SpoleLink(ServerKind.EMBY, "eilev")

    /**
     * Spole's launcher provider: [answer] answers each query and [result] each call, it trips while [tripped], it keeps what
     * it was asked, and while [held] is set every question waits for it, as a Spole stuck on a Seerr that never answers.
     */
    private class FakeSpole(var answer: (Uri) -> Cursor? = { oldSpole() }, var result: String? = null, var tripped: Boolean = false) : ContentProvider() {
        val asked = CopyOnWriteArrayList<Uri>()
        val calls = CopyOnWriteArrayList<Pair<String, Bundle?>>()
        @Volatile var held: CountDownLatch? = null
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? {
            asked += uri
            held?.await(10, TimeUnit.SECONDS)
            check(!tripped) { "Spole tripped" }
            return answer(uri)
        }
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
            calls += method to extras
            held?.await(10, TimeUnit.SECONDS)
            check(!tripped) { "Spole tripped" }
            return result?.let { Bundle().apply { putString("result", it) } }
        }
    }

    private fun register(spole: FakeSpole, pkg: String = "app.reelstack") = spole.also { ShadowContentResolver.registerProviderInternal("$pkg.launcher", it) }
    /** [withProvider] declares the launcher provider as Spole's manifest does; without it the package is a Spole too old to have one. */
    private fun install(pkg: String = "app.reelstack", withProvider: Boolean = false) = shadowOf(context.packageManager).installPackage(PackageInfo().apply {
        packageName = pkg
        if (withProvider) providers = arrayOf(ProviderInfo().apply { authority = "$pkg.launcher"; packageName = pkg; name = "LauncherProfilesProvider" })
    })
    private fun Cursor.seerr(access: String) = apply { setExtras(Bundle().apply { putString("seerr", access) }) }
    private fun rows() = MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop", "status", "community", "genres"))
        .apply { addRow(arrayOf<Any?>("trending", "movie", 603, "The Matrix", 1999, null, "http://p/603", null, 5, 7.8, "Action")) }

    @Test fun rowsNeedTheSeerrExtra() {
        val spole = register(FakeSpole())
        // Without Spole nothing is asked.
        assertNull(SpoleDiscover(context).rows(link))
        assertTrue(spole.asked.isEmpty())
        // An older Spole answers every path with its profiles' empty cursor and no extras: that is no answer.
        install()
        val discover = SpoleDiscover(context)
        assertNull(discover.rows(link))
        assertNull(discover.requests(link, refresh = true))
        assertNull(discover.details(link, DiscoverType.MOVIE, 603))
        assertEquals(emptyList<DiscoverRow>(), discover.related(link, DiscoverType.MOVIE, 603))
        assertNull(discover.search(link, "dune", 2))
        assertEquals(listOf("discover?kind=emby&user=eilev", "discover/requests?kind=emby&user=eilev&refresh=1",
            "discover/title?kind=emby&user=eilev&type=movie&tmdb=603", "discover/related?kind=emby&user=eilev&type=movie&tmdb=603",
            "discover/search?kind=emby&user=eilev&q=dune&page=2"),
            spole.asked.map { it.toString().removePrefix("content://app.reelstack.launcher/") })
        // A Spole that knows Discover says what the person may do.
        spole.answer = { rows().seerr("personal") }
        assertEquals(DiscoverAnswer(SeerrAccess.PERSONAL, listOf(DiscoverRow(DiscoverRowKind.TRENDING, listOf(DiscoverTitle(DiscoverType.MOVIE,
            603, "The Matrix", 1999, null, "http://p/603", null, SeerrStatus.AVAILABLE, 7.8, listOf("Action")))))), discover.rows(link))
        spole.answer = { MatrixCursor(arrayOf("row")).seerr("none") }
        assertEquals(DiscoverAnswer(SeerrAccess.NONE, emptyList()), discover.rows(link))
        // Requests are asked again from Seerr only when Stova says so.
        discover.requests(link, refresh = false)
        assertEquals("discover/requests?kind=emby&user=eilev", spole.asked.last().toString().removePrefix("content://app.reelstack.launcher/"))
    }

    @Test fun requestCallSendsKindUserTypeTmdbSeasons() {
        val spole = register(FakeSpole(result = "ok"))
        // Without Spole there is no one to ask.
        assertEquals(DiscoverActionResult.NOT_SUPPORTED, SpoleDiscover(context).request(link, DiscoverType.SERIES, 1399, listOf(1, 2)))
        assertTrue(spole.calls.isEmpty())
        install()
        assertEquals(DiscoverActionResult.DONE, SpoleDiscover(context).request(link, DiscoverType.SERIES, 1399, listOf(1, 2)))
        val (method, extras) = spole.calls.single()
        assertEquals("discover_request", method)
        assertEquals("emby", extras!!.getString("kind"))
        assertEquals("eilev", extras.getString("user"))
        assertEquals("tv", extras.getString("type"))
        // Numbers go as Int: Spole reads a Long as 0.
        assertEquals(1399, extras.getInt("tmdb"))
        assertArrayEquals(intArrayOf(1, 2), extras.getIntArray("seasons"))
        // A withdrawal names the request the same way.
        spole.result = "unknown"
        assertEquals(DiscoverActionResult.UNKNOWN, SpoleDiscover(context).withdraw(link, 42))
        val (withdraw, named) = spole.calls.last()
        assertEquals("discover_withdraw", withdraw)
        assertEquals("emby", named!!.getString("kind"))
        assertEquals("eilev", named.getString("user"))
        assertEquals(42, named.getInt("request_id"))
        // An older Spole has no answer to either call.
        spole.result = null
        assertEquals(DiscoverActionResult.NOT_SUPPORTED, SpoleDiscover(context).request(link, DiscoverType.MOVIE, 603, emptyList()))
    }

    @Test fun detailsOfASeriesAlsoAskForItsSeasons() {
        val spole = register(FakeSpole(answer = { uri ->
            when (uri.path) {
                "/discover/title" -> MatrixCursor(arrayOf("type", "tmdb", "title", "can_request"))
                    .apply { addRow(arrayOf<Any?>(uri.getQueryParameter("type"), uri.getQueryParameter("tmdb"), "Game of Thrones", 1)) }.seerr("personal")
                "/discover/seasons" -> MatrixCursor(arrayOf("number", "name", "episodes", "status", "air_date"))
                    .apply { addRow(arrayOf<Any?>(1, "Season 1", 10, 5, "2011-04-17")); addRow(arrayOf<Any?>(2, "Season 2", 10, 1, null)) }.seerr("personal")
                else -> oldSpole()
            }
        }))
        install()
        val details = SpoleDiscover(context).details(link, DiscoverType.SERIES, 1399)!!
        assertEquals("Game of Thrones", details.title.title)
        assertEquals(listOf(DiscoverSeason(1, "Season 1", 10, SeerrStatus.AVAILABLE, "2011-04-17"), DiscoverSeason(2, "Season 2", 10, SeerrStatus.UNKNOWN, null)),
            details.seasons)
        assertEquals(listOf("discover/title?kind=emby&user=eilev&type=tv&tmdb=1399", "discover/seasons?kind=emby&user=eilev&type=tv&tmdb=1399"),
            spole.asked.map { it.toString().removePrefix("content://app.reelstack.launcher/") })
        // A film has no seasons to ask for.
        spole.asked.clear()
        assertEquals(emptyList<DiscoverSeason>(), SpoleDiscover(context).details(link, DiscoverType.MOVIE, 603)!!.seasons)
        assertEquals(listOf("/discover/title"), spole.asked.map { it.path })
    }

    // A Spole that trips is not one without Discover: the repository keeps the rows shown and says it failed.
    @Test fun aSpoleThatTripsThrowsInsteadOfAnsweringNothing() {
        val spole = register(FakeSpole(tripped = true))
        install()
        val discover = SpoleDiscover(context)
        assertThrows(IllegalStateException::class.java) { discover.rows(link) }
        assertThrows(IllegalStateException::class.java) { discover.details(link, DiscoverType.MOVIE, 603) }
        assertThrows(IllegalStateException::class.java) { discover.related(link, DiscoverType.MOVIE, 603) }
        assertThrows(IllegalStateException::class.java) { discover.search(link, "dune", 1) }
        assertEquals(DiscoverActionResult.FAILED, discover.request(link, DiscoverType.MOVIE, 603, emptyList()))
        // An older Spole still answers nothing, and so does one without the launcher provider, which gets no cursor.
        spole.tripped = false
        assertNull(discover.rows(link))
        spole.answer = { null }
        assertNull(discover.rows(link))
        // With the provider there, no cursor is Android's word for a Spole that died or threw what cannot cross.
        install(withProvider = true)
        assertThrows(IllegalStateException::class.java) { discover.rows(link) }
        // Another installed Spole that answers is taken instead.
        register(FakeSpole(answer = { rows().seerr("browse") }, result = "ok"), "app.reelstack.debug")
        install("app.reelstack.debug")
        assertEquals(SeerrAccess.BROWSE, discover.rows(link)?.access)
        spole.tripped = true
        assertEquals(DiscoverActionResult.DONE, discover.request(link, DiscoverType.MOVIE, 603, emptyList()))
    }

    // A Spole stuck on a Seerr that never answers is a failure once the wait is up, not a page left fetching for ever: the
    // rows shown stay, the next ask is not queued behind it, a search finds nothing and a request says it failed.
    @Test fun aSpoleThatNeverAnswersFailsAfterTheTimeout() = runBlocking<Unit> {
        val spole = register(FakeSpole(answer = { uri ->
            if (uri.path == "/discover") rows().seerr("personal") else MatrixCursor(arrayOf("row")).seerr("personal")
        }, result = "ok"))
        install()
        // Asked once before, so the first good answer below is not slowed by the first look at the provider.
        SpoleDiscover(context).rows(link)
        val repository = DiscoverRepository(SpoleDiscoverFetch(SpoleDiscover(context), timeoutMillis = 1_000))
        repository.refresh("p1", link)
        val shown = repository.state("p1").value!!.rows
        assertEquals(1, shown.size)
        val stuck = CountDownLatch(1)
        spole.held = stuck
        try {
            val started = System.nanoTime()
            repository.refresh("p1", link, force = true)
            assertEquals(DiscoverState(SeerrAccess.PERSONAL, shown, emptyList(), loading = false, failed = true), repository.state("p1").value)
            assertNull(repository.search(link, "dune", 1))
            assertEquals(DiscoverActionResult.FAILED, repository.request(link, DiscoverType.MOVIE, 603, emptyList()))
            // Three questions of a second each, not the ten seconds Spole would keep them waiting.
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) < 8)
        } finally {
            stuck.countDown()
        }
    }

    private companion object {
        /** What a Spole without Discover answers: its profiles' cursor, empty, with no extras. */
        fun oldSpole(): Cursor = MatrixCursor(arrayOf("id", "name", "kind", "user", "avatar", "kid"))
    }
}
