package app.stova.home

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import app.stova.media.*
import app.stova.media.model.*
import app.stova.profiles.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeDeckTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private val rows = listOf(
        HomeRowUi.Apps(HomeRowKey.APPS, listOf(InstalledApp("demo/.Main", "demo", ".Main", "Min app", true))),
        HomeRowUi.Media(HomeRowKey.continueWatching("a"), account, MediaRowKind.CONTINUE,
            listOf(MediaItem("film", "a", ItemType.MOVIE, "Film")), false),
    )
    private fun render(prefs: AppearancePrefs = AppearancePrefs(), third: Boolean = false, fontScale: Float = 1f, home: HomePrefs = HomePrefs()) {
        val data = if (third) rows + (rows[1] as HomeRowUi.Media).copy(key = HomeRowKey.latestMovies("a"),
            items = listOf(MediaItem("third", "a", ItemType.MOVIE, "Tredje"))) else rows
        compose.setContent {
            val density = LocalDensity.current
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            StovaTheme(reduceMotion = true, appearance = prefs) {
                val context = LocalContext.current
                HomeContent(data, null, HomeRowKey.APPS, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                    {}, {}, {}, {}, homePrefs = home,
                    spotlight = SpotlightEntry(MediaItem("spot", "a", ItemType.MOVIE, "Spotlight"), account, false))
            }
            }
        }
    }

    @Test fun twoRowChoiceShowsTwoWholeRowsWithoutAThirdPeeking() {
        render(AppearancePrefs(homeRows = HomeRows.TWO), third = true)
        compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithContentDescription("Film").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tredje").assertIsNotDisplayed()
    }

    @Test fun largeTextAndPosterOverridesKeepTheCardAndCaptionInsideTheScreen() {
        render(AppearancePrefs(homeRows = HomeRows.TWO, showTitles = true), third = true, fontScale = 2f,
            home = HomePrefs(rowFormats = mapOf(HomeRowKey.APPS to RowStyle(format = RowFormat.POSTER,
                size = ArtworkSize.LARGE, title = "Ei lang overskrift som kan gå over to linjer", headingSize = HeadingSize.LARGE))))
        val app = compose.onNode(hasContentDescription("Min app") and hasClickAction())
        app.assertIsDisplayed().assertIsFocused()
        val bounds = app.getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom <= compose.onRoot().getUnclippedBoundsInRoot().bottom)
        compose.onNodeWithContentDescription("Film").assertIsNotDisplayed()
    }
    @Test fun defaultShowsOneRowAtTheBottomWithoutTheNextRowPeekingIn() {
        render()
        val app = compose.onNode(hasContentDescription("Min app") and hasClickAction())
        app.assertIsDisplayed()
        compose.onNodeWithContentDescription("Film").assertIsNotDisplayed()
        val bottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("The single row belongs at the bottom", app.fetchSemanticsNode().boundsInRoot.bottom > bottom - 140f)
    }
    @Test fun changingTheBottomRowKeepsTheSpotlightAndHidesThePreviousRow() {
        render()
        compose.onNode(hasContentDescription("Min app") and hasClickAction()).performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithContentDescription("Film").assertIsFocused().assertIsDisplayed()
        compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsNotDisplayed()
        compose.onNodeWithText("Spotlight").assertIsDisplayed()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsFocused().assertIsDisplayed()
    }

    @Test fun heroOffStillKeepsTheSingleRowAtTheBottom() {
        render(AppearancePrefs(heroSize = HeroSize.OFF))
        compose.onNodeWithText("Spotlight").assertDoesNotExist()
        val app = compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsDisplayed()
        val bottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(app.fetchSemanticsNode().boundsInRoot.bottom > bottom - 140f)
    }
}
