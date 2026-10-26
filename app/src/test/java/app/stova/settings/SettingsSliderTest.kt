package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.SettingsSliderRow
import app.stova.core.ui.StovaButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsSliderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun onlyTheEditingSliderOwnsArrowsAndEndpointsNeverWrap() {
        var value by mutableStateOf(1)
        compose.setContent { StovaTheme {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            Column {
                SettingsSliderRow("Opacity", value, 0..100, "$value %") { value = it }
                StovaButton("Next", {})
            }
        } }
        val slider = compose.onNodeWithText("Opacity")
        slider.performSemanticsAction(SemanticsActions.RequestFocus)
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(1, value)
        slider.performKeyInput { pressKey(Key.DirectionCenter) }
        slider.performKeyInput { pressKey(Key.DirectionLeft); pressKey(Key.DirectionLeft) }
        assertEquals(0, value); slider.assertIsFocused()
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(1, value)
        compose.mainClock.autoAdvance = false
        slider.performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionRight); pressKey(Key.DirectionRight) }
        assertEquals(4, value)
        compose.mainClock.autoAdvance = true
        slider.performKeyInput { pressKey(Key.Back) }
        slider.assertIsFocused()
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(4, value)
        slider.performKeyInput { pressKey(Key.Enter); pressKey(Key.DirectionDown) }
        compose.onNodeWithText("Next").assertIsFocused()
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(200f) }
        assertEquals(100, value)
    }
    @Test fun backFromAdjustmentStaysInsideTheAppearanceCategory() {
        compose.setContent { StovaTheme {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            AppearanceMenu { _, first -> SettingsSliderRow("Opacity", 50, 0..100, "50 %", first) {} }
        } }
        compose.onNode(hasText("Ikon") and hasClickAction()).performClick()
        compose.onNodeWithText("Opacity").assertIsFocused().performKeyInput { pressKey(Key.Enter); pressKey(Key.Back) }
        compose.onNodeWithText("Opacity").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Back) }
        compose.onNode(hasText("Ikon") and hasClickAction()).assertIsFocused()
    }
    @Test fun theTrackAcceptsDirectTouchWithoutChangingOtherControls() {
        var value by mutableStateOf(0)
        compose.setContent { StovaTheme {
            SettingsSliderRow("Opacity", value, 0..100, "$value %") { value = it }
        } }
        compose.onNodeWithText("Opacity").performTouchInput { click(center) }
        assertEquals(50, value)
        compose.onNodeWithText("Opacity").performTouchInput { swipe(center, centerRight) }
        assertEquals(100, value)
    }
    @Test fun verticalSwipeFromTheTrackScrollsThePageInsteadOfChangingTheValue() {
        var value by mutableStateOf(50)
        var scroll: androidx.compose.foundation.ScrollState? = null
        compose.setContent { StovaTheme {
            val state = rememberScrollState()
            SideEffect { scroll = state }
            Column(Modifier.height(180.dp).verticalScroll(state)) {
                SettingsSliderRow("Opacity", value, 0..100, "$value %") { value = it }
                repeat(8) { StovaButton("Next $it", {}) }
            }
        } }
        compose.onNodeWithText("Opacity").performTouchInput { swipe(center, topCenter) }
        assertEquals(50, value)
        compose.runOnIdle { assertTrue(scroll!!.value > 0) }
    }
}
