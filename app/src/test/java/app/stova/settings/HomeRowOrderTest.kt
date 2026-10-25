package app.stova.settings

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.home.HomeRowKey
import app.stova.home.moveItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeRowOrderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rowHeadingCanBeEditedWithoutLosingItsLayoutChoices() {
        var style by mutableStateOf(app.stova.profiles.RowStyle(format = app.stova.profiles.RowFormat.ICON))
        compose.setContent { StovaTheme {
            HomeRowOrderSettings(listOf(HomeRowKey.APPS), emptySet(), { _, _ -> }, { _, _ -> },
                rowFormats = mapOf(HomeRowKey.APPS to style), onStyle = { _, changed -> style = changed })
        } }
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Mine appar")
        compose.onNodeWithText("Lagre").performClick()
        org.junit.Assert.assertTrue(kotlinx.serialization.json.Json.encodeToString(app.stova.profiles.RowStyle.serializer(), style).contains("Mine appar"))
        org.junit.Assert.assertEquals(app.stova.profiles.RowFormat.ICON, style.format)
        compose.onAllNodesWithText("Mine appar").assertCountEquals(2)
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Ikkje lagra")
        compose.onNodeWithText("Avbryt").performClick()
        org.junit.Assert.assertEquals("Mine appar", style.title)
        compose.onNodeWithText("Endre overskrift").performClick()
        compose.onNodeWithText("Bruk standardoverskrift").performClick()
        org.junit.Assert.assertNull(style.title)
        org.junit.Assert.assertEquals(app.stova.profiles.RowFormat.ICON, style.format)
    }

    @Test fun focusFollowsRowMovedWithDpad() {
        compose.setContent {
            var rows by remember { mutableStateOf(listOf(HomeRowKey.APPS, HomeRowKey.WATCH_NEXT)) }
            StovaTheme {
                HomeRowOrderSettings(rows, emptySet(), { _, _ -> }, { key, delta -> rows = moveItem(rows, key, delta) })
            }
        }
        compose.onAllNodesWithText("Flytt ned")[0].performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onAllNodesWithText("Flytt ned")[0].performKeyInput {
            keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter)
        }
        compose.onAllNodesWithText("Flytt ned")[1].assertIsFocused()
    }
}
