package app.stova.ha.ui

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
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
class SmartHomeV2UiTest {
    @get:Rule val compose=createComposeRule()
    private val adult=Profile("a","A",AvatarColor.LIME)
    @Test fun firstCardReceivesInitialRemoteFocus() {
        compose.setContent {StovaTheme {SmartHomeContent(adult,listOf(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys")),emptyMap(),HaStatus.CONNECTED)}}
        compose.onNodeWithText("Leselys").assertIsFocused()
    }
    @Test fun menuChangesSizeThenRestoresFocus() {
        var card by mutableStateOf(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys"))
        compose.setContent {StovaTheme {SmartHomeContent(adult,listOf(card),emptyMap(),HaStatus.CONNECTED,onConfigure={card=it})}}
        compose.onNodeWithText("Leselys").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Leselys").performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Storleik").performClick()
        compose.onNodeWithText("L · 2 × 2").performClick()
        compose.runOnIdle {assertEquals(CardSize.L,card.size)}
        compose.onNodeWithText("Leselys").assertIsFocused()
    }
    @Test fun roomSectionsAndFavoritesAreVisible() {
        val cards=listOf(CardConfig("a",CardType.SENSOR,"sensor.a",favorite=true),CardConfig("b",CardType.SENSOR,"sensor.b"))
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED,registry=HaRegistry(mapOf("sensor.b" to HaEntityInfo(room="Stove"))))}}
        compose.onNodeWithText("Favorittar").assertExists()
        compose.onNodeWithText("Stove").performScrollTo().assertIsDisplayed()
    }
    @Test fun binaryDoorShowsOpenWord() {
        val door=HaEntity("binary_sensor.door","on",buildJsonObject {put("device_class","door")},0,0)
        compose.setContent {StovaTheme {HaCard(CardConfig("d",CardType.SENSOR,door.id),door,false,{},{})}}
        compose.onNodeWithText("Open").assertExists();compose.onNodeWithText("På").assertDoesNotExist()
    }
}
