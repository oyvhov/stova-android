package app.stova.ha.cards

import androidx.compose.ui.input.key.Key
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
class TvControlPanelTest {
    @get:Rule val compose=createComposeRule()
    private val calls=mutableListOf<ServiceCall>()
    private fun panel(dialog:CardDialog,state:String,attrs:String,name:String="Eining") {
        val entity=HaEntity(dialog.entityId,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
        compose.setContent {StovaTheme(reduceMotion=true) {CardDialogHost(dialog,mapOf(entity.id to entity),FakeHaConnection(),null,false,
            {c,_->calls+=c},{},room="Småstova",name=name)}}
    }
    @Test fun offLightNeverClaimsFullBrightnessAndRightTurnsItOnDimmed() {
        panel(CardDialog.Brightness("light.a"),"off","""{"supported_color_modes":["brightness"]}""")
        compose.onNodeWithText("100 %").assertDoesNotExist()
        compose.onNodeWithText("Av").assertExists()
        compose.onNodeWithTag("slider-brightness").assertIsFocused().performKeyInput {pressKey(Key.DirectionRight)}
        compose.mainClock.advanceTimeBy(400);compose.waitForIdle()
        assertEquals(listOf(brightnessCall(1)),calls)
    }
    // On the Shield, Down from the brightness went past «Slå på» to «Farge og kvittone», and Up back
    // to the brightness: the one button that turns the light on could not be reached.
    @Test fun downFromTheBrightnessReachesTurnOnAndThenTheColours() {
        panel(CardDialog.Brightness("light.a"),"off","""{"supported_color_modes":["brightness","color_temp"],"supported_features":32}""")
        compose.onNodeWithTag("slider-brightness").assertIsFocused().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Slå på").assertIsFocused().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Farge og kvittone").assertIsFocused().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Slå på").assertIsFocused().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithTag("slider-brightness").assertIsFocused()
    }
    @Test fun downFromTheVolumeLandsOnPauseAndGoesOnToTheSources() {
        panel(CardDialog.Controls("media_player.sonos",CardType.MEDIA_PLAYER),"playing",
            """{"supported_features":53,"volume_level":0.25,"source":"Spotify","source_list":["TV","Spotify"]}""")
        compose.onNodeWithTag("slider-volume").assertIsFocused().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Pause").assertIsFocused().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Kjelde", substring = true).assertIsFocused().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Pause").assertIsFocused()
    }
    @Test fun climateLabelsTheRoomAndKeepsDriftSeparateFromPresets() {
        panel(CardDialog.Climate("climate.a"),"heat","""{"current_temperature":25,"temperature":22,"hvac_modes":["off","heat"],"preset_modes":["none","quiet","powerful"],"preset_mode":"quiet"}""")
        compose.onNodeWithText("Romtemperatur").assertExists()
        compose.onNodeWithText("Drift").assertExists()
        compose.onNodeWithText("Førehandsval").performClick()
        listOf("Normal","Stille","Kraftig").forEach {compose.onNodeWithText(it).assertExists()}
        compose.onNodeWithText("quiet").assertDoesNotExist()
        compose.onNodeWithText("Kraftig").performClick()
        assertEquals("powerful",calls.single().data.text("preset_mode"))
    }
    @Test fun tvSourceStartsAtVolumeWithHeaderAndCloseVisible() {
        panel(CardDialog.Controls("media_player.a",CardType.MEDIA_PLAYER),"playing",
            """{"volume_level":0.28,"source":"TV","source_list":["TV","Radio"],"supported_features":18485}""","Sonos Lydplanke")
        compose.onNodeWithTag("slider-volume").assertIsFocused()
        compose.onNodeWithText("Sonos Lydplanke").assertIsDisplayed()
        compose.onNodeWithText("Lukk").assertIsDisplayed()
        compose.onNodeWithText("Førre spor").assertDoesNotExist()
        compose.onNodeWithText("Radio").assertDoesNotExist()
    }
    @Test fun sourcePageBackReturnsToItsOpenerWithoutSendingAnything() {
        panel(CardDialog.Controls("media_player.a",CardType.MEDIA_PLAYER),"playing",
            """{"volume_level":0.28,"source":"TV","source_list":["TV","Radio"],"supported_features":2052}""")
        compose.onNodeWithText("Kjelde").performClick()
        compose.onNode(hasText("TV") and hasClickAction()).assertIsFocused().performKeyInput {pressKey(Key.Back)}
        compose.onNode(hasText("Kjelde") and hasClickAction()).assertIsFocused()
        compose.onNodeWithText("Radio").assertDoesNotExist()
        assertTrue(calls.isEmpty())
    }

    @Test fun bothClimateChoicesFitAboveTheFixedFooterAtNormalTextSize() {
        panel(CardDialog.Climate("climate.a"),"heat","""{"current_temperature":23.5,"temperature":21,"hvac_modes":["off","heat"],"preset_modes":["none","quiet"]}""")
        val presets=compose.onNode(hasText("Førehandsval") and hasClickAction()).getUnclippedBoundsInRoot()
        val footer=compose.onNodeWithText("Lukk").getUnclippedBoundsInRoot()
        assertTrue("preset choice is hidden below the footer",presets.bottom<=footer.top)
    }

    @Test fun energySummaryCanBeReachedByRemoteWhileCloseStaysVisible() {
        panel(CardDialog.Graph("sensor.price",true),"0.84","""{"unit_of_measurement":"kr/kWh","today":[0.7,0.8,1.2,0.9]}""")
        compose.onNodeWithTag("price-summary").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithTag("price-summary").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithText("Lukk").assertIsDisplayed()
        assertTrue(calls.isEmpty())
    }

    @Test @Config(fontScale=2f) fun largeTextCanReadTheRoomThenReachTheTargetWithDown() {
        panel(CardDialog.Climate("climate.a"),"heat","""{"current_temperature":23.5,"temperature":21,"hvac_modes":["off","heat"],"preset_modes":["none","quiet"]}""")
        compose.onNode(hasText("Romtemperatur") and isFocusable()).assertIsFocused().assertIsDisplayed()
            .performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithTag("slider-temperature").assertIsFocused()
        compose.onNodeWithText("Eining").assertIsDisplayed()
        compose.onNodeWithText("Lukk").assertIsDisplayed()
        assertTrue(calls.isEmpty())
    }
}
