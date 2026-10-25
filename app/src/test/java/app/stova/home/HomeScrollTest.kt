package app.stova.home

import androidx.compose.ui.input.key.Key
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
        StovaTheme(reduceMotion = true) {
            HomeContent(rows, focused, rows.first().key, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                {}, {}, {}, {})
        }
    }

    @Test fun startRowTakesFocusWhenRowsFirstAppear() {
        render()
        compose.waitForIdle()
        compose.onNode(isFocused()).assert(hasText("Fyrste", substring = true))
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

    @Test fun homeShowsNoServiceLogo() {
        render(FocusedTarget.Media(rows.first().items.first(), account))
        compose.waitForIdle()
        // The server name stays in the hero and in each row header; only the logo goes.
        compose.onAllNodesWithText("Demo").assertCountEquals(3)
        compose.onAllNodesWithContentDescription("Jellyfin").assertCountEquals(0)
    }
}
