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
    private fun render(prefs: AppearancePrefs = AppearancePrefs(), third: Boolean = false, fontScale: Float = 1f, home: HomePrefs = HomePrefs(),
        currentPrefs: () -> AppearancePrefs = { prefs }, initial: HomeRowKey = HomeRowKey.APPS,
        target: FocusedTarget? = null, duplicate: Boolean = false, longSource: Boolean = false) {
        val data = if (third) rows + (rows[1] as HomeRowUi.Media).copy(key = HomeRowKey.latestMovies("a"),
            kind = MediaRowKind.LATEST_MOVIES, items = if (duplicate) (rows[1] as HomeRowUi.Media).items else listOf(MediaItem("third", "a", ItemType.MOVIE, "Tredje"))) else rows
        val rendered = if (longSource) data.map { if (it is HomeRowUi.Media) it.copy(account = account.copy(serverName = "Eit svært langt namn på medietenaren i heimen vår"), offline = true) else it } else data
        compose.setContent {
            val density = LocalDensity.current
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            StovaTheme(reduceMotion = true, appearance = currentPrefs()) {
                val context = LocalContext.current
                HomeContent(rendered, target, initial, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
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
    @Test fun deckShowsNoRowCounterOrKeyHint() {
        render(third = true)
        compose.onNodeWithText("Opp/ned", substring = true).assertDoesNotExist()
        compose.onNodeWithText("1 / 3", substring = true).assertDoesNotExist()
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

    @Test fun liveModeChangesKeepTheSameVisibleFocusedRow() {
        var prefs by mutableStateOf(AppearancePrefs())
        render(third = true, currentPrefs = { prefs })
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.DirectionDown) }
        compose.onNodeWithContentDescription("Tredje").assertIsFocused().assertIsDisplayed()
        for (mode in listOf(HomeRows.TWO, HomeRows.SCROLLING, HomeRows.ONE)) {
            compose.runOnIdle { prefs = prefs.copy(homeRows = mode) }
            compose.onNodeWithContentDescription("Tredje").assertIsFocused().assertIsDisplayed()
        }
    }

    @Test fun duplicateMediaReturnsToTheRememberedRowInsteadOfTheFirstMatch() {
        render(third = true, duplicate = true, initial = HomeRowKey.latestMovies("a"),
            target = FocusedTarget.Media((rows[1] as HomeRowUi.Media).items[0], account))
        compose.onNodeWithText("Nyleg lagt til · Filmar").assertIsDisplayed()
        compose.onNodeWithText("Hald fram").assertIsNotDisplayed()
    }

    @Test fun longServerAndOfflineStateDoNotSqueezeTheDoubleFontRowHeading() {
        render(fontScale = 2f, longSource = true, initial = HomeRowKey.continueWatching("a"))
        compose.onNodeWithText("Hald fram").assertIsDisplayed()
        compose.onNodeWithContentDescription("Film").assertIsFocused().assertIsDisplayed()
        val heading = compose.onNodeWithText("Hald fram").getUnclippedBoundsInRoot()
        assertTrue("A short heading must have useful width", (heading.right - heading.left).value > 100f)
        val source = compose.onNodeWithText("Eit svært langt namn på medietenaren i heimen vår").getUnclippedBoundsInRoot()
        assertTrue("The measured and rendered source share a width cap", (source.right - source.left).value <= 200.5f)
    }
}
