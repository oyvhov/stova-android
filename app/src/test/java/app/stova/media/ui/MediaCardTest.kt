package app.stova.media.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.media.model.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class) @Config(sdk=[30], qualifiers=TV_QUALIFIERS)
class MediaCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dpadMovesFocusAndEnterAndMenuInvokeActions() {
        var focused = ""
        var opened = ""
        var menu = ""
        compose.setContent {
            val first = remember { FocusRequester() }
            StovaTheme {
                Row {
                    listOf("First", "Second").forEachIndexed { i, title ->
                        MediaCard(MediaItem(title, "a", ItemType.MOVIE, title), false,
                            { opened = title }, { menu = title }, { focused = title },
                            if (i == 0) Modifier.focusRequester(first) else Modifier)
                    }
                }
            }
            LaunchedEffect(Unit) { first.requestFocus() }
        }
        compose.onNodeWithText("First").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("Second").assertIsFocused()
        assertEquals("Second", focused)
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }
        assertEquals("Second", opened)
        compose.onRoot().performKeyInput { pressKey(Key.Menu) }
        assertEquals("Second", menu)
    }

    @Test fun episodeCardNamesTheSeriesWithoutAnEpisodeCode() {
        compose.setContent {
            StovaTheme {
                MediaCard(MediaItem("e1", "a", ItemType.EPISODE, "Pilot", seriesName = "Kystvakta", season = 1, episode = 2),
                    false, {}, {}, {})
            }
        }
        compose.onNodeWithText("Kystvakta").assertIsDisplayed()
        compose.onAllNodesWithText("E2", substring = true).assertCountEquals(0)
    }
}
