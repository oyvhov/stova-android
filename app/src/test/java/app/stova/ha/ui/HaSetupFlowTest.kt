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
