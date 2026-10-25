package app.stova.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import app.stova.media.SpotlightEntry
import org.junit.Assert.assertEquals
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeScrollTest {
    @get:Rule val compose = createComposeRule()

    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private fun row(key: HomeRowKey, title: String) = HomeRowUi.Media(key, account, MediaRowKind.CONTINUE,
        listOf(MediaItem("$title-id", "a", ItemType.MOVIE, title, progress = 0.4f)), false)
    private val rows = listOf(row(HomeRowKey.continueWatching("a"), "Fyrste"), row(HomeRowKey.latestMovies("a"), "Andre"),
        row(HomeRowKey.latestSeries("a"), "Tredje"))

    private fun render(focused: FocusedTarget? = null) = compose.setContent {
        val context = LocalContext.current
        StovaTheme(reduceMotion = true, appearance = app.stova.profiles.AppearancePrefs(homeRows = app.stova.profiles.HomeRows.SCROLLING)) {
            HomeContent(rows, focused, rows.first().key, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                {}, {}, {}, {}, topBar = { app.stova.core.ui.StovaButton("Toppen", {}) },
                spotlight = SpotlightEntry(MediaItem("hero", "a", ItemType.MOVIE, "Heroen"), account, false))
        }
    }

    @Test fun startRowTakesFocusWhenRowsFirstAppear() {
        render()
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Fyrste", substring = true))
    }

    @Test fun movingDownReclaimsHeroSpaceAndMovingUpRestoresIt() {
        render()
        compose.onNodeWithText("Heroen").assertIsDisplayed()
        val firstTop = compose.onNodeWithText("Fyrste").getUnclippedBoundsInRoot().top
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithText("Heroen").assertIsNotDisplayed()
        val secondTop = compose.onNodeWithText("Andre").getUnclippedBoundsInRoot().top
        org.junit.Assert.assertTrue("the row uses the hero's former space", secondTop < firstTop)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithText("Heroen").assertIsDisplayed()
        compose.onNode(isFocused()).assert(hasText("Fyrste", substring = true))
    }

    @Test fun returningToTheTopBarRevealsTheHero() {
        render()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Heroen").assertIsNotDisplayed()
        compose.onNodeWithText("Toppen").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithText("Heroen").assertIsDisplayed()
        compose.onNodeWithText("Toppen").assertIsFocused()
    }

    @Test fun focusedRowSnapsToTheTopWithoutThePreviousRowPeeking() {
        render()
        compose.onNodeWithText("Fyrste").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Andre", substring = true))
        // Minimal bring-into-view left the bottom of the card above showing as a bare bar under the hero.
        compose.onNodeWithText("Fyrste").assertIsNotDisplayed()
    }

    @Test fun startFocusMovesToTheFirstMediaRowWhenItArrivesLate() {
        var shown by mutableStateOf(listOf(rows[1]))
        var start by mutableStateOf(rows[1].key)
        compose.setContent {
            val context = LocalContext.current
            StovaTheme(reduceMotion = true) {
                HomeContent(shown, null, start, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), {}, {}, {}, {})
            }
        }
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Andre", substring = true))
        compose.runOnIdle { shown = rows; start = rows[0].key }
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Fyrste", substring = true))
    }

    @Test fun startFocusStaysWhereTheUserWent() {
        var shown by mutableStateOf(rows.drop(1))
        var start by mutableStateOf(rows[1].key)
        compose.setContent {
            val context = LocalContext.current
            StovaTheme(reduceMotion = true) {
                HomeContent(shown, null, start, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), {}, {}, {}, {})
            }
        }
        compose.waitForIdle()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Tredje", substring = true))
        compose.runOnIdle { shown = rows; start = rows[0].key }
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Tredje", substring = true))
    }

    @Test fun homeShowsNoServiceLogo() {
        render(FocusedTarget.Media(rows.first().items.first(), account))
        compose.waitForIdle()
        // The server name stays in each row header; only the logo goes.
        compose.onAllNodesWithText("Demo").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Jellyfin").assertCountEquals(0)
    }
}
