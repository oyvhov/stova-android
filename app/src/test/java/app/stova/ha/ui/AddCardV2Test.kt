package app.stova.ha.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class AddCardV2Test {
    @get:Rule val compose=createComposeRule()
    private fun light(id:String)=HaEntity(id,"on",buildJsonObject {put("friendly_name",id.substringAfter('.'))},0,0)
    @Test fun waitsForConnectionAndReloadsAfterReconnect() {
        val socket=FakeHaConnection().apply {status.value=HaStatus.CONNECTING;allStates=listOf(light("light.Lampe"))}
        compose.setContent {StovaTheme {AddCardFlow(socket,{_,_->},{})}}
        compose.onNodeWithText("Koplar til …").assertExists();assertEquals(0,socket.stateRequests)
        // The picker needs live states, so it holds the connection while shown.
        assertEquals(1,socket.holders)
        compose.runOnIdle {socket.status.value=HaStatus.CONNECTED}
        compose.waitUntil(5_000) {socket.stateRequests==1}
        compose.onNodeWithText("Lampe").assertExists()
        compose.runOnIdle {socket.status.value=HaStatus.DISCONNECTED}
        compose.onNodeWithText("Fråkopla").assertExists()
        compose.runOnIdle {socket.allStates=listOf(light("light.Ny"));socket.status.value=HaStatus.CONNECTED}
        compose.waitUntil(5_000) {socket.stateRequests==2}
        compose.onNodeWithText("Ny").assertExists()
    }
    @Test fun roomsSearchHiddenAndAlreadyAddedAreVisible() {
        val socket=FakeHaConnection().apply {
            allStates=listOf(light("light.Lampe"),light("light.Diagnose"))
            registryValue=HaRegistry(mapOf("light.Lampe" to HaEntityInfo(room="Stove"),"light.Diagnose" to HaEntityInfo(room="Gang",hidden=true)))
        }
        compose.setContent {StovaTheme {AddCardFlow(socket,{_,_->},{},existing=setOf("light.Lampe"))}}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Stove").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Lagt til").assertExists();compose.onNodeWithText("Diagnose").assertDoesNotExist()
        compose.onNodeWithText("Vis alle").performClick();compose.onNodeWithText("Diagnose").assertExists()
        compose.onNodeWithText("Søk etter namn eller rom").performTextInput("Stove")
        compose.onNodeWithText("Lampe").assertExists();compose.onNodeWithText("Diagnose").assertDoesNotExist()
    }
}
