package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.security.InMemorySecretStore
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.StovaButton
import app.stova.ha.HaStatus
import app.stova.ha.ui.HaSettingsContent
import app.stova.media.MediaAccountRepository
import app.stova.media.MediaAccountsFile
import app.stova.media.net.ClientIdentity
import app.stova.media.net.MediaAuth
import app.stova.media.net.ServerProbe
import app.stova.media.ui.AddAccountContent
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// The loft's Google TV Streamer, fresh: no Home Assistant and no account. OK on «Legg til konto»
// threw the user out to «Heim og utsjånad», because the pressed button left with the swap and
// Compose handed focus to the first tab. Every step of signing in must keep Smarthus.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AccountsFocusTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val server = MockWebServer()
    @get:Rule val tmp = TemporaryFolder()

    private fun render() {
        val accounts = MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"), MediaAccountsFile.serializer(), MediaAccountsFile()), InMemorySecretStore())
        val client = OkHttpClient()
        compose.setContent {
            var selected by remember { mutableStateOf(SettingsSection.SMART_HOME) }
            StovaTheme(reduceMotion = true) {
                SettingsLayout(selected, { selected = it }) { section, _ ->
                    if (section == SettingsSection.SMART_HOME) {
                        HaSettingsContent(HaStatus.NOT_CONFIGURED, null, emptyList(), {}, {}, { _, _ -> })
                        AccountsContent(emptyList(), onSignOut = {}) { done ->
                            AddAccountContent("p", accounts, ServerProbe(client), MediaAuth(client, ClientIdentity("d", "i", "v")), emptyFlow(), done)
                        }
                    } else StovaButton("${section.name} 0", {})
                }
            }
        }
    }

    private fun tab(label: String) = compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    private fun press(key: Key) = compose.onNode(isFocused()).performKeyInput { pressKey(key) }

    private fun openSignIn() {
        tab("Smarthus og kontoar").performSemanticsAction(SemanticsActions.RequestFocus)
        press(Key.DirectionRight)
        compose.onNodeWithText("Kople til").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithText("Legg til konto").assertIsFocused()
        press(Key.DirectionCenter)
    }

    @Test fun addingAnAccountStaysInSmarthusFromTheButtonToThePassword() {
        server.enqueue(MockResponse().setBody("""{"ProductName":"Emby","Id":"s","ServerName":"Demo"}"""))
        render()
        openSignIn()
        compose.onNodeWithText("Skriv adresse").assertIsFocused()
        tab("Smarthus og kontoar").assertIsSelected()

        press(Key.DirectionCenter)
        compose.onNodeWithText("Serveradresse").assertIsFocused().performTextInput(server.url("/").toString())
        press(Key.DirectionDown)
        compose.onNodeWithText("Neste").assertIsFocused()
        press(Key.DirectionCenter)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Brukarnamn").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Brukarnamn").assertIsFocused()
        tab("Smarthus og kontoar").assertIsSelected()
    }

    @Test fun backFromSigningInReturnsToSmarthus() {
        render()
        openSignIn()
        compose.onNodeWithText("Skriv adresse").assertIsFocused()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Legg til konto").assertIsDisplayed()
        compose.onNodeWithText("Kople til").assertIsFocused()
        tab("Smarthus og kontoar").assertIsSelected()
    }
}
