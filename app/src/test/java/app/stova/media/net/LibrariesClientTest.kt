package app.stova.media.net

import app.stova.media.BackgroundLibrary
import app.stova.media.model.*
import app.stova.profiles.BackgroundLibraryRef
import app.stova.profiles.SpotlightSource
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

/** A child's Home from a grown-up's account: every row asked for inside the chosen libraries only. */
class LibrariesClientTest {
    @get:Rule val server = MockWebServer()
    private val account = MediaAccount("a", "kid", ServerKind.EMBY, "s", "Server", "", "u", "Øyvind")
    private fun scoped(parent: String) = JellyfinEmbyClient(account.copy(baseUrl = server.url("/").toString().trimEnd('/')),
        "token", OkHttpClient(), ClientIdentity("d", "i", "v"), parentId = parent)
    private fun empty() = MockResponse().setBody("""{"Items":[]}""")

    @Test fun everyRowIsAskedForInsideTheLibrary() = runTest {
        repeat(4) { server.enqueue(empty()) }
        val client = scoped("barnefilmar")
        client.resume(); client.nextUp(); client.latest(Endpoint.LATEST_MOVIES); client.latest(Endpoint.FAVORITES)
        val paths = List(4) { server.takeRequest().requestUrl!! }
        assertEquals(listOf("/Users/u/Items/Resume", "/Shows/NextUp", "/Users/u/Items/Latest", "/Users/u/Items"), paths.map { it.encodedPath })
        assertTrue(paths.all { it.queryParameterValues("ParentId") == listOf("barnefilmar") })
    }

    @Test fun knownTitlesAndAnotherLibraryKeepTheirOwnQuery() = runTest {
        repeat(2) { server.enqueue(empty()) }
        val client = scoped("barnefilmar")
        client.items(listOf("x")); client.backgrounds(SpotlightSource.LIBRARY, "barneseriar")
        assertEquals(emptyList<String>(), server.takeRequest().requestUrl!!.queryParameterValues("ParentId"))
        assertEquals(listOf("barneseriar"), server.takeRequest().requestUrl!!.queryParameterValues("ParentId"))
    }

    private fun item(id: String) = MediaItem(id, "a", ItemType.MOVIE, id)
    private fun part(vararg ids: String) = object : MediaServerClient {
        override suspend fun resume() = ids.map(::item)
        override suspend fun nextUp() = emptyList<MediaItem>()
        override suspend fun latest(endpoint: Endpoint) = ids.map(::item)
        override suspend fun backgroundLibraries() = listOf("barnefilmar", "barneseriar", "skrekk").map {
            BackgroundLibrary(BackgroundLibraryRef("a", it), it, "Server")
        }
        override suspend fun backgrounds(source: SpotlightSource, libraryId: String?, query: String?, ids: List<String>?) =
            listOf(item("bg-$libraryId"))
    }

    @Test fun rowsFromSeveralLibrariesTakeTurnsWithoutRepeatsAndStayShort() = runTest {
        val client = LibrariesClient(mapOf("barnefilmar" to part("f1", "f2", "f3", "both"), "barneseriar" to part("s1", "both")))
        assertEquals(listOf("f1", "s1", "f2", "both", "f3"), client.latest(Endpoint.LATEST_MOVIES).map { it.id })
        assertEquals(listOf("f1", "s1", "f2", "both", "f3"), client.resume().map { it.id })
        val many = LibrariesClient(mapOf("a" to part(*Array(15) { "a$it" }), "b" to part(*Array(15) { "b$it" })))
        assertEquals(20, many.latest(Endpoint.LATEST_SERIES).size)
    }

    @Test fun onlyTheChosenLibrariesAreOffered() = runTest {
        val client = LibrariesClient(mapOf("barnefilmar" to part(), "barneseriar" to part()))
        assertEquals(listOf("barnefilmar", "barneseriar"), client.backgroundLibraries().map { it.ref.libraryId })
        assertEquals(listOf("bg-barneseriar"), client.backgrounds(SpotlightSource.LIBRARY, "barneseriar").map { it.id })
        assertTrue("a library the parents did not choose gives nothing", client.backgrounds(SpotlightSource.LIBRARY, "skrekk").isEmpty())
        assertNull(client.userImageTag())
    }
}
