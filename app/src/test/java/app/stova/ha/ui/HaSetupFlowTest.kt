package app.stova.ha.ui

import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.AppContainer
import app.stova.TV_QUALIFIERS
import app.stova.core.security.InMemorySecretStore
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HaSetupFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun settingsWakesTheConfiguredConnectionWithoutWatchingAnyEntities() {
        val messages = java.util.concurrent.CopyOnWriteArrayList<String>()
        val server = okhttp3.mockwebserver.MockWebServer()
        server.enqueue(okhttp3.mockwebserver.MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                webSocket.send("""{"type":"auth_required"}""")
            }
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                messages += text
                if (text.contains("\"type\":\"auth\"")) webSocket.send("""{"type":"auth_ok"}""")
            }
        }))
        server.start()
        val container = AppContainer(ApplicationProvider.getApplicationContext(), secrets = InMemorySecretStore())
        try {
            runBlocking {
                container.haAuth.useLongLivedToken("test-only")
                container.household.update { it.copy(haBaseUrl = server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/')) }
            }
            container.haSocket.start()
            compose.setContent { StovaTheme { HaSettings(container) } }
            compose.waitUntil(5_000) { container.haSocket.status.value == HaStatus.CONNECTED }
            compose.onNodeWithContentDescription("Tilkopla").assertIsDisplayed()
            compose.onNodeWithText("Tilkopla").assertDoesNotExist()
            compose.onNodeWithText("Fråkopla").assertDoesNotExist()
            assertFalse(messages.any { it.contains("subscribe_entities") || it.contains("call_service") })
        } finally {
            container.haSocket.stop(0)
            container.appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            server.shutdown()
        }
    }
    @Test fun loginClientInterceptsOnlyMatchingCallback() {
        var code: String? = null
        val client = HaLoginClient("s1") { code = it }
        val view = WebView(ApplicationProvider.getApplicationContext())
        assertTrue(client.shouldOverrideUrlLoading(view, "$HA_REDIRECT_URI?code=c1&state=s1"))
        assertEquals("c1", code)
        assertFalse(client.shouldOverrideUrlLoading(view, "http://ha.local:8123/auth/login"))
    }
    @Test fun insecurePublicAddressShowsMessage() {
        val container = AppContainer(ApplicationProvider.getApplicationContext(), secrets = InMemorySecretStore())
        compose.setContent { StovaTheme { HaSetupFlow(container) {} } }
        compose.onNodeWithText("Adresse").performTextInput("http://ha.example.no")
        compose.onNodeWithText("Logg inn").performClick()
        compose.onNodeWithText("Bruk https for adresser utanfor heimenettet.").assertExists()
    }
    @Test fun longLivedTokenSavesAndSignsIn() {
        val container = AppContainer(ApplicationProvider.getApplicationContext(), secrets = InMemorySecretStore())
        var done = false
        compose.setContent { StovaTheme { HaSetupFlow(container) { done = true } } }
        compose.onNodeWithText("Adresse").performTextInput("ha.local")
        compose.onNodeWithText("Bruk langtidstoken").performClick()
        compose.onNodeWithText("Token").performTextInput("ll")
        compose.onNodeWithText("Lagre").performClick()
        compose.waitUntil(5_000) { done }
        assertEquals(HaAuthState.SIGNED_IN, container.haAuth.state.value)
        assertEquals("http://ha.local:8123", runBlocking { container.household.current().haBaseUrl })
    }
}
