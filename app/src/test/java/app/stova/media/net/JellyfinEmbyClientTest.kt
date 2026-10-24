package app.stova.media.net
import app.stova.media.model.*
import org.junit.*
import org.junit.Assert.*
import okhttp3.*
import okhttp3.mockwebserver.*
import kotlinx.coroutines.test.runTest
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
    @Test fun embyTokenDoesNotFollowForeignRedirect() = runTest {
        MockWebServer().use { other ->
            other.start()
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", other.url("/redirected")))
            other.enqueue(MockResponse().setBody("[]"))
            val account = MediaAccount("a", "p", ServerKind.EMBY, "s", "Demo", server.url("/").toString().trimEnd('/'), "u", "Demo")
            JellyfinEmbyClient(account, "secret", mediaHttp(), ClientIdentity("d", "i", "v")).resume()
            assertNull(other.takeRequest().getHeader("X-Emby-Authorization"))
        }
    }
    @Test fun sendsTokenHeader() = runTest {
        server.enqueue(MockResponse().setBody("[]")); client(ServerKind.EMBY).latest(Endpoint.LATEST_MOVIES)
        assertTrue(server.takeRequest().getHeader("X-Emby-Authorization")!!.contains("Token=\"tok\""))
    }
}
