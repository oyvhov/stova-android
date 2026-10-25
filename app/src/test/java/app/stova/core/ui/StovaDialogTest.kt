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
}
