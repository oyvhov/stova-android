package app.stova.ha.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.AppContainer
import app.stova.TV_QUALIFIERS
import app.stova.core.security.InMemorySecretStore
import app.stova.core.storage.StovaJson
import app.stova.core.theme.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmartHomeScreenTest {
    @get:Rule val compose=createComposeRule()
    private val adult=Profile("a","A",AvatarColor.LIME)
    private fun assertStatusCentered(fontScale: Float) {
        compose.setContent { StovaTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                SmartHomeContent(adult, emptyList(), emptyMap(), HaStatus.CONNECTED)
            }
        } }
        val heading = compose.onNodeWithText("Smarthus").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Tilkopla").assertDoesNotExist()
        val indicator=compose.onNodeWithContentDescription("Tilkopla").assertIsDisplayed()
        val bounds=indicator.getUnclippedBoundsInRoot()
        assertTrue("Status must stay visually small",bounds.right-bounds.left<=16.dp && bounds.bottom-bounds.top<=16.dp)
        val status = indicator.fetchSemanticsNode().boundsInRoot
        assertEquals("Status must share the heading's vertical centre", heading.center.y, status.center.y, 1f)
    }
    @Test fun statusIsCenteredWithHeading() = assertStatusCentered(1f)
    @Test fun statusIsCenteredWithHeadingAtDoubleFont() = assertStatusCentered(2f)
    @Test fun pageAndGroupsHaveConsistentVisualSymbols() {
        val cards = listOf(CardConfig("1", CardType.LIGHT, "light.x", favorite = true))
        compose.setContent { StovaTheme { SmartHomeContent(adult.copy(smartHome=DashboardPrefs(grouping=DashboardGrouping.ROOM)),cards,emptyMap(),HaStatus.CONNECTED) } }
        compose.onNodeWithTag("page-icon-smart-home",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithTag("section-icon-favorites",useUnmergedTree=true).assertIsDisplayed()
    }
    @Test fun remoteFocusOnTheFirstCardKeepsItsGroupHeadingVisible() {
        compose.setContent { StovaTheme(reduceMotion=true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SmartHomeContent(adult.copy(smartHome=DashboardPrefs(grouping=DashboardGrouping.ROOM)),List(9) { CardConfig("$it",CardType.SENSOR,"sensor.$it",name=if(it==0) "Stova" else "Kort $it",favorite=true) },
                emptyMap(),HaStatus.CONNECTED,initialFocusId="0")
        } }
        compose.onNodeWithText("Stova").assertIsFocused()
        compose.onNodeWithText("Favorittar").assertIsDisplayed()
        compose.onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        compose.onNodeWithText("Kort 1").assertIsFocused()
        compose.onNodeWithText("Favorittar").assertIsDisplayed()
    }
    @Test fun emptyStateOffersAddForAdult() {
        var adding=false
        compose.setContent { StovaTheme { SmartHomeContent(adult,emptyList(),emptyMap(),HaStatus.CONNECTED,onAdd={adding=true}) } }
        compose.onNodeWithText("Ingen kort enno").assertExists()
        compose.onNodeWithText("Legg til kort").performClick(); assertTrue(adding)
    }
    @Test fun kidSeesNoEditButton() {
        compose.setContent { StovaTheme { SmartHomeContent(adult.copy(kid=true),listOf(CardConfig("1",CardType.LIGHT,"light.x")),emptyMap(),HaStatus.CONNECTED) } }
        compose.onNodeWithText("Rediger").assertDoesNotExist();compose.onNodeWithText("Legg til kort").assertDoesNotExist()
    }
    @Test fun addCardFlowGroupsAndAdds() {
        val socket=FakeHaConnection().apply { allStates=listOf(HaEntity("light.stove","on",StovaJson.parseToJsonElement("""{"friendly_name":"Stove"}""").jsonObject,0,0),HaEntity("sensor.ute","4",JsonObject(emptyMap()),0,0)) }
        var added:Pair<HaEntity,CardType>?=null
        compose.setContent { StovaTheme { AddCardFlow(socket,{e,type->added=e to type},{}) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Lys").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Lys").performClick();compose.onNodeWithText("Stove").performClick()
        compose.waitUntil(5_000) { added!=null };assertEquals(CardType.LIGHT,added!!.second)
    }
    @Test fun fourthOnHomeShowsLimit() {
        val cards=List(4) {CardConfig("$it",CardType.SENSOR,"sensor.$it",onHome=it<3)}
        compose.setContent { StovaTheme { SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED,onHome={false}) } }
        compose.onNodeWithText("Rediger").performClick()
        compose.onNodeWithText("sensor.3").performScrollTo().performClick()
        compose.onNodeWithText("Vis på heim").performClick()
        compose.onNodeWithText("Ikkje plass til meir på heim. Gjer ei brikke til berre ikon, eller skjul ei.").assertExists()
    }
    // "Utført" and "lukkast ikkje" fade out with their own words instead of vanishing.
    @Test fun feedbackFadesOutInsteadOfVanishing() {
        var feedback by androidx.compose.runtime.mutableStateOf<HaFeedback?>(HaFeedback.DONE)
        compose.setContent { StovaTheme { SmartHomeContent(adult,listOf(CardConfig("1",CardType.LIGHT,"light.x")),emptyMap(),HaStatus.CONNECTED,feedback=feedback) } }
        compose.onNodeWithText("Utført").assertExists()
        compose.mainClock.autoAdvance=false
        compose.runOnIdle { feedback=null }
        compose.waitForIdle() // deliver the change; the frames below recompose it
        compose.mainClock.advanceTimeByFrame();compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Utført").assertExists() // still fading, same words
        compose.mainClock.autoAdvance=true
        compose.waitForIdle()
        compose.onNodeWithText("Utført").assertDoesNotExist()
    }
    @Test fun notConfiguredShowsInstructions() {
        compose.setContent { StovaTheme { SmartHomeContent(adult,emptyList(),emptyMap(),HaStatus.NOT_CONFIGURED) } }
        compose.onNodeWithText("Kople til Home Assistant i Innstillingar").assertExists()
    }
    // The socket rests until something needs it; the Smart Home page is such a need, even with no cards yet.
    @Test fun smartHomePageWakesAnIdleSocket() {
        val server=MockWebServer()
        server.enqueue(MockResponse().withWebSocketUpgrade(object:WebSocketListener() {
            override fun onOpen(ws:WebSocket,response:Response) {ws.send("""{"type":"auth_required"}""")}
            override fun onMessage(ws:WebSocket,text:String) {if(text.contains("\"auth\"")) ws.send("""{"type":"auth_ok"}""")}
        }))
        try {
            val container=AppContainer(ApplicationProvider.getApplicationContext(),secrets=InMemorySecretStore())
            var show by mutableStateOf(false)
            var ready by mutableStateOf(false)
            compose.setContent {
                var profile by remember { mutableStateOf<Profile?>(null) }
                LaunchedEffect(Unit) {
                    container.household.update {it.copy(haBaseUrl=server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/'))}
                    container.haAuth.useLongLivedToken("ll")
                    profile=container.profiles.create("A",AvatarColor.LIME,false).also {container.session.enter(it.id)}
                    container.haSocket.start()
                    ready=true
                }
                if(show) profile?.let { StovaTheme { SmartHomeScreen(container,it) } }
            }
            compose.waitUntil(5_000) { ready }
            assertNull("no cards, nothing shown: no connection",server.takeRequest(300,TimeUnit.MILLISECONDS))
            compose.runOnIdle { show=true }
            compose.waitForIdle()
            assertNotNull("the page is shown: connect",server.takeRequest(5,TimeUnit.SECONDS))
            compose.waitUntil(5_000) {container.haSocket.status.value==HaStatus.CONNECTED}
            compose.runOnIdle { show=false }
            container.haSocket.stop(0)
        } finally {server.shutdown()}
    }
}
