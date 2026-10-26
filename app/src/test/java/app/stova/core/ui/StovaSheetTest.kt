package app.stova.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaSheetTest {
    @get:Rule val compose = createComposeRule()
    private var dismissed = 0
    private var chosen = 0

    private fun show(reduceMotion: Boolean = true) {
        compose.setContent {
            StovaTheme(reduceMotion = reduceMotion) {
                var open by remember { mutableStateOf(true) }
                if (open) StovaSheet({ open = false; dismissed++ }) { pages ->
                    when (pages.current) {
                        null -> {
                            SheetHeader("Film", "2019")
                            StovaButton("Spel", { chosen++ }, primary = true)
                            SheetAction("Rad-val", StovaIcons.Rows, { pages.open("row") },
                                Modifier.focusRequester(pages.opener("row")), opensPage = true)
                        }
                        "row" -> SheetAction("Kortformat", StovaIcons.Rows, {})
                    }
                }
            }
        }
    }

    @Test fun focusStartsOnTheFirstActionAndThePanelHugsTheRightEdge() {
        show()
        compose.onNodeWithText("Spel").assertIsFocused()
        val panel = compose.onNodeWithText("Film").fetchSemanticsNode().boundsInRoot
        val root = compose.onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { it.boundsInRoot.right }
        val width = with(compose.density) { StovaLayout.SheetWidth.toPx() }
        // The title sits inside the panel, which takes the right-hand part of the screen.
        assertTrue(panel.left > root - width)
    }

    @Test fun aPageOpensInsideThePanelAndBackReturnsToTheRowThatOpenedIt() {
        show()
        compose.onNodeWithText("Rad-val").performClick()
        compose.onNodeWithText("Kortformat").assertIsFocused()
        compose.onNodeWithText("Spel").assertDoesNotExist()
        compose.onNodeWithText("Kortformat").performKeyInput { pressKey(Key.Back) }
        compose.onNodeWithText("Rad-val").assertIsFocused()
        assertEquals(0, dismissed)
        compose.onNodeWithText("Rad-val").performKeyInput { pressKey(Key.Back) }
        compose.onNodeWithText("Rad-val").assertDoesNotExist()
        assertEquals(1, dismissed)
    }

    @Test fun theBackRowOnAPageWorksLikeBack() {
        show()
        compose.onNodeWithText("Rad-val").performClick()
        compose.onNodeWithText("Tilbake").performClick()
        compose.onNodeWithText("Rad-val").assertIsFocused()
    }

    @Test fun systemBackAlsoStepsOnePageAtATime() {
        show()
        compose.onNodeWithText("Rad-val").performClick()
        compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }
        compose.onNodeWithText("Rad-val").assertIsFocused()
        compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun theHeldOkThatOpenedThePanelChoosesNothing() {
        show()
        fun ok(action: Int, repeat: Int = 0) {
            compose.runOnIdle {
                val now = android.os.SystemClock.uptimeMillis()
                ShadowDialog.getLatestDialog().dispatchKeyEvent(android.view.KeyEvent(now, now, action,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, repeat))
            }
            compose.waitForIdle()
        }
        ok(android.view.KeyEvent.ACTION_DOWN, repeat = 3)
        ok(android.view.KeyEvent.ACTION_UP)
        assertEquals(0, chosen)
        ok(android.view.KeyEvent.ACTION_DOWN)
        ok(android.view.KeyEvent.ACTION_UP)
        assertEquals(1, chosen)
    }

    @Test fun backSlidesThePanelOutBeforeItCloses() {
        show(reduceMotion = false)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }
        // The slide needs layout passes, which a paused test clock does not run; the caller waits.
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, dismissed)
        compose.onNodeWithText("Spel").assertExists()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(1, dismissed)
        compose.onNodeWithText("Spel").assertDoesNotExist()
    }
}
