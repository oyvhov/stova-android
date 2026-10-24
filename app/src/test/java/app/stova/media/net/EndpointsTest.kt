package app.stova.media.net
import app.stova.media.model.*
import org.junit.*
import org.junit.Assert.*
import okhttp3.*
import okhttp3.mockwebserver.*
import kotlinx.coroutines.test.runTest
class EndpointsTest {
    @Test fun jellyfinTriesModernThenLegacy() = assertEquals(
        listOf("UserItems/Resume?userId=u&MediaTypes=Video&$ITEM_QUERY", "Users/u/Items/Resume?MediaTypes=Video&$ITEM_QUERY"),
        endpointCandidates(ServerKind.JELLYFIN, Endpoint.RESUME, "u"))
    @Test fun embyUsesUserInPathOnly() = assertEquals(1, endpointCandidates(ServerKind.EMBY, Endpoint.LATEST_MOVIES, "u").size)
    @Test fun headerPerKind() {
        val id = ClientIdentity("SHIELD Android TV", "dev1", "0.1.0")
        assertEquals("X-Emby-Authorization", authHeader(ServerKind.EMBY, id, "t").first)
        assertEquals("""MediaBrowser Client="Stova", Device="SHIELD Android TV", DeviceId="dev1", Version="0.1.0", Token="t"""", authHeader(ServerKind.JELLYFIN, id, "t").second)
    }
}
class JellyfinEmbyClientTest {
    @get:Rule val server = MockWebServer()
    private fun client(kind: ServerKind = ServerKind.JELLYFIN) = JellyfinEmbyClient(
        MediaAccount("a", "p", kind, "s", "S", server.url("/").toString().trimEnd('/'), "u", "n"), "tok", OkHttpClient(), ClientIdentity("d", "i", "v"))
    @Test fun fallsBackOn404() = runTest {
        server.enqueue(MockResponse().setResponseCode(404)); server.enqueue(MockResponse().setBody("""{"Items":[]}"""))
        assertEquals(emptyList<MediaItem>(), client().resume())
        assertTrue(server.takeRequest().path!!.startsWith("/UserItems/Resume")); assertTrue(server.takeRequest().path!!.startsWith("/Users/u/Items/Resume"))
    }
    @Test(expected = MediaError.Unauthorized::class) fun unauthorizedDoesNotFallBack() = runTest {
        server.enqueue(MockResponse().setResponseCode(401)); client().resume(); Unit }
    @Test fun sendsTokenHeader() = runTest {
        server.enqueue(MockResponse().setBody("[]")); client(ServerKind.EMBY).latest(Endpoint.LATEST_MOVIES)
        assertTrue(server.takeRequest().getHeader("X-Emby-Authorization")!!.contains("Token=\"tok\""))
    }
}
