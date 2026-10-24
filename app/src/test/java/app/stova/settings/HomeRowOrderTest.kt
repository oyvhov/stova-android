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
