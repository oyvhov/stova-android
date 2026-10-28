package app.stova.ha

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
class HaIconPickerTest {
    @get:Rule val compose = createComposeRule()

    // Before a search, icons a house often uses; a search finds the rest by Home Assistant's own
    // name, the exact name first.
    @Test fun searchFindsAnIconByItsHomeAssistantName() {
        var selected: String? = null
        compose.setContent { StovaTheme(reduceMotion = true) { HaIconPicker("mdi:movie-open", { selected = it }, {}) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("lightbulb").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Søk på engelsk", substring = true).performTextInput("cat")
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("cat") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("lightbulb").assertDoesNotExist()
        val icon = compose.onNode(hasText("cat") and !hasSetTextAction())
        icon.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        icon.performKeyInput { pressKey(Key.Enter) }
        assertEquals("mdi:cat", selected)
    }
}
