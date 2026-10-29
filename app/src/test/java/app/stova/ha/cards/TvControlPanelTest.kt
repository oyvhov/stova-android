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
}
