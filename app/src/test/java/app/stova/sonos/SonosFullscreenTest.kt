package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The quiet volume rail and the explicit full-screen mode, through the controlled page. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosFullscreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun room(id: String = "media_player.stova", name: String = "Stova", state: String = "playing", features: Int = 1048575,
        volume: Double = .3, extra: String = "") = SonosRoom(HaEntity(id, state, StovaJson.parseToJsonElement(
        """{"media_title":"Inspector Norse","media_artist":"Todd Terje","entity_picture":"/api/art/a","supported_features":$features,"volume_level":$volume$extra}""").jsonObject, 0, 0), name)

    private val actions = mutableListOf<SonosAction>()
    private var immersive by mutableStateOf(false)
    private var rooms by mutableStateOf(listOf<SonosRoom>())
    private var left = 0
    private var artworkStarts = 0

    private fun page(vararg shown: SonosRoom, busy: Boolean = false, backgroundArt: Boolean = true, roomsOpen: Boolean = false,
        reduceMotion: Boolean = true) {
        rooms = shown.toList()
        compose.setContent { StovaTheme(reduceMotion = reduceMotion) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            // As the app: the page's own Back comes after Sonos is left.
            BackHandler { left++ }
            SonosContent(SonosState(rooms, loading = false), rooms.firstOrNull()?.id, {}, { actions += it }, {}, {}, {},
                busy = busy, backgroundArt = backgroundArt, roomsOpen = roomsOpen,
                artwork = { path, modifier ->
                    DisposableEffect(path) { artworkStarts++; onDispose {} }
                    Box(modifier.testTag("art-$path"))
                },
                immersive = immersive, onImmersive = { immersive = it })
        } }
    }
    private fun rail(id: String = "media_player.stova") = compose.onNodeWithTag("slider-sonos-$id")
    private fun key(k: Key) = compose.onRoot().performKeyInput { pressKey(k) }
    private fun systemBack() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }

    // 1. Volume: no words on screen, all of them for TalkBack.
    @Test fun volumeShowsNoTextButKeepsItsAccessibleNameValueAndRange() {
        page(room())
        compose.onNodeWithText("Volum", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("30 %", useUnmergedTree = true).assertDoesNotExist()
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Volum · Stova")))
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "30 %"))
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(30f, 0f..100f, steps = 49)))
    }

    @Test fun volumeStepsTwoPercentAndSendsOnceAfterTheKeysSettle() {
        page(room())
        rail().requestFocus()
        repeat(3) { key(Key.DirectionRight) }
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "36 %"))
        compose.mainClock.advanceTimeBy(600)
        assertEquals(listOf<SonosAction>(SonosAction.Volume(36)), actions)
    }

    @Test fun volumeStaysFocusedAndSendsNothingWhileBusyAndIsDisabledForAnOfflineRoom() {
        page(room(), busy = true)
        rail().requestFocus()
        key(Key.DirectionRight)
        compose.mainClock.advanceTimeBy(2500)
        rail().assertIsFocused()
        assertTrue(actions.isEmpty())
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "30 %"))
    }

    @Test fun offlineRoomVolumeIsDisabled() {
        page(room(state = "unavailable"))
        rail().assertIsNotEnabled()
    }

    @Test fun aPendingDraftNeverFollowsToTheNextRoom() {
        var id by mutableStateOf("media_player.a")
        var volume by mutableIntStateOf(30)
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosVolumeRail(id, volume, enabled = true, busy = false, label = "Volum · $id", onAction = { actions += it })
        } }
        rail("media_player.a").requestFocus()
        key(Key.DirectionRight)
        // Whatever room A sent was for room A; after the switch nothing more may go out.
        val beforeSwitch = actions.toList()
        assertTrue("$beforeSwitch", beforeSwitch.all { it == SonosAction.Volume(32) })
        compose.runOnUiThread { id = "media_player.b"; volume = 50 }
        compose.mainClock.advanceTimeBy(2500)
        rail("media_player.b").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50 %"))
        assertEquals(beforeSwitch, actions)
    }

    // 2. Full screen.
    @Test fun playerShowsTheTrackAndShortRoomNameWithoutARepeatedHeading() {
        page(room(name = "Sonos Storestova"), roomsOpen = true)
        compose.onNodeWithText("Inspector Norse").assertIsDisplayed()
        compose.onNodeWithText("Storestova", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Din musikk").assertDoesNotExist()
    }

    @Test fun playlistArtIsOnThePlayerAndOpeningItDoesNotReplaceTheQueue() {
        val current = room()
        val playlist = SonosMedia("favorite:mix", "favorite_item_id", "Kveldsmiks", "/mix.jpg", playable = true, mediaClass = "playlist")
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(listOf(current), loading = false), current.id, {}, { actions += it }, {}, {}, {},
                shelf = listOf(playlist), shelfThumbnail = { path, modifier, _, loaded ->
                    Box(modifier.testTag(path))
                    SideEffect { loaded() }
                })
        } }
        compose.onNodeWithText("Favorittar og spelelister").assertIsDisplayed()
        compose.onNodeWithTag("/mix.jpg", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Kveldsmiks").performClick()
        assertTrue(actions.isEmpty())
        compose.onNodeWithText("Spel").performClick()
        assertEquals(listOf<SonosAction>(SonosAction.PlayMedia(playlist)), actions)
    }

    @Test fun longPressEditsGroupInTheRoomListAndOnlyApplySendsTheDraft() {
        val leader = room(id = "media_player.a", name = "A")
        val other = room(id = "media_player.b", name = "B")
        var submitted: SonosGroupDraft? = null
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(listOf(leader, other), loading = false), leader.id, {}, {}, {}, {}, {},
                onApplyGroup = { draft, done -> submitted = draft; done(true) })
        } }
        val title = compose.onNodeWithText("Inspector Norse")
        val titleTop = title.getUnclippedBoundsInRoot().top
        compose.onNodeWithTag("sonos-room-${leader.id}").performSemanticsAction(SemanticsActions.OnLongClick)
        title.assertIsDisplayed()
        compose.onNodeWithText("Hovudrom", substring = true).assertIsDisplayed()
        assertEquals(titleTop, title.getUnclippedBoundsInRoot().top)
        rail(leader.id).assertIsDisplayed()
        compose.onNodeWithTag("sonos-room-${leader.id}").assertIsSelected()
        compose.onNodeWithTag("sonos-room-${other.id}").performClick().assertIsSelected()
        compose.runOnIdle { assertNull(submitted) }
        compose.onNodeWithText(compose.activity.getString(app.stova.R.string.sonos_apply)).performClick()
        compose.runOnIdle { assertEquals(SonosGroupDraft(leader.id, setOf(leader.id), setOf(leader.id, other.id)), submitted) }
        compose.onNodeWithTag("sonos-room-${other.id}").assertIsNotSelected()
    }

    @Test fun backdropKeepsTheOldImageWhileTheNextDecodes() {
        var path by mutableStateOf<String?>("first")
        val callbacks = mutableMapOf<String, () -> Unit>()
        compose.setContent { StovaTheme {
            SonosArtworkFade(path, backdrop = true) { image, modifier, ready, _ ->
                SideEffect { callbacks[image] = ready }
                Box(modifier.testTag(image))
            }
        } }
        compose.runOnIdle { callbacks.getValue("first")() }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { path = "next" }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("first").assertExists()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { callbacks.getValue("next")() }
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("first").assertExists()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag("first").assertDoesNotExist()
        compose.onNodeWithTag("next").assertExists()
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { path = null }
        compose.onNodeWithTag("next").assertDoesNotExist()
    }

    @Test fun enteringFullScreenKeepsTheExistingBackgroundImageLoaded() {
        page(room())
        // The backdrop and the square cover are already loaded.
        compose.runOnIdle { assertEquals(2, artworkStarts) }
        compose.onNodeWithTag("sonos-fullscreen").performClick()
        compose.onNodeWithTag("sonos-immersive").assertExists()
        compose.runOnIdle { assertEquals("Changing the viewing mode must not reload its backdrop", 2, artworkStarts) }
    }

    @Test fun restoredFullScreenOnlyRequestsFocusFromItsOwnControls() {
        val output = java.io.ByteArrayOutputStream()
        val original = System.out
        try {
            System.setOut(java.io.PrintStream(output))
            immersive = true
            page(room())
            compose.onNodeWithTag("sonos-play").assertIsFocused()
            assertFalse(output.toString().contains("FocusRequester is not initialized"))
            assertTrue(actions.isEmpty())
        } finally { System.setOut(original) }
    }

    @Test fun fullScreenEntersOnPlayAndBackReturnsToItsButtonWithoutTouchingMusic() {
        page(room())
        compose.onNodeWithTag("sonos-fullscreen").performClick()
        assertTrue(immersive)
        compose.onNodeWithTag("sonos-fullscreen-exit").assertExists()
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        // Management stays behind: no library, group, rooms or options here.
        compose.onNodeWithText("Musikk og favorittar").assertDoesNotExist()
        compose.onNodeWithText("Fleire val").assertDoesNotExist()
        compose.onNodeWithText("Inspector Norse").assertIsDisplayed()
        compose.onNodeWithText("Stova", substring = true).assertExists()
        systemBack()
        assertFalse(immersive)
        compose.onNodeWithTag("sonos-fullscreen").assertIsFocused()
        assertEquals(0, left)
        systemBack()
        assertEquals(1, left)
        assertTrue(actions.isEmpty())
    }

    @Test fun fullScreenVolumeUsesTheRoomSpeakerAndBackKeepsTheDraft() {
        page(room().copy(model = "Era 100"))
        compose.onNodeWithTag("sonos-fullscreen").performClick()
        val button = compose.onNodeWithTag("sonos-volume-button")
        val exit = compose.onNodeWithTag("sonos-fullscreen-exit")
        compose.onNodeWithTag("sonos-volume-speaker", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(button.getUnclippedBoundsInRoot().left > exit.getUnclippedBoundsInRoot().right)
        rail().assertDoesNotExist()
        key(Key.DirectionUp)
        button.assertIsFocused()
        button.performKeyInput { pressKey(Key.DirectionCenter) }
        rail().assertIsFocused()
        rail().performKeyInput { pressKey(Key.DirectionUp); pressKey(Key.DirectionUp); pressKey(Key.DirectionDown) }
        rail().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "32 %"))
        rail().performKeyInput { pressKey(Key.Back) }
        rail().assertDoesNotExist()
        button.assertIsFocused()
        assertTrue(immersive)
        assertEquals(listOf<SonosAction>(SonosAction.Volume(32)), actions)
    }

    @Test fun exitButtonLeavesFullScreenAndTransportStillWorksInside() {
        page(room())
        compose.onNodeWithTag("sonos-fullscreen").performClick()
        compose.onNodeWithContentDescription("Neste spor").performClick()
        assertEquals(listOf<SonosAction>(SonosAction.Next), actions)
        compose.onNodeWithTag("sonos-fullscreen-exit").performClick()
        assertFalse(immersive)
        compose.onNodeWithTag("sonos-fullscreen").assertIsFocused()
    }

    @Test fun volumePanelLetsGoOfFocusWhileItGlidesOut() {
        immersive = true
        page(room(), reduceMotion = false)
        val button = compose.onNodeWithTag("sonos-volume-button")
        val panel = compose.onNodeWithTag("sonos-volume-panel", useUnmergedTree = true)
        button.requestFocus()
        button.performKeyInput { pressKey(Key.DirectionCenter) }
        rail().assertIsFocused()
        compose.mainClock.autoAdvance = false
        rail().performKeyInput { pressKey(Key.Back) }
        compose.mainClock.advanceTimeBy(64)
        panel.assertExists()
        rail().assertIsNotFocused()
        compose.mainClock.advanceTimeBy(250)
        panel.assertDoesNotExist()
        button.assertIsFocused()
        assertTrue(immersive)
    }

    @Test fun withoutPlaybackSupportFullScreenFocusesItsExit() {
        page(room(features = 0))
        compose.onNodeWithTag("sonos-fullscreen").performClick()
        compose.onNodeWithTag("sonos-fullscreen-exit").assertIsFocused()
    }

    private fun tvGate(full: Boolean) {
        immersive = full
        page(room())
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(2000)
        assertTrue(compose.onAllNodesWithTag("art-/api/art/a", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.runOnIdle { rooms = listOf(room(extra = ""","source":"TV"""")) }
        // Frame by frame: the very first frame that says TV audio already has no album art.
        var frames = 0
        while (compose.onAllNodesWithText("TV-lyd", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() && frames++ < 60)
            compose.mainClock.advanceTimeByFrame()
        assertTrue("TV state never arrived", frames < 60)
        compose.onAllNodesWithTag("art-/api/art/a", useUnmergedTree = true).assertCountEquals(0)
    }
    @Test fun tvAudioNeverShowsTheOldArtNotEvenFadingOut() = tvGate(full = false)
    @Test fun tvAudioNeverShowsTheOldArtInFullScreen() = tvGate(full = true)

    @Test fun normalTransportIsIconOnlyWithSpokenNames() {
        page(room())
        compose.onNodeWithText("Førre spor").assertDoesNotExist()
        compose.onNodeWithContentDescription("Førre spor").performClick()
        compose.onNodeWithContentDescription("Pause").performClick()
        assertEquals(listOf(SonosAction.Previous, SonosAction.PlayPause), actions)
    }
}
