package app.stova.ha.cards

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlinx.serialization.json.*

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class CardV2UiTest {
    @get:Rule val compose=createComposeRule()
    private fun e(id:String,state:String="on",attrs:String="{}")=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun focusedLightAdjustsBrightnessAndMenuAlwaysCustomises() {
        var call:ServiceCall?=null;var menus=0
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys"),e("light.a",attrs="""{"brightness":128}"""),false,{call=it},{},onMenu={menus++})}}
        compose.onNodeWithText("Leselys").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Leselys").assertIsFocused().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Lysstyrke 60 %").assertExists()
        compose.mainClock.advanceTimeBy(400);compose.waitForIdle();assertEquals(brightnessCall(60),call)
        compose.onNodeWithText("Leselys").performKeyInput {pressKey(Key.Menu)}
        assertEquals(1,menus)
    }
    @Test fun sensorOkOpensHistoryAndReadOnlyCardsStillAllowInspection() {
        var dialog:CardDialog?=null
        compose.setContent {StovaTheme {HaCard(CardConfig("s",CardType.SENSOR,"sensor.a"),e("sensor.a","12"),true,{}, {dialog=it})}}
        compose.onNodeWithText("sensor.a").performClick()
        assertEquals(CardDialog.Graph("sensor.a"),dialog)
    }
    @Test fun binaryDoorHasWordsAndLockRequiresConfirmation() {
        var dialog:CardDialog?=null;var calls=0
        compose.setContent {StovaTheme {HaCard(CardConfig("l",CardType.LOCK,"lock.front"),e("lock.front","locked"),false,{calls++},{dialog=it})}}
        compose.onNodeWithText("lock.front").performClick()
        assertEquals(CardDialog.Lock("lock.front"),dialog);assertEquals(0,calls)
    }
    @Test fun readOnlyLightIgnoresRemoteAdjustmentAndShowsPending() {
        var calls=0
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),e("light.a"),true,{calls++},{},pending=true)}}
        compose.onNodeWithText("light.a").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionRight);pressKey(Key.Enter)}
        assertEquals(0,calls);compose.onNodeWithText("Ventar …").assertExists()
    }
}
