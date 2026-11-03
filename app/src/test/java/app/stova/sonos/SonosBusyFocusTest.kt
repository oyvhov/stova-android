package app.stova.sonos

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.annotation.GraphicsMode

/**
 * Found on the TVs: while a Sonos action is pending (busy), the pressed control was disabled, so
 * focus jumped to a neighbour (the Rooms button, or the next Home widget) and the second OK did
 * something else. A pending action must keep focus where it is and ignore repeats.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosBusyFocusTest {
    @get:Rule val compose = createComposeRule()
    private fun room(features: Int = 1048575) = SonosRoom(HaEntity("media_player.stova", "playing", StovaJson.parseToJsonElement(
        """{"media_title":"Inspector Norse","media_artist":"Todd Terje","supported_features":$features,"volume_level":0.3}""").jsonObject, 0, 0), "Stova")

    private val actions = mutableListOf<SonosAction>()
    private var busy by mutableStateOf(false)

    /** As SonosScreen does: the first action makes the page busy until HA answers. */
    private fun page(roomsOpen: Boolean, features: Int = 1048575) {
        val r = room(features)
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosContent(SonosState(listOf(r), loading = false), r.id, {}, { actions += it; busy = true }, {}, {}, {},
                busy = busy, roomsOpen = roomsOpen)
        } }
    }
    private fun ok() = compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }

    @Test fun hiddenRoomsPlayButtonKeepsFocusWhileBusyAndIgnoresTheSecondOk() {
        page(roomsOpen = false)
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        ok()
        assertEquals(listOf<SonosAction>(SonosAction.PlayPause), actions)
        assertTrue(busy)
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        ok()
        assertEquals(1, actions.size)
        compose.onNodeWithTag("sonos-room-media_player.stova").assertDoesNotExist()
        compose.runOnIdle { busy = false }
        ok()
        assertEquals(2, actions.size)
    }

    @Test fun openRoomsTransportKeepsFocusWhileBusy() {
        page(roomsOpen = true)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithContentDescription("Førre spor").assertIsFocused()
        ok()
        assertEquals(listOf<SonosAction>(SonosAction.Previous), actions)
        compose.onNodeWithContentDescription("Førre spor").assertIsFocused()
        ok()
        assertEquals(1, actions.size)
    }

    @Test fun volumeKeepsFocusAndSendsNothingWhileBusy() {
        page(roomsOpen = false)
        compose.runOnIdle { busy = true }
        compose.onNodeWithTag("slider-sonos-media_player.stova").requestFocus()
        compose.onNodeWithTag("slider-sonos-media_player.stova").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(3000)
        compose.onNodeWithTag("slider-sonos-media_player.stova").assertIsFocused()
        assertTrue(actions.none { it is SonosAction.Volume })
    }

    @Test fun unsupportedPlaybackStaysDisabled() {
        page(roomsOpen = false, features = 0)
        compose.onNodeWithContentDescription("Pause").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Førre spor").assertIsNotEnabled()
    }

    @Test fun homeWidgetPlayKeepsFocusWhileBusyAndIgnoresRepeats() {
        val r = room()
        var presses = 0
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosNowPlaying(r, r, busy, false, {}, { presses++; busy = true })
        } }
        val play = compose.onNodeWithContentDescription("Pause")
        play.requestFocus()
        ok()
        assertEquals(1, presses)
        play.assertIsFocused()
        ok()
        assertEquals(1, presses)
        compose.runOnIdle { busy = false }
        ok()
        assertEquals(2, presses)
    }

    @Test fun homeWidgetWithoutPlaybackSupportStaysDisabled() {
        val r = room(features = 0)
        compose.setContent { StovaTheme(reduceMotion = true) { SonosNowPlaying(r, r, false, false, {}, {}) } }
        compose.onNodeWithContentDescription("Pause").assertIsNotEnabled()
    }
}
