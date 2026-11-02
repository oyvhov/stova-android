package app.stova.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Search on the Oppdag page: the field, Seerr's answer as posters six across, «Hent fleire» while there is more, and no hits said aloud. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class DiscoverSearchContentTest {
    @get:Rule val compose = createComposeRule()

    private fun title(tmdbId: Int, name: String) =
        DiscoverTitle(DiscoverType.MOVIE, tmdbId, name, 2020, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    private val found = listOf("Heat", "Heat 2", "The Heat", "Heatwave", "Dead Heat", "White Heat", "Heat Lightning")
        .mapIndexed { i, name -> title(100 + i, name) }

    private fun card(name: String) = compose.onNode(hasContentDescription(name) and hasClickAction())
    private val field get() = compose.onNode(hasSetTextAction())
    private val more = hasContentDescription("Hent fleire") and hasClickAction()

    private fun show(query: String, page: DiscoverPage?, onQuery: (String) -> Unit = {}, onOpenTitle: (DiscoverTitle) -> Unit = {}) =
        compose.setContent { StovaTheme(reduceMotion = true) { DiscoverSearchContent(query, page, false, onQuery, {}, onOpenTitle) } }

    @Test fun resultsShowAsPosters() {
        val opened = mutableListOf<DiscoverTitle>()
        show("Heat", DiscoverPage(found, 1, hasMore = false), onOpenTitle = { opened += it })
        compose.onNodeWithText("Søk").assertIsDisplayed()
        // The field has focus as the step opens, and Down goes from it to the first title.
        field.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("Heat").assertIsFocused()
        // Six posters across the safe area: the first six share a row from edge to edge, and the seventh starts the next.
        val bounds = found.map { card(it.title).getBoundsInRoot() }
        assertTrue(bounds.take(6).all { it.top == bounds[0].top })
        assertTrue(bounds[6].top > bounds[0].bottom)
        assertEquals(bounds[0].left, bounds[6].left)
        assertEquals(48f, bounds[0].left.value, 0.5f)
        assertEquals(960f - 48f, bounds[5].right.value, 0.5f)
        // Each card is a poster, half again as tall as it is wide, with the name under it.
        assertTrue(bounds[0].height > bounds[0].width * 1.5f)
        // OK opens the title.
        card("Heat").performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(found.first()), opened)
    }

    @Test fun moreTileAppearsOnlyWithMorePages() {
        var current by mutableStateOf(DiscoverPage(found.take(3), 1, hasMore = false))
        var asked = 0
        compose.setContent { StovaTheme(reduceMotion = true) { DiscoverSearchContent("Heat", current, false, {}, { asked++ }, {}) } }
        card("The Heat").assertIsDisplayed()
        compose.onAllNodes(more).assertCountEquals(0)
        compose.runOnIdle { current = current.copy(hasMore = true) }
        // The tile comes last: right after the last title.
        card("The Heat").performSemanticsAction(SemanticsActions.RequestFocus)
        card("The Heat").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNode(more).assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, asked)
        // The next page is added under the first, and its first title takes focus; with no page after it the tile is gone.
        compose.runOnIdle { current = DiscoverPage(found, 2, hasMore = false) }
        card("Heatwave").assertIsFocused()
        card("Heat").assertExists()
        compose.onAllNodes(more).assertCountEquals(0)
    }

    @Test fun emptyQueryShowsNoHitsText() {
        var query by mutableStateOf("Zzyzx")
        var loading by mutableStateOf(false)
        compose.setContent {
            StovaTheme(reduceMotion = true) { DiscoverSearchContent(query, DiscoverPage(emptyList(), 1, hasMore = false), loading, {}, {}, {}) }
        }
        compose.onNodeWithText("Ingen treff for «Zzyzx».").assertIsDisplayed()
        // While Seerr is asked, the page says so instead.
        compose.runOnIdle { loading = true }
        compose.onAllNodesWithText("Ingen treff", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Hentar frå Seerr …").assertIsDisplayed()
        // A query shorter than two characters is no search, and has no hits to tell of.
        compose.runOnIdle { loading = false; query = "Z" }
        compose.onAllNodesWithText("Ingen treff", substring = true).assertCountEquals(0)
    }

    @Test fun typingCallsOnQuery() {
        val typed = mutableListOf<String>()
        show("", null, onQuery = { typed += it })
        compose.onNodeWithText("Filmar og seriar").assertIsDisplayed()
        field.performTextInput("Dune")
        compose.runOnIdle { assertEquals("Dune", typed.last()) }
    }
}
