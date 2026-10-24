package app.stova.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.PinCheck
import app.stova.saveScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS, fontScale = 2f)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PinLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun deleteLabelFitsOneLineAtDoubleFontScale() {
        compose.setContent { StovaTheme { PinPanel("PIN", { PinCheck.Rejected(0) }, {}) } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Slett", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertFalse(layouts.single().didOverflowWidth)
        compose.saveScreen("minor-pin-font-2")
    }
}
