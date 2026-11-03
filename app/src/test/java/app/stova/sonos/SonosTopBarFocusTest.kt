package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import app.stova.ha.cards.CardType
import app.stova.ha.ui.ChipContent
import app.stova.ha.ui.HomeStatusChips
import app.stova.navigation.TopBar
import app.stova.profiles.Profile
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Found on the Shield: Right from the widget's title skipped its own play button and the light chip
 * and landed on the media chip. The whole top bar, as Home builds it, with D-pad keys.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosTopBarFocusTest {
    @get:Rule val compose = createComposeRule()
    private val room = SonosRoom(HaEntity("media_player.stova", "playing", StovaJson.parseToJsonElement(
        """{"media_title":"Inspector Norse","media_artist":"Todd Terje","supported_features":1048575}""").jsonObject, 0, 0), "Stova")

    /** Ordinary motion, as on the Shield: the focused control grows a little (lift and 1.02 zoom). */
    private fun topBar(appearance: app.stova.profiles.AppearancePrefs = app.stova.profiles.AppearancePrefs()) = compose.setContent {
        StovaTheme(reduceMotion = false, appearance = appearance) {
        val input = androidx.compose.ui.platform.LocalInputModeManager.current
        SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            // As HomeContent wraps the top field: one restoring focus group.
            Box(Modifier.focusRestorer().focusGroup()) {
            TopBar(Profile("p", "Øyvind", AvatarColor.LIME), {}, {}, onSonos = {},
                nowPlaying = { modifier -> SonosNowPlaying(room, room, false, false, {}, {}, modifier) },
                leading = {
                    HomeStatusChips(listOf(
                        ChipContent(CardType.LIGHT, "3 lys", cardId = "light.stova", name = "Lys"),
                        ChipContent(CardType.MEDIA_PLAYER, "Lydplanke", cardId = "media_player.tv", name = "Media")), onOpenCard = {}) { _, _ -> }
                })
            }
        }
    } }
    private fun right() { compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }; compose.mainClock.advanceTimeBy(1000) }
    private fun left() { compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }; compose.mainClock.advanceTimeBy(1000) }
    private val title get() = compose.onNode(hasText("Inspector Norse") and hasClickAction(), useUnmergedTree = false)

    @Test fun rightFromTheWidgetTitleVisitsPlayThenTheLightThenTheMediaChip() {
        topBar()
        title.requestFocus()
        compose.mainClock.advanceTimeBy(1000)
        title.assertIsFocused()
        right()
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        right()
        compose.onNodeWithContentDescription("Lys, 3 lys").assertIsFocused()
        right()
        compose.onNodeWithContentDescription("Media, Lydplanke").assertIsFocused()
    }

    @Test fun sameWithChipsOnTheLeftInHalloween() {
        topBar(app.stova.profiles.AppearancePrefs(chipPlacement = app.stova.profiles.ChipPlacement.LEFT,
            seasonChoice = app.stova.profiles.SeasonChoice.HALLOWEEN))
        title.requestFocus()
        compose.mainClock.advanceTimeBy(1000)
        right()
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        right()
        compose.onNodeWithContentDescription("Lys, 3 lys").assertIsFocused()
    }

    @Test fun leftFromTheMediaChipWalksBackThroughTheLightAndPlay() {
        topBar()
        compose.onNodeWithContentDescription("Media, Lydplanke").requestFocus()
        compose.mainClock.advanceTimeBy(1000)
        left()
        compose.onNodeWithContentDescription("Lys, 3 lys").assertIsFocused()
        left()
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        left()
        title.assertIsFocused()
    }
}
