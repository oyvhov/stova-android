package app.stova.home

import androidx.compose.runtime.*
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
import app.stova.profiles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class RowEditingTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private val first = HomeRowKey.continueWatching("a")
    private val second = HomeRowKey.latestMovies("a")
    private val all = listOf(first, second).mapIndexed { i, key -> HomeRowUi.Media(key, account,
        if (i == 0) MediaRowKind.CONTINUE else MediaRowKind.LATEST_MOVIES,
        listOf(MediaItem("$i", "a", ItemType.MOVIE, "Film $i")) +
            if (i == 0) listOf(MediaItem("extra", "a", ItemType.MOVIE, "Ekstra")) else emptyList(), false) }

    private fun render(onSaved: (HomePrefs) -> Unit = {}) {
        compose.setContent {
            val context = LocalContext.current
            var home by remember { mutableStateOf(HomePrefs()) }
            val rows = arrangeRows(all.map { it.key }, home).map { key -> all.first { it.key == key } }
            StovaTheme(reduceMotion = true) {
                HomeContent(rows, null, first, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                    {}, {}, {}, {}, homePrefs = home, onHomeChange = { home = it; onSaved(it) }, canEditRows = true)
            }
        }
        compose.waitForIdle()
    }

    private fun menu() {
        compose.onNodeWithContentDescription("Film 0").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { pressKey(Key.Menu) }
    }

    @Test fun menuMovesWholeRowWithRemoteAndBackEndsMove() {
        var saved = HomePrefs()
        render { saved = it }
        menu()
        compose.onNodeWithText("Flytt rada").performClick()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(listOf(second, first), saved.rowOrder)
        compose.onNodeWithContentDescription("Film 0").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Back) }
        compose.onNodeWithText("Opp/ned flyttar rada · OK eller Tilbake avsluttar").assertDoesNotExist()
        compose.onNodeWithContentDescription("Film 0").assertIsFocused()
    }

    @Test fun hideAndUndoRestoreRowAndFocus() {
        var saved = HomePrefs()
        render { saved = it }
        menu()
        compose.onNodeWithText("Skjul rada").performClick()
        assertTrue(first in saved.hiddenRows)
        compose.onNodeWithContentDescription("Film 0").assertDoesNotExist()
        compose.onNodeWithText("Angre").performClick()
        assertFalse(first in saved.hiddenRows)
        compose.onNodeWithContentDescription("Film 0").assertIsFocused()
    }

    @Test fun rowChoicesPersistAndKeepOtherRowUntouched() {
        var saved = HomePrefs()
        render { saved = it }
        menu()
        compose.onNodeWithText("Tilpass rada").performClick()
        compose.onNodeWithText("Kortformat for rada").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Kortformat for rada").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("Storleik for rada").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Storleik for rada").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("Vis radtittel").performScrollTo().performClick()
        assertEquals(RowStyle(RowFormat.LARGE_WIDE, ArtworkSize.LARGE, false), saved.rowFormats[first])
        assertNull(saved.rowFormats[second])
    }

    @Test fun rowMenuShowsCommonActionsBeforeOpeningItsDetailedSettings() {
        render()
        menu()
        compose.onNodeWithText("Flytt rada").assertIsDisplayed()
        compose.onNodeWithText("Skjul rada").assertIsDisplayed()
        compose.onNodeWithText("Kortformat for rada").assertDoesNotExist()
        compose.onNodeWithText("Tilpass rada").performClick()
        compose.onNodeWithText("Kortformat for rada").assertExists()
        compose.onNodeWithText("Tilbake").performClick()
        compose.onNodeWithText("Kortformat for rada").assertDoesNotExist()
        compose.onNodeWithText("Tilpass rada").assertIsFocused()
    }

    @Test fun dismissingMenuAndFinishingMoveRestoreTheSameCard() {
        render()
        compose.onNodeWithContentDescription("Ekstra").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithContentDescription("Ekstra").performKeyInput { pressKey(Key.Menu) }
        compose.onNodeWithText("Flytt rada").performClick()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.Enter) }
        compose.onNodeWithContentDescription("Ekstra").assertIsFocused()
    }

    @Test fun undoRestoresTheHiddenCardEvenAfterFocusMovedToAnotherRow() {
        render()
        compose.onNodeWithContentDescription("Ekstra").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithContentDescription("Ekstra").performKeyInput { pressKey(Key.Menu) }
        compose.onNodeWithText("Skjul rada").performClick()
        compose.onNodeWithContentDescription("Film 1").assertIsFocused()
        compose.onNodeWithText("Angre").performClick()
        compose.onNodeWithContentDescription("Ekstra").assertIsFocused()
    }
}
