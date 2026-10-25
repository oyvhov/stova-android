package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class RemoteActionsTest {
    @get:Rule val compose=createComposeRule()
    private fun entity(id:String="light.a",state:String="on")=HaEntity(id,state,StovaJson.parseToJsonElement("""{"brightness":128,"volume_level":0.5}""").jsonObject,0,0)
    private fun focus(name:String) {compose.onNodeWithText(name).performSemanticsAction(SemanticsActions.RequestFocus)}
    @Test fun arrowsMoveBetweenLightsWithoutAnyService() {
        val calls=mutableListOf<ServiceCall>()
        compose.setContent {StovaTheme {Row {
            listOf("a","b").forEach {id -> HaCard(CardConfig(id,CardType.LIGHT,"light.$id"),entity("light.$id"),false,{calls+=it},{},modifier=Modifier.width(264.dp))}
        }}}
        focus("light.a")
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("light.b").assertIsFocused()
        compose.onNodeWithText("light.b").performKeyInput {pressKey(Key.DirectionLeft)}
        compose.onNodeWithText("light.a").assertIsFocused()
        compose.mainClock.advanceTimeBy(500)
        assertTrue(calls.isEmpty())
    }
    @Test fun heldOkOpensPanelAndMenuOpensCardOptions() {
        var dialog:CardDialog?=null;var menus=0;var calls=0
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),entity(),false,{calls++},{dialog=it},onMenu={menus++})}}
        focus("light.a")
        compose.onNodeWithText("light.a").performKeyInput {keyDown(Key.DirectionCenter)}
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("light.a").performKeyInput {keyUp(Key.DirectionCenter)}
        assertEquals(CardDialog.Brightness("light.a"),dialog);assertEquals(0,menus);assertEquals(0,calls)
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.Menu)}
        assertEquals(1,menus)
    }
    @Test fun shortOkTogglesOnAndOff() {
        val calls=mutableListOf<ServiceCall>()
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),entity(),false,{calls+=it},{})}}
        focus("light.a")
        repeat(2) {compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionCenter)}}
        assertEquals(listOf(ServiceCall("light","toggle"),ServiceCall("light","toggle")),calls)
    }
    @Test fun mediaRemoteKeysHaveSeparateActionsAndNeverUseArrows() {
        val calls=mutableListOf<ServiceCall>()
        compose.setContent {StovaTheme {HaCard(CardConfig("m",CardType.MEDIA_PLAYER,"media_player.a"),entity("media_player.a","playing"),false,{calls+=it},{})}}
        focus("media_player.a")
        compose.onNodeWithText("media_player.a").performKeyInput {
            pressKey(Key.MediaPlayPause);pressKey(Key.MediaFastForward);pressKey(Key.MediaRewind);pressKey(Key.DirectionRight)
        }
        assertEquals(listOf("media_play_pause","media_next_track","media_previous_track"),calls.map {it.service})
    }
    @Test fun readOnlyOkAndHeldOkInspectWithoutAnyServiceOrMenu() {
        var dialog:CardDialog?=null;var calls=0
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),entity(),true,{calls++},{dialog=it})}}
        focus("light.a")
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionCenter);pressKey(Key.Menu)}
        assertEquals(CardDialog.Brightness("light.a"),dialog);assertEquals(0,calls)
    }
}
