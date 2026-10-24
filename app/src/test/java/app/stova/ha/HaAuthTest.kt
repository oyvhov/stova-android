package app.stova.ha

import app.stova.core.security.InMemorySecretStore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HaAuthTest {
    @get:Rule val server = MockWebServer()
    private var now = 1_000_000L
    private val secrets = InMemorySecretStore()
    private fun auth() = HaAuth(OkHttpClient(), secrets, { HaAddress(server.url("/").toString().trimEnd('/'), "") }, { now })
    @Test fun authorizeUrlEncodes() = assertEquals("http://ha:8123/auth/authorize?response_type=code&client_id=https%3A%2F%2Foyvhov.github.io%2Fstova%2F&redirect_uri=https%3A%2F%2Foyvhov.github.io%2Fstova%2Fauth-callback&state=s1", authorizeUrl(HaAddress("http://ha:8123", ""), "s1"))
    @Test fun callbackNeedsMatchingStateAndExactDestination() {
        assertEquals("c1", parseCallback("$HA_REDIRECT_URI?code=c1&state=s1", "s1"))
        listOf("$HA_REDIRECT_URI?code=c1&state=evil", "$HA_REDIRECT_URI/evil?code=c1&state=s1", "https://evil.example/auth-callback?code=c1&state=s1").forEach { assertNull(parseCallback(it, "s1")) }
    }
    @Test fun exchangeThenRefreshNearExpiry() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a1","expires_in":1800,"refresh_token":"r1"}"""))
        val a = auth(); a.exchange("c1"); assertEquals("a1", a.accessToken())
        now += 1_741_000
        server.enqueue(MockResponse().setBody("""{"access_token":"a2","expires_in":1800}"""))
        assertEquals("a2", a.accessToken())
        assertEquals("/auth/token", server.takeRequest().path)
        assertTrue(server.takeRequest().body.readUtf8().contains("grant_type=refresh_token"))
    }
    @Test fun revokedRefreshSignsOut() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a1","expires_in":1800,"refresh_token":"r1"}"""))
        val a = auth(); a.exchange("c1"); a.invalidateAccess()
        server.enqueue(MockResponse().setResponseCode(400))
        try { a.accessToken(); fail("expected signed out") } catch (_: HaSignedOut) { }
        assertEquals(HaAuthState.SIGNED_OUT, a.state.value); assertNull(secrets.read("ha/tokens"))
    }
    @Test fun longLivedNeverRefreshesAndRestores() = runTest { val a = auth(); a.useLongLivedToken("ll"); now = Long.MAX_VALUE - 1; assertEquals("ll", a.accessToken()); assertEquals("ll", auth().accessToken()) }
    @Test fun concurrentRefreshOnlyPostsOnce() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a","expires_in":1,"refresh_token":"r"}"""))
        val a = auth(); a.exchange("c")
        server.enqueue(MockResponse().setBody("""{"access_token":"b","expires_in":1800}"""))
        assertEquals(List(8) { "b" }, List(8) { async { a.accessToken() } }.awaitAll())
        assertEquals(2, server.requestCount)
    }
    @Test fun signOutRevokesAndDeletes() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a","expires_in":1800,"refresh_token":"r"}"""))
        val a = auth(); a.exchange("c"); server.takeRequest()
        server.enqueue(MockResponse()); a.signOut()
        val request = server.takeRequest(); assertEquals("/auth/token", request.path); assertTrue(request.body.readUtf8().contains("action=revoke"))
        assertNull(secrets.read("ha/tokens"))
    }
}
