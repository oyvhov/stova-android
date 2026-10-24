package app.stova.media.ui
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.storage.JsonFileStore
import app.stova.core.security.InMemorySecretStore
import app.stova.media.*
import app.stova.media.net.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
@RunWith(AndroidJUnit4::class) @Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class AddAccountFlowTest {
 @get:Rule val compose=createComposeRule()
 @get:Rule val server=MockWebServer()
 @get:Rule val tmp=TemporaryFolder()
 private fun repo()=MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),MediaAccountsFile.serializer(),MediaAccountsFile()),InMemorySecretStore())
 private fun start(r: MediaAccountRepository) {
  compose.setContent { StovaTheme { AddAccountContent("p",r,ServerProbe(OkHttpClient()),MediaAuth(OkHttpClient(),ClientIdentity("d","i","v")),emptyFlow(),{}) } }
  compose.onNodeWithText("Skriv adresse").performClick()
  compose.onNodeWithText("Serveradresse").performTextInput(server.url("/").toString())
  compose.onNodeWithText("Neste").performClick()
 }
 private fun awaitText(text:String) { compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
 @Test fun manualAddressToPasswordSavesAccount() {
  server.enqueue(MockResponse().setBody("""{"ProductName":"Emby","Id":"s","ServerName":"Demo"}"""))
  server.enqueue(MockResponse().setBody("""{"AccessToken":"test-token","User":{"Id":"u","Name":"Demo"}}"""))
  val r=repo();start(r);awaitText("Brukarnamn")
  compose.onNodeWithText("Brukarnamn").performTextInput("demo"); compose.onNodeWithText("Passord").performTextInput("test")
  compose.onNodeWithText("Logg inn").performClick()
  compose.waitUntil(10_000) { runBlocking { r.forProfile("p").first().isNotEmpty() } }
  assertEquals(app.stova.media.model.ServerKind.EMBY,runBlocking { r.forProfile("p").first().single().kind })
 }
 @Test fun quickConnectShowsCode() {
  server.enqueue(MockResponse().setBody("""{"ProductName":"Jellyfin","Id":"s"}""")); server.enqueue(MockResponse().setBody("true"))
  server.enqueue(MockResponse().setBody("""{"Code":"123456","Secret":"s"}""")); server.enqueue(MockResponse().setBody("""{"Authenticated":false}"""))
  start(repo());awaitText("123456");compose.onNodeWithText("123456").assertIsDisplayed()
  compose.onNodeWithText("Bruk passord i staden").performClick();awaitText("Brukarnamn")
 }
 @Test fun wrongPasswordShowsMessage() {
  server.enqueue(MockResponse().setBody("""{"ProductName":"Emby","Id":"s"}"""));server.enqueue(MockResponse().setResponseCode(401))
  start(repo());awaitText("Logg inn");compose.onNodeWithText("Logg inn").performClick();awaitText("Feil brukarnamn eller passord")
  compose.onNodeWithText("Feil brukarnamn eller passord").assertIsDisplayed()
 }
}