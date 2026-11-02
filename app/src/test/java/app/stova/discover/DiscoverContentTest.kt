package app.stova.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/** The Oppdag page: its rows in order, a hero that follows the card in focus, and what it says when Seerr fails or only lets the person look. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class DiscoverContentTest {
    @get:Rule val compose = createComposeRule()

    private fun title(type: DiscoverType, tmdbId: Int, name: String) =
        DiscoverTitle(type, tmdbId, name, 2025, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    private val heat = DiscoverTitle(DiscoverType.MOVIE, 949, "Heat", 1995, "Ein gjeng ranarar og politimannen som jagar dei.", null, null,
        SeerrStatus.AVAILABLE, 8.3, listOf("Action", "Krim", "Drama", "Thriller"))
    private val rows = listOf(
        DiscoverRow(DiscoverRowKind.TRENDING, listOf(title(DiscoverType.MOVIE, 693134, "Dune: Part Two"), heat)),
        DiscoverRow(DiscoverRowKind.UPCOMING_MOVIES, listOf(title(DiscoverType.MOVIE, 696506, "Mickey 17"))),
        DiscoverRow(DiscoverRowKind.UPCOMING_SERIES, listOf(title(DiscoverType.SERIES, 95396, "Severance"))),
    )

    private fun state(access: SeerrAccess = SeerrAccess.PERSONAL, rows: List<DiscoverRow> = this.rows, failed: Boolean = false) =
        DiscoverState(access, rows, emptyList(), loading = false, failed = failed)

    private fun show(state: DiscoverState?, onOpenSearch: () -> Unit = {}, onRetry: () -> Unit = {}) = compose.setContent {
        StovaTheme(reduceMotion = true) { DiscoverContent(state, {}, onOpenSearch, {}, onRetry) }
    }

    private fun card(name: String) = compose.onNode(hasContentDescription(name) and hasClickAction())
    private val inHero = hasAnyAncestor(hasTestTag("discover-hero"))

    @Test fun rowsShowTheirHeadingsAndTitles() {
        show(state())
        compose.onNodeWithText("Populært no").assertIsDisplayed()
        card("Heat").assertIsDisplayed()
        // The page opens on the first popular title, and Down walks the rows in their order.
        card("Dune: Part Two").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("Mickey 17").assertIsFocused()
        compose.onNodeWithText("Komande filmar").assertExists()
        card("Mickey 17").performKeyInput { pressKey(Key.DirectionDown) }
        card("Severance").assertIsFocused()
        compose.onNodeWithText("Komande seriar").assertExists()
    }

    @Test fun focusingACardFillsTheHero() {
        show(state())
        // Before Heat has focus the hero shows the first popular title, which Seerr knows nothing of.
        compose.onNode(hasText("Dune: Part Two") and inHero).assertIsDisplayed()
        compose.onAllNodes(hasText("I biblioteket", substring = true) and inHero).assertCountEquals(0)
        card("Heat").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNode(hasText("Heat") and inHero).assertIsDisplayed()
        compose.onNode(hasText("I biblioteket", substring = true) and inHero).assertIsDisplayed()
        compose.onAllNodes(hasText("Dune: Part Two") and inHero).assertCountEquals(0)
    }

    @Test fun failedStateOffersRetry() {
        var retried = 0
        var current by mutableStateOf(state(rows = emptyList(), failed = true))
        compose.setContent { StovaTheme(reduceMotion = true) { DiscoverContent(current, {}, {}, {}, { retried++ }) } }
        compose.onNodeWithText("Seerr svara ikkje.").assertIsDisplayed()
        // OK tries again at once: the button has focus.
        compose.onNodeWithText("Prøv att").assertIsFocused().performClick()
        assertEquals(1, retried)
        // While Spole is asked again the page says so; when Seerr fails once more the button is back, with focus.
        compose.runOnIdle { current = current.copy(loading = true) }
        compose.onNodeWithText("Hentar frå Seerr …").assertIsDisplayed()
        compose.onAllNodesWithText("Prøv att").assertCountEquals(0)
        compose.runOnIdle { current = current.copy(loading = false) }
        compose.onNodeWithText("Seerr svara ikkje.").assertIsDisplayed()
        compose.onNodeWithText("Prøv att").assertIsFocused()
    }

    @Test fun browseAccessShowsTheSignInHint() {
        show(state(access = SeerrAccess.BROWSE))
        compose.onNodeWithText("Logg inn i Spole for å be om titlar.").assertIsDisplayed()
        card("Heat").assertIsDisplayed()
    }

    @Test fun searchButtonOpensSearch() {
        var searched = 0
        show(state(), onOpenSearch = { searched++ })
        // Up from the rows reaches the button past the hero, which takes no focus.
        card("Dune: Part Two").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithText("Søk").assertIsFocused().performClick()
        assertEquals(1, searched)
    }
}
