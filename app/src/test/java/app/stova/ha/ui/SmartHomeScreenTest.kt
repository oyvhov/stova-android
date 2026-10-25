package app.stova.ha.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
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
        val status = compose.onNodeWithText("Tilkopla").fetchSemanticsNode().boundsInRoot
        assertEquals("Status must share the heading's vertical centre", heading.center.y, status.center.y, 1f)
    }
    @Test fun statusIsCenteredWithHeading() = assertStatusCentered(1f)
    @Test fun statusIsCenteredWithHeadingAtDoubleFont() = assertStatusCentered(2f)
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
        compose.onNodeWithText("Maks tre kort på heim").assertExists()
    }
    @Test fun notConfiguredShowsInstructions() {
        compose.setContent { StovaTheme { SmartHomeContent(adult,emptyList(),emptyMap(),HaStatus.NOT_CONFIGURED) } }
        compose.onNodeWithText("Kople til Home Assistant i Innstillingar").assertExists()
    }
}
