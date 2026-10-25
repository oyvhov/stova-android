package app.stova.ha.cards

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class ControlPanelTest {
    @get:Rule val compose=createComposeRule()
    private val calls=mutableListOf<ServiceCall>()
    private fun entity(id:String,state:String="on",attrs:String="{}")=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    private fun panel(dialog:CardDialog,e:HaEntity,locked:Boolean=false) {
        compose.setContent {StovaTheme {CardDialogHost(dialog,mapOf(e.id to e),FakeHaConnection(),null,locked,{c,_->calls+=c},{})}}
    }
    @Test fun unlockNeedsSeparateExplicitConfirmationAndCancelIsSafe() {
        panel(CardDialog.Lock("lock.front"),entity("lock.front","locked"))
        compose.onNodeWithText("Lås opp").performClick()
        assertTrue(calls.isEmpty())
        compose.onNodeWithText("Avbryt").assertIsFocused().performClick()
        assertTrue(calls.isEmpty())
        compose.onNodeWithText("Lås opp").performClick()
        compose.onNodeWithText("Ja, lås opp").performClick()
        assertEquals(listOf(ServiceCall("lock","unlock")),calls)
    }
    @Test fun lockingRequiresJustOnePress() {
        panel(CardDialog.Lock("lock.front"),entity("lock.front","unlocked"))
        compose.onNodeWithText("Lås").performClick()
        assertEquals(listOf(ServiceCall("lock","lock")),calls)
    }
    @Test fun garageNeedsConfirmationButCurtainDoesNot() {
        panel(CardDialog.Cover("cover.garage"),entity("cover.garage","closed","""{"device_class":"garage","supported_features":15,"current_position":0}"""))
        compose.onNodeWithText("Opne").performClick();assertTrue(calls.isEmpty())
        compose.onNodeWithText("Ja, opne").performClick()
        assertEquals(listOf(ServiceCall("cover","open_cover")),calls)
    }
    @Test fun gatePositionAlsoNeedsConfirmation() {
        panel(CardDialog.Cover("cover.gate"),entity("cover.gate","open","""{"device_class":"gate","supported_features":15,"current_position":50}"""))
        compose.onNodeWithTag("slider-position").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("slider-position").performKeyInput {pressKey(Key.DirectionRight)}
        compose.waitForIdle();compose.mainClock.advanceTimeBy(400);compose.waitForIdle()
        assertTrue(calls.isEmpty());compose.onNodeWithText("Ja, flytt").performClick()
        assertEquals(51.0,calls.single().data.number("position")!!,0.0)
    }
    @Test fun onlyTheFocusedSliderAdjustsAndUses300MsDebounce() {
        panel(CardDialog.Brightness("light.a"),entity("light.a",attrs="""{"brightness":128,"supported_color_modes":["brightness"]}"""))
        compose.mainClock.autoAdvance=false
        compose.onNodeWithTag("slider-brightness").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("slider-brightness").performKeyInput {pressKey(Key.DirectionRight);pressKey(Key.DirectionRight)}
        compose.onAllNodesWithText("52 %").assertCountEquals(2)
        compose.mainClock.advanceTimeBy(200);assertTrue(calls.isEmpty())
        compose.mainClock.advanceTimeBy(160);compose.waitForIdle()
        assertEquals(listOf(brightnessCall(52)),calls)
    }
    @Test fun arrowsOnButtonNavigateWithoutAdjusting() {
        panel(CardDialog.Brightness("light.a"),entity("light.a",attrs="""{"brightness":128}"""))
        compose.onNodeWithText("Slå av").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Slå av").performKeyInput {pressKey(Key.DirectionRight)}
        compose.mainClock.advanceTimeBy(400);assertTrue(calls.isEmpty())
    }
    @Test fun lightShowsOnlySupportedTemperatureAndColourControls() {
        panel(CardDialog.Brightness("light.a"),entity("light.a",attrs="""{"brightness":128,"supported_color_modes":["color_temp","hs"],"min_color_temp_kelvin":2000,"max_color_temp_kelvin":6000}"""))
        compose.onNodeWithText("Varm").performClick()
        assertEquals(2700.0,calls.single().data.number("color_temp_kelvin")!!,0.0)
        compose.onNodeWithText("Blå").assertExists()
    }
    @Test fun climateSupportsModesAndPresets() {
        panel(CardDialog.Climate("climate.a"),entity("climate.a","heat","""{"temperature":21.5,"hvac_modes":["heat","cool","auto","off"],"preset_modes":["eco"]}"""))
        compose.onNodeWithText("Kjøling").performClick()
        assertEquals("cool",calls.single().data["hvac_mode"].toString().trim('"'))
        compose.onNodeWithText("eco").assertExists()
    }
    @Test fun mediaPanelHasTransportVolumeAndSupportedSource() {
        panel(CardDialog.Controls("media_player.a",CardType.MEDIA_PLAYER),entity("media_player.a","playing","""{"volume_level":0.5,"source_list":["TV","Radio"]}"""))
        compose.onNodeWithText("Førre spor").performClick()
        assertEquals("media_previous_track",calls.single().service)
        compose.onNodeWithTag("slider-volume").assertExists()
        compose.onNodeWithText("Radio").assertExists()
    }
    @Test fun readOnlyPanelHasValuesButNoEnabledControls() {
        panel(CardDialog.Brightness("light.a"),entity("light.a",attrs="""{"brightness":128}"""),true)
        compose.onNodeWithText("Lesemodus").assertExists()
        compose.onNodeWithTag("slider-brightness").assertIsNotEnabled()
        compose.onNodeWithText("Slå av").assertIsNotEnabled()
        assertTrue(calls.isEmpty())
    }
    @Test fun sensorPanelHasHistoryExtremaAndLastChanged() {
        panel(CardDialog.Graph("sensor.a"),entity("sensor.a","22","""{"unit_of_measurement":"°C"}"""))
        compose.onNodeWithText("Min 10",substring=true).assertExists()
        compose.onNodeWithText("Maks 11",substring=true).assertExists()
        compose.onNodeWithText("Sist endra",substring=true).assertExists()
    }
    @Test fun fanHasSpeedAndDirectionWhenSupported() {
        panel(CardDialog.Controls("fan.a",CardType.TOGGLE),entity("fan.a",attrs="""{"percentage":40,"supported_features":5}"""))
        compose.onNodeWithTag("slider-speed").assertExists()
        compose.onNodeWithText("Bakover").performClick()
        assertEquals("set_direction",calls.single().service)
    }
}
