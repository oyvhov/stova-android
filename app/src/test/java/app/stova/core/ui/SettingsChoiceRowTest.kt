package app.stova.core.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
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
class SettingsChoiceRowTest {
    @get:Rule val compose = createComposeRule()

    // Review finding 3: Left changed the value, so the section list on the left was unreachable.
    @Test fun leftLeavesTheRowAndRightSteps() {
        val steps = mutableListOf<Int>()
        compose.setContent { StovaTheme { Row {
            Text("Meny", Modifier.size(120.dp).testTag("menu").focusable())
            SettingsChoiceRow("Tema", "Skog") { steps += it }
        } } }
        compose.onNodeWithText("Tema").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(listOf(1), steps)
        compose.onNodeWithTag("menu").assertIsFocused()
    }
}
