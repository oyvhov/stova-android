package app.stova.kids

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import app.stova.profiles.Profile
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KidsHomeTest {
    @get:Rule val compose = createComposeRule()
    private val child = Profile("k", "Ola", AvatarColor.LIME, kid = true)
    private val account = MediaAccount("a", "k", ServerKind.EMBY, "s", "Heim", "https://demo.invalid", "u", "Ola")
    private val rows = listOf(HomeRowUi.Media(HomeRowKey.continueWatching("a"), account, MediaRowKind.CONTINUE, listOf(
        MediaItem("e1", "a", ItemType.EPISODE, "Episode 4", seriesName = "Bluey", progress = 0.4f),
        MediaItem("m1", "a", ItemType.MOVIE, "Kaptein Sabeltann")), false))
    private val halloween = LocalDate.of(2026, 10, 29)

    private fun show(rows: List<HomeRowUi>?, time: LocalTime = LocalTime.NOON, profile: Profile = child, spoken: MutableList<String>? = null) =
        compose.setContent { StovaTheme(reduceMotion = true) {
            // A TV starts with the remote, not touch; Robolectric starts with touch.
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            KidsHomeContent(profile, rows, null, halloween, time, onPlay = {}, onApp = {}, onProfile = {},
                onSpeak = spoken?.let { list -> { text: String -> list += text } })
        } }

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Test fun pipDrawsEveryMoodInEveryOutfit() {
        compose.setContent { StovaTheme { FlowRow { for (mood in PipMood.entries) for (outfit in PipOutfit.entries)
            Pip(mood, outfit, androidx.compose.runtime.remember { PipMotion() }, Modifier.size(50.dp, 75.dp)) } } }
        compose.waitForIdle()
    }

    @Test fun everySeasonHasItsOwnOutfit() {
        assertEquals(KidsSeason.entries.size, KidsSeason.entries.map { it.outfit() }.toSet().size)
        assertTrue(PipOutfit.NIGHTCAP !in KidsSeason.entries.map { it.outfit() })
    }

    @Test fun pipGreetsTheChildThenSaysTheNameOfWhateverIsInFocus() {
        val spoken = mutableListOf<String>()
        show(rows, spoken = spoken)
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals("Hei, Ola!", spoken.first())
        // In his own bubble, beside him: up by the avatar his hat would cover it.
        compose.onNodeWithTag("pip-says").assertTextEquals("Hei, Ola!")
        compose.onNodeWithText("Hei, Ola!").assertExists()
        // Episodes go by the name of their series: that is what a child knows them by.
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionLeft) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithTag("pip-says").assertTextEquals("Bluey")
        assertEquals("Bluey", spoken.last())
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithTag("pip-says").assertTextEquals("Kaptein Sabeltann")
        assertEquals("Kaptein Sabeltann", spoken.last())
    }

    @Test fun pipStaysQuietWhenTheParentsHaveTurnedHisVoiceOff() {
        val spoken = mutableListOf<String>()
        show(rows, profile = child.copy(pip = PipPrefs(readAloud = false)), spoken = spoken)
        compose.mainClock.advanceTimeBy(3_000)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(2_000)
        assertEquals(emptyList<String>(), spoken)
    }

    @Test fun withPipOffTheNameStillShowsOverTheCard() {
        show(rows, profile = child.copy(pip = PipPrefs(shown = false)))
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionLeft) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithTag("pip-says").assertTextEquals("Bluey")
        // The row's own name moves into the bubble while the row has focus (its label fades but stays for TalkBack).
        compose.onAllNodesWithText("Hald fram").assertCountEquals(2)
    }

    @Test fun aNewEpisodeWearsItsTag() {
        val fresh = listOf(HomeRowUi.Media(HomeRowKey.latestSeries("a"), account, MediaRowKind.LATEST_SERIES,
            listOf(MediaItem("s1", "a", ItemType.SERIES, "Bluey", isNew = true)), false))
        show(fresh)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Nytt!").assertExists()
    }

    // A first visit has no snapshot: "Continue" can arrive after the other rows and lands above the one in focus.
    @Test fun aRowArrivingAboveTheFocusedOneIsScrolledIntoTheSkyBandNotUnderTheHeader() {
        val latest = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES,
            listOf(MediaItem("m2", "a", ItemType.MOVIE, "Flåklypa")), false)
        val current = androidx.compose.runtime.mutableStateOf<List<HomeRowUi>>(listOf(latest))
        compose.setContent { StovaTheme(reduceMotion = true) {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            KidsHomeContent(child, current.value, null, halloween, LocalTime.NOON, onPlay = {}, onApp = {}, onProfile = {})
        } }
        compose.mainClock.advanceTimeBy(1_000)
        current.value = rows + latest
        compose.mainClock.advanceTimeBy(2_000)
        val card = compose.onNodeWithContentDescription("Bluey").getUnclippedBoundsInRoot()
        assertTrue("the new first row starts in the sky band, below the header: ${card.top}", card.top >= 150.dp)
    }

    @Test fun afterBedtimePipSleepsAndSaysGoodnight() {
        show(rows, LocalTime.of(20, 0), child.copy(pip = PipPrefs(bedtime = 19 * 60)))
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("God natt, Ola!").assertExists()
        compose.onNodeWithTag("pip-says").assertTextEquals("Pip søv. Sov godt!")
    }

    @Test fun pipSaysWhenThereIsNothingYetAndWhileItIsComing() {
        show(null)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("pip-says").assertTextEquals("Pip hentar filmane dine …")
    }

    @Test fun anEmptyHomeAsksForAGrownUp() {
        show(emptyList())
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("pip-says").assertTextEquals("Her er det tomt enno. Spør ein vaksen.")
    }
}
