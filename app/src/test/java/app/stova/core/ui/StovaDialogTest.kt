package app.stova.core.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaMotion
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaDialogTest {
    @get:Rule val compose = createComposeRule()
    private var dismissed = 0

    private fun show(reduceMotion: Boolean = false) {
        compose.setContent {
            StovaTheme(reduceMotion = reduceMotion) {
                var open by remember { mutableStateOf(true) }
                if (open) StovaDialog({ open = false; dismissed++ }) {
                    val first = remember { FocusRequester() }
                    LaunchedEffect(Unit) { first.requestFocus() }
                    StovaButton("Lukk", { open = false }, Modifier.focusRequester(first))
                }
            }
        }
    }

    private fun pressBack() = compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }

    private fun ok(action: Int, repeat: Int = 0, key: Int = android.view.KeyEvent.KEYCODE_DPAD_CENTER) {
        compose.runOnIdle {
            val now = android.os.SystemClock.uptimeMillis()
            ShadowDialog.getLatestDialog().dispatchKeyEvent(android.view.KeyEvent(
                now, now, action, key, repeat,
            ))
        }
        compose.waitForIdle()
    }

    @Test fun heldOkFromTheOpeningWindowCannotChooseTheFirstAction() {
        openingHoldDoesNotChoose(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
    }

    @Test fun heldEnterFromTheOpeningWindowCannotChooseTheFirstAction() {
        openingHoldDoesNotChoose(android.view.KeyEvent.KEYCODE_ENTER)
    }

    @Test fun heldNumpadEnterFromTheOpeningWindowCannotChooseTheFirstAction() {
        openingHoldDoesNotChoose(android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
    }

    private fun openingHoldDoesNotChoose(key: Int) {
        show()
        ok(android.view.KeyEvent.ACTION_DOWN, repeat = 3, key = key)
        ok(android.view.KeyEvent.ACTION_UP, key = key)
        compose.onNodeWithText("Lukk").assertIsDisplayed()
        // No timeout: the very next deliberate OK is still accepted.
        ok(android.view.KeyEvent.ACTION_DOWN, key = key)
        ok(android.view.KeyEvent.ACTION_UP, key = key)
        compose.onNodeWithText("Lukk").assertDoesNotExist()
    }

    @Test fun releasingAnOkThatStartedElsewhereDoesNotChooseAnAction() {
        show()
        ok(android.view.KeyEvent.ACTION_UP)
        compose.onNodeWithText("Lukk").assertIsDisplayed()
        ok(android.view.KeyEvent.ACTION_DOWN)
        ok(android.view.KeyEvent.ACTION_UP)
        compose.onNodeWithText("Lukk").assertDoesNotExist()
    }

    @Test fun anOkPressThatStartsInsideTheDialogStillWorksWhenHeld() {
        show()
        ok(android.view.KeyEvent.ACTION_DOWN)
        ok(android.view.KeyEvent.ACTION_DOWN, repeat = 1)
        ok(android.view.KeyEvent.ACTION_DOWN, repeat = 2)
        ok(android.view.KeyEvent.ACTION_UP)
        compose.onNodeWithText("Lukk").assertDoesNotExist()
    }

    @Test fun contentIsThereFromTheFirstFrameAndTakesFocus() {
        show()
        compose.onNodeWithText("Lukk").assertIsFocused()
    }

    @Test fun backPlaysTheExitThenCloses() {
        show()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        pressBack()
        // The panel is still leaving: the caller has not heard about it yet.
        assertEquals(0, dismissed)
        compose.onNodeWithText("Lukk").assertExists()
        compose.mainClock.advanceTimeBy(StovaMotion.QuickMillis + 100L)
        assertEquals(1, dismissed)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Lukk").assertDoesNotExist()
    }

    @Test fun reducedMotionClosesOnBackAtOnce() {
        show(reduceMotion = true)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        pressBack()
        assertEquals(1, dismissed)
    }

    @Test fun aButtonInsideStillClosesTheDialog() {
        show()
        compose.onNodeWithText("Lukk").performClick()
        compose.onNodeWithText("Lukk").assertDoesNotExist()
        assertEquals(0, dismissed)
    }

    @Test fun touchingTheScrimClosesTheFullScreenWindow() {
        show(reduceMotion = true)
        compose.onNode(isDialog()).performTouchInput { click(Offset(2f, 2f)) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
    }
}
