package app.stova.ha.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class RoomSetupTest {
    @get:Rule val compose=createComposeRule()
    @Test fun emptyDashboardStartsWithAddingInsteadOfLayout() {
        var added=0
        compose.setContent {StovaTheme(reduceMotion=true) {SmartHomeContent(Profile("p","P",AvatarColor.LIME),
            emptyList(),emptyMap(),HaStatus.CONNECTED,onAdd={added++})}}
        compose.onNodeWithText("Legg til kort").assertIsFocused().performKeyInput {pressKey(Key.Enter)}
        assertEquals(1,added)
        compose.onNodeWithText("Oppsett").assertDoesNotExist()
    }
    @Test fun dashboardNeverLoadsTheMovieBehindItsControls() {
        compose.setContent {StovaTheme {SmartHomeAtmosphere("file:///film.jpg","cloudy")}}
        compose.onNodeWithTag("smarthome-backdrop:file:///film.jpg",useUnmergedTree=true).assertDoesNotExist()
        compose.onNodeWithTag("smarthome-sky:cloudy",useUnmergedTree=true).assertExists()
    }
    @Test fun roomSelectionPreviewsOnlyItsUsefulNewCardsAndDoesNotSendServices() {
        val added=mutableListOf<String>();var done=0
        fun entity(id:String)=HaEntity(id,"on",JsonObject(emptyMap()),0,0)
        val socket=FakeHaConnection().apply {
            allStates=listOf("light.a","light.old","climate.pump","light.hidden","sensor.diagnostic","light.other").map(::entity)
            registryValue=HaRegistry(mapOf(
                "light.a" to HaEntityInfo("Lampa","Loftet"), "light.old" to HaEntityInfo("Gamal","Loftet"),
                "climate.pump" to HaEntityInfo("Varmepumpa","Loftet"), "light.hidden" to HaEntityInfo("Skjult","Loftet",hidden=true),
                "sensor.diagnostic" to HaEntityInfo("Diagnose","Loftet",hidden=true), "light.other" to HaEntityInfo("Utelampe","Ute")))
        }
        compose.setContent {StovaTheme(reduceMotion=true) {
            AddCardFlow(socket,{entity,_->added+=entity.id},{done++},existing=setOf("light.old"))
        }}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Vel rom").fetchSemanticsNodes().isNotEmpty()}
        compose.onNode(hasText("Loftet") and hasClickAction()).performClick()
        assertTrue(added.isEmpty())
        compose.onNodeWithText("Lampa").assertExists()
        compose.onNodeWithText("Varmepumpa").assertExists()
        listOf("Gamal","Skjult","Diagnose","Utelampe").forEach {compose.onNodeWithText(it).assertDoesNotExist()}
        compose.onNodeWithText("Varmepumpa").performClick()
        compose.onNodeWithText("Legg til 1 kort").performClick()
        compose.waitUntil(5_000) {done==1}
        assertEquals(listOf("light.a"),added);assertEquals(0,socket.calls)
    }
}
