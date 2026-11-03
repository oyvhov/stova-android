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

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosContentTest {
    @get:Rule val compose = createComposeRule()
    private val room = SonosRoom(HaEntity("media_player.stova", "playing", StovaJson.parseToJsonElement(
        """{"media_title":"Northern Lights","media_artist":"Test Artist","supported_features":1048575,"volume_level":0.3}""").jsonObject, 0, 0), "Stova")

    @Test fun nowPlayingChipHasSeparateOpenAndPlaybackActions() {
        var opened = false
        var played = false
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosNowPlaying(room, room, false, false, { opened = true }, { played = true })
        } }
        compose.onNodeWithText("Northern Lights").performClick()
        assertTrue(opened)
        assertFalse(played)
        compose.onNodeWithContentDescription("Pause").performClick()
        assertTrue(played)
    }

    @Test fun tvChipDoesNotShowStaleMusicOrLoadItsArtwork() {
        var imageLoaded = false
        val tv = room.copy(entity = room.entity.copy(attributes = kotlinx.serialization.json.JsonObject(room.entity.attributes +
            mapOf("source" to kotlinx.serialization.json.JsonPrimitive("TV"), "entity_picture" to kotlinx.serialization.json.JsonPrimitive("/old.jpg")))))
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosNowPlaying(tv, tv, false, false, {}, {}, artwork = { _, _ -> imageLoaded = true })
        } }
        compose.onNodeWithText("TV-lyd").assertDoesNotExist()
        compose.onNodeWithText("Northern Lights").assertDoesNotExist()
        assertFalse(imageLoaded)
    }

    @Test fun musicHeroNamesTheMusicInsteadOfTheMovieSpotlight() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            CompositionLocalProvider(LocalHomeSonos provides room) {
                app.stova.home.HeroPanel(null, androidx.compose.ui.Modifier)
            }
        } }
        compose.onNodeWithText("Northern Lights").assertIsDisplayed()
        compose.onNodeWithText("Test Artist").assertIsDisplayed()
    }

    @Test fun playButtonSendsAnActionAndRemoteCanReachRoomSelection() {
        var action: SonosAction? = null
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosContent(SonosState(listOf(room), loading = false), room.id, {}, { action = it }, {}, {}, {})
        } }
        compose.onNodeWithText("Northern Lights").assertIsDisplayed()
        compose.onNodeWithText("Musikk i heile huset").assertDoesNotExist()
        compose.onNodeWithText("Spelar no · Stova").assertIsDisplayed()
        compose.onNodeWithTag("sonos-room-${room.id}").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("sonos-room-${room.id}").assertIsNotFocused()
        compose.onNodeWithText("Pause").performClick()
        assertEquals(SonosAction.PlayPause, action)
    }

    @Test fun unavailableRoomDoesNotOfferEnabledPlayback() {
        compose.setContent { StovaTheme {
            SonosContent(SonosState(listOf(room.copy(entity = room.entity.copy(state = "unavailable"))), loading = false), room.id, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Spel").assertIsNotEnabled()
    }

    @Test fun groupDraftDoesNothingBeforeApplyAndCoordinatorCannotBeRemoved() {
        var saved: Set<String>? = null
        val other = room.copy(entity = room.entity.copy(id = "media_player.kitchen"), name = "Kjøkken")
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosGroupDialog(room, listOf(room, other), false, false, { saved = it }, {})
        } }
        compose.onNodeWithText("Kjøkken").performClick()
        assertNull(saved)
        compose.onNodeWithText("Bruk").performClick()
        assertEquals(setOf(room.id, other.id), saved)
    }
}
