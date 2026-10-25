package app.stova.home

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeroPanelTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Midttunet", "http://jf", "u", "U")

    @Test fun aLogoThatCannotBeLoadedStillShowsTheTitle() {
        val film = MediaItem("missing", "a", ItemType.MOVIE, "Fjordlys", art = ItemArt(logo = "missing-logo"))
        compose.setContent { StovaTheme(reduceMotion = true) { HeroPanel(SpotlightEntry(film, account, false)) } }
        compose.onNodeWithText("Fjordlys").assertIsDisplayed()
    }

    @Test fun newSeasonIsMarkedAndTheSeriesNamed() {
        val series = MediaItem("s1", "a", ItemType.SERIES, "Kystvakta", year = 2022, overview = "Livet langs kysten.")
        compose.setContent { StovaTheme(reduceMotion = true) { HeroPanel(SpotlightEntry(series, account, newSeason = true)) } }
        compose.onNodeWithText("Kystvakta").assertIsDisplayed()
        compose.onNodeWithText("Ny sesong").assertIsDisplayed()
        compose.onNodeWithText("Livet langs kysten.").assertDoesNotExist()
    }

    @Test fun filmShowsYearRuntimeAndRatingButNoServer() {
        val film = MediaItem("f1", "a", ItemType.MOVIE, "Fjordlys", year = 2024, runtimeMinutes = 112, ratings = Ratings(official = "12"))
        compose.setContent { StovaTheme(reduceMotion = true) { HeroPanel(SpotlightEntry(film, account, newSeason = false)) } }
        compose.onNodeWithText("2024").assertIsDisplayed()
        compose.onAllNodesWithText("12").assertCountEquals(1)
        compose.onNodeWithText("Ny sesong").assertDoesNotExist()
        compose.onNodeWithText("Midttunet").assertDoesNotExist()
    }

    @Test fun invalidCommunityScoreIsHidden() {
        val film = MediaItem("f", "a", ItemType.MOVIE, "Fjordlys", ratings = Ratings(39.0))
        compose.setContent { StovaTheme { HeroPanel(SpotlightEntry(film, account, false)) } }
        compose.onNodeWithText("39", substring = true).assertDoesNotExist()
    }

    @Test fun seriesShowsItsYearWithoutAnEpisodeRuntime() {
        val series = MediaItem("s1", "a", ItemType.SERIES, "Kystvakta", year = 2022, runtimeMinutes = 45)
        compose.setContent { StovaTheme(reduceMotion = true) { HeroPanel(SpotlightEntry(series, account, newSeason = false)) } }
        compose.onNodeWithText("2022").assertIsDisplayed()
        compose.onNodeWithText("45", substring = true).assertDoesNotExist()
    }

    @Test fun entireTextColumnStaysWithin400DpOn960DpScreen() {
        val title = "Fjordlys og den lange ferda gjennom fjordar og ukjende landskap"
        val overview = "Ei ferd gjennom stille fjordar og opne landskap. Vegen vidare byr på nye møte og ukjende stader."
        val item = MediaItem("m", "a", ItemType.MOVIE, title, year = 2024, runtimeMinutes = 112, overview = overview, ratings = Ratings(7.84, 91, "12"))
        compose.setContent { StovaTheme(reduceMotion = true) {
            Box(Modifier.fillMaxSize()) { HeroPanel(SpotlightEntry(item, account, newSeason = true), Modifier.fillMaxWidth()) }
        } }
        compose.onRoot().assertWidthIsEqualTo(960.dp)
        listOf(title, "Ny sesong", "2024", "★ 7,8", app.stova.media.ui.criticText(91, java.util.Locale.forLanguageTag("nn")), "12").forEach { text ->
            val bounds = compose.onNodeWithText(text).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("Hero text must be at most 400 dp wide: $text ($bounds)", bounds.right - bounds.left <= 400.dp)
            assertTrue("Hero text must stay inside the 400 dp column: $text ($bounds)", bounds.left >= 48.dp && bounds.right <= 448.dp)
        }
    }
}
