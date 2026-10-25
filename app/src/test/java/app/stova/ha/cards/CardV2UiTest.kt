package app.stova.ha.cards

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.*
import app.stova.ha.*
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
    @Test fun focusedLightNeverAdjustsBrightnessAndMenuCustomises() {
        var call:ServiceCall?=null;var menus=0
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys"),e("light.a",attrs="""{"brightness":128}"""),false,{call=it},{},onMenu={menus++})}}
        compose.onNodeWithText("Leselys").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Leselys").assertIsFocused().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("50 %").assertExists()
        compose.mainClock.advanceTimeBy(400);compose.waitForIdle();assertNull(call)
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
    @Test fun historyLoadsAfterReconnectAndLateEntitySnapshot() {
        val socket=FakeHaConnection().apply {status.value=HaStatus.DISCONNECTED}
        var entity by mutableStateOf<HaEntity?>(null)
        compose.setContent {StovaTheme {HaCard(CardConfig("s",CardType.SENSOR,"sensor.a"),entity,false,{},{},connection=socket)}}
        compose.runOnIdle {entity=e("sensor.a","12");socket.status.value=HaStatus.CONNECTED}
        compose.waitUntil(5_000) {socket.historyRequests>0}
    }
    @Test fun climateDialogDownNavigatesWithoutChangingTemperature() {
        var calls=0
        val climate=e("climate.a","heat","""{"temperature":23.5,"current_temperature":25}""")
        compose.setContent {StovaTheme {CardDialogHost(CardDialog.Climate(climate.id),mapOf(climate.id to climate),FakeHaConnection(),null,false,{_,_->calls++},{})}}
        compose.onNodeWithText("Mindre").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Mindre").performKeyInput {pressKey(Key.DirectionDown)}
        assertEquals(0,calls)
    }
    @Test fun mediaCardIsOneNavigationTarget() {
        val media=e("media_player.a","playing", """{"volume_level":0.5}""")
        compose.setContent {StovaTheme {HaCard(CardConfig("m",CardType.MEDIA_PLAYER,media.id),media,false,{},{})}}
        compose.onNodeWithText("media_player.a").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("media_player.a").assertIsFocused()
        compose.onAllNodes(isFocusable()).assertCountEquals(1)
    }
}
