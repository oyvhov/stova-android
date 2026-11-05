package app.stova.sonos

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import app.stova.profiles.*
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

    @Test fun roomListUsesTheHomeAssistantAreaIconWhenProvided() {
        val withIcon=room.copy(roomIcon="mdi:stove")
        compose.setContent {StovaTheme(reduceMotion=true) {
            SonosContent(SonosState(listOf(withIcon),loading=false),withIcon.id,{},{},{},{},{})
        }}
        val icon=hasTestTag("ha-icon") and hasAnyAncestor(hasTestTag("sonos-room-${room.id}"))
        compose.waitUntil(5_000) {compose.onAllNodes(icon,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNode(icon,useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithTag("sonos-room-playing-${room.id}",useUnmergedTree=true).assertIsDisplayed()
    }

    @Test fun seasonDecorationFollowsTheSameProfilePreferenceAsHome() {
        var choice by mutableStateOf(SeasonChoice.HALLOWEEN)
        var decor by mutableStateOf(SeasonalDecor.CALM)
        compose.setContent { StovaTheme(reduceMotion = true, appearance = choice.applyTo(AppearancePrefs(seasonalDecor = decor))) {
            SonosContent(SonosState(listOf(room), loading = false), room.id, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("season-mark", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { choice = SeasonChoice.CHRISTMAS }
        compose.onNodeWithTag("season-mark", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { decor = SeasonalDecor.OFF }
        compose.onNodeWithTag("season-mark", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun dailyControlsFitWithoutScrollingEvenWithLongMetadataAndAGroup() {
        val detailed = room.copy(entity = room.entity.copy(attributes = StovaJson.parseToJsonElement("""{
            "media_title":"Halloween Forest, Wind, Spooky Sounds and Northern Lights",
            "media_artist":"A rather long artist name", "media_album_name":"A very long album name",
            "media_duration":240, "volume_level":0.3, "supported_features":1048575,
            "source_list":["TV","Radio"], "group_members":["media_player.stova","media_player.second"]
        }""").jsonObject))
        val second = detailed.copy(entity = detailed.entity.copy(id = "media_player.second"), name = "Kjøkken")
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(listOf(detailed, second), loading = false), detailed.id, {}, {}, {}, {}, {}, actionFailed = true, onBrowseMusicAssistant = {})
        } }
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        listOf(compose.onNodeWithTag("slider-sonos-${room.id}"), compose.onNodeWithText("Musikk og favorittar"),
            compose.onNodeWithText("Grupper rom"), compose.onNodeWithText("Music Assistant"), compose.onNodeWithText("Fleire val")).forEach { node ->
            node.assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("Entire control must fit inside the TV safe area: $bounds", bounds.bottom <= root.bottom - 24.dp)
        }
        compose.onNodeWithText("TV", substring = false).assertDoesNotExist()
    }

    // A: the rooms are a side menu that can be hidden so the music takes the page.
    @Test fun hidingTheRoomsFocusesPlaybackAndOpeningThemFocusesTheSelectedRoom() {
        val kitchen = room.copy(entity = room.entity.copy(id = "media_player.kitchen", state = "paused"), name = "Kjøkken")
        var open by mutableStateOf(true)
        var selected: String? = null
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosContent(SonosState(listOf(room, kitchen), loading = false), room.id, { selected = it }, {}, {}, {}, {},
                roomsOpen = open, onRoomsOpen = { open = it })
        } }
        compose.onNodeWithTag("sonos-room-${kitchen.id}").performClick()
        assertEquals(kitchen.id, selected)
        compose.onNodeWithText("Gøym rom").performClick()
        assertFalse(open)
        compose.onNodeWithTag("sonos-room-${room.id}").assertDoesNotExist()
        // What plays takes focus; the selected room stays named on the button that brings the list back.
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithText("Rom · Stova").assertExists()
        compose.onNodeWithText("Rom · Stova").performClick()
        assertTrue(open)
        compose.onNodeWithTag("sonos-room-${room.id}").assertIsFocused()
    }

    @Test fun hiddenRoomsLeaveEveryDailyControlOnScreenAndAnEmptyPageHasNoToggle() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(listOf(room.copy(entity = room.entity.copy(attributes = StovaJson.parseToJsonElement("""{
                "media_title":"Inspector Norse","media_artist":"Todd Terje","media_duration":480,"volume_level":0.3,"supported_features":1048575}""").jsonObject))),
                loading = false), room.id, {}, {}, {}, {}, {}, roomsOpen = false, onBrowseMusicAssistant = {})
        } }
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        listOf(compose.onNodeWithTag("slider-sonos-${room.id}"), compose.onNodeWithText("Fleire val"), compose.onNodeWithText("Music Assistant")).forEach { node ->
            node.assertIsDisplayed()
            assertTrue(node.getUnclippedBoundsInRoot().bottom <= root.bottom - 24.dp)
        }
        compose.onNodeWithText("Gøym rom").assertDoesNotExist()
    }

    @Test fun emptyOrOfflinePageOffersNoRoomToggle() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(emptyList(), loading = false, failed = true), null, {}, {}, {}, {}, {}, roomsOpen = false)
        } }
        compose.onNodeWithText("Gøym rom").assertDoesNotExist()
        compose.onNodeWithText("Rom", substring = true).assertDoesNotExist()
    }

    @Test fun moreOptionsOpenWithoutSendingActionsAndReturnFocusToTheirButton() {
        var action: SonosAction? = null
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosContent(SonosState(listOf(room), loading = false), room.id, {}, { action = it }, {}, {}, {})
        } }
        compose.onNodeWithText("Fleire val").performClick()
        compose.onNodeWithText("Tilfeldig av").assertIsDisplayed()
        assertNull(action)
        compose.onNodeWithText("Lukk").performClick()
        compose.onNodeWithText("Fleire val").assertIsFocused()
        assertNull(action)
    }

    @Test fun rightFromTheLastRoomEntersPlaybackAndLeftReturnsToTheSelectedRoom() {
        val rooms = (1..4).map { room.copy(entity = room.entity.copy(id = "media_player.room$it"), name = "Rom $it") }
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            SonosContent(SonosState(rooms, loading = false), rooms.last().id, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("sonos-room-${rooms.last().id}").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithContentDescription("Førre spor").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithTag("sonos-room-${rooms.last().id}").assertIsFocused()
    }

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

    @Test fun whenTheWidgetIsVisibleTheMusicHeroDoesNotRepeatItsLabels() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            CompositionLocalProvider(LocalHomeSonos provides room, LocalHomeSonosWidget provides true) {
                app.stova.home.HeroPanel(null, androidx.compose.ui.Modifier)
                SonosNowPlaying(room, room, false, false, {}, {})
            }
        } }
        compose.onAllNodesWithText("Northern Lights").assertCountEquals(1)
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
        compose.onNodeWithText("Spelar no · Stova").assertDoesNotExist()
        compose.onNodeWithTag("sonos-room-${room.id}").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("sonos-room-${room.id}").assertIsNotFocused()
        compose.onNodeWithContentDescription("Pause").performClick()
        assertEquals(SonosAction.PlayPause, action)
    }

    @Test fun unavailableRoomDoesNotOfferEnabledPlayback() {
        compose.setContent { StovaTheme {
            SonosContent(SonosState(listOf(room.copy(entity = room.entity.copy(state = "unavailable"))), loading = false), room.id, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithContentDescription("Spel").assertIsNotEnabled()
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

    private fun attrs(room: SonosRoom, vararg extra: Pair<String, String>) = room.copy(entity = room.entity.copy(attributes =
        kotlinx.serialization.json.JsonObject(room.entity.attributes + extra.map { (k, v) -> k to kotlinx.serialization.json.JsonPrimitive(v) })))
    private val era = room.copy(model = "Era 300", manufacturer = "Sonos")
    private val plain = room.copy(entity = room.entity.copy(id = "media_player.loft", state = "paused"), name = "Loft")
    private fun active(id: String, value: Boolean) =
        compose.onNodeWithTag("sonos-speaker-$id", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SonosSpeakerActive, value))

    @Test fun roomsShowTheirSpeakerAndModelOnlyWhenTheRegistryKnowsIt() {
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(listOf(era, plain), loading = false), era.id, {}, {}, {}, {}, {})
        } }
        compose.onAllNodesWithText("Era 300", substring = true, useUnmergedTree = true).onFirst().assertExists()
        compose.onNodeWithTag("sonos-speaker-${plain.id}", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("sonos-speaker-${era.id}", useUnmergedTree = true).assertExists()
        // Reduced motion: the playing speaker stays still.
        active(era.id, false)
    }

    @Test fun speakerMovesOnlyWhileMusicPlays() {
        val tv = attrs(room.copy(entity = room.entity.copy(id = "media_player.tv"), name = "TV-rom"), "source" to "TV")
        compose.mainClock.autoAdvance = false
        compose.setContent { StovaTheme(reduceMotion = false) {
            SonosContent(SonosState(listOf(era, plain, tv), loading = false), era.id, {}, {}, {}, {}, {})
        } }
        compose.mainClock.advanceTimeBy(500)
        active(era.id, true)
        active(plain.id, false)
        active(tv.id, false)
    }

    /** Lay out, let the main looper deliver the throttled visibility callbacks, then recompose. */
    private fun settle() {
        repeat(3) {
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(200)
            org.robolectric.shadows.ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
    }

    @Test fun speakerStopsWhenScrolledOutOfSightAndResumesOnReturn() {
        val scroll = androidx.compose.foundation.ScrollState(0)
        compose.mainClock.autoAdvance = false
        compose.setContent { StovaTheme(reduceMotion = false) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.height(200.dp).verticalScroll(scroll)) {
                SonosSpeakerArt(era.speaker, rememberSonosVisual(era, SonosState(listOf(era), loading = false)),
                    androidx.compose.ui.Modifier.size(120.dp, 80.dp), "sonos-speaker-${era.id}")
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(1000.dp))
            }
        } }
        compose.mainClock.advanceTimeBy(200)
        active(era.id, true)
        compose.runOnIdle { scroll.dispatchRawDelta(600f) }
        settle()
        active(era.id, false)
        compose.runOnIdle { scroll.dispatchRawDelta(-600f) }
        settle()
        active(era.id, true)
    }

    @Test fun speakerIsStillUnlessTheScreenIsResumed() {
        val owner = object : androidx.lifecycle.LifecycleOwner {
            val registry = androidx.lifecycle.LifecycleRegistry.createUnsafe(this)
            override val lifecycle get() = registry
        }
        owner.registry.currentState = androidx.lifecycle.Lifecycle.State.STARTED
        compose.mainClock.autoAdvance = false
        compose.setContent { StovaTheme(reduceMotion = false) {
            CompositionLocalProvider(androidx.lifecycle.compose.LocalLifecycleOwner provides owner) {
                SonosSpeakerArt(era.speaker, rememberSonosVisual(era, SonosState(listOf(era), loading = false)),
                    androidx.compose.ui.Modifier.size(120.dp, 80.dp), "sonos-speaker-${era.id}")
            }
        } }
        compose.mainClock.advanceTimeBy(200)
        active(era.id, false)
        compose.runOnIdle { owner.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(200)
        active(era.id, true)
        compose.runOnIdle { owner.registry.currentState = androidx.lifecycle.Lifecycle.State.STARTED }
        compose.mainClock.advanceTimeBy(200)
        active(era.id, false)
    }

    @Test fun groupDialogShowsEachRoomsOwnSpeaker() {
        val sub = room.copy(entity = room.entity.copy(id = "media_player.sub", state = "idle"), name = "Kjellar", model = "Sub Mini")
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosGroupDialog(era, listOf(era, sub), false, false, {}, {})
        } }
        compose.onAllNodesWithText("Sub Mini", substring = true, useUnmergedTree = true).onFirst().assertExists()
        compose.onNodeWithTag("sonos-speaker-${sub.id}", useUnmergedTree = true).assertExists()
    }
}
