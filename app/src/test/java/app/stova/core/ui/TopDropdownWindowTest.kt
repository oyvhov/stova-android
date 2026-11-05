package app.stova.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class TopDropdownWindowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun theNativePopupKeepsItsBoundsUntilTheCallerRemovesIt() {
        var dismissed = 0
        compose.setContent { StovaTheme { Box {
            TopDropdown({ dismissed++ }, 220.dp) { StovaButton("Meny", {}) }
        } } }
        val before = compose.onNode(isPopup()).getUnclippedBoundsInRoot()
        compose.onNodeWithText("Meny").requestFocus().performKeyInput { pressKey(Key.Back) }
        compose.waitForIdle()
        assertEquals(1, dismissed)
        assertEquals(before, compose.onNode(isPopup()).getUnclippedBoundsInRoot())
    }
}
