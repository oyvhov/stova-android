package app.stova.discover

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.background.BackdropState
import app.stova.core.theme.StovaTheme
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.BackgroundSource
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
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

    private fun state(access: SeerrAccess = SeerrAccess.PERSONAL, rows: List<DiscoverRow> = this.rows, failed: Boolean = false,
        requests: List<DiscoverRequest> = emptyList()) = DiscoverState(access, rows, requests, loading = false, failed = failed)

    private fun request(tmdbId: Int, name: String, stage: RequestStage, percent: Int? = null) =
        DiscoverRequest(DiscoverType.MOVIE, tmdbId, name, null, null, stage, percent, emptyList(), tmdbId, is4k = false)
    private val gorge = request(950396, "The Gorge", RequestStage.DOWNLOADING, 42)
    private val nosferatu = request(426063, "Nosferatu", RequestStage.REQUESTED, 10)

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

    @Test fun requestsRowAppearsOnlyWhenThereAreAny() {
        var current by mutableStateOf(state())
        val opened = mutableListOf<DiscoverRequest>()
        compose.setContent { StovaTheme(reduceMotion = true) { DiscoverContent(current, {}, {}, { opened += it }, {}) } }
        compose.onAllNodesWithText("Dine førespurnader").assertCountEquals(0)
        compose.runOnIdle { current = current.copy(requests = listOf(gorge, nosferatu)) }
        compose.onNodeWithText("Dine førespurnader").assertExists()
        // The row stands right after «Populært no»: Down from the first popular title lands on the first request, and Down again on what is coming.
        card("Dune: Part Two").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("The Gorge").assertIsFocused()
        compose.onNode(hasText("The Gorge") and inHero).assertIsDisplayed()
        card("The Gorge").performKeyInput { pressKey(Key.DirectionDown) }
        card("Mickey 17").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        // OK on a request opens its title.
        card("The Gorge").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(gorge), opened)
        compose.runOnIdle { current = current.copy(requests = emptyList()) }
        compose.onAllNodesWithText("Dine førespurnader").assertCountEquals(0)
    }

    @Test fun downloadingCardShowsPercent() {
        show(state(requests = listOf(gorge, nosferatu)))
        // The row stands under the fold; walking down to it brings the card into view whole, with the line under it.
        card("Dune: Part Two").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("The Gorge").assertIsFocused()
        compose.onNodeWithText("Lastar ned · 42 %").assertIsDisplayed()
        compose.onNodeWithText("Bede om").assertIsDisplayed()
        // Only the download draws a bar, as far across its card as the download has come.
        compose.onAllNodesWithTag("card-progress", useUnmergedTree = true).assertCountEquals(1)
        val bar = compose.onNode(hasTestTag("card-progress") and hasAnyAncestor(hasContentDescription("The Gorge")), useUnmergedTree = true)
        val cardWidth = card("The Gorge").fetchSemanticsNode().size.width
        assertEquals(cardWidth * 0.42f, bar.fetchSemanticsNode().size.width.toFloat(), 1f)
    }

    // Spole gives a request no backdrop: its own when it has one, else the same title's from the rows, else the picture already shown.
    @Test fun backdropForARequestComesFromItselfTheRowsOrWhatIsShown() {
        val shown = "https://demo.invalid/shown"
        val withPictures = rows.map { row -> row.copy(items = row.items.map { it.copy(backdrop = "https://demo.invalid/${it.key}") }) }
        val severance = DiscoverRequest(DiscoverType.SERIES, 95396, "Severance", null, null, RequestStage.REQUESTED, null, listOf(2), 3, is4k = false)
        assertEquals("https://demo.invalid/tv-95396", backdropFor(severance, withPictures, shown))
        // The same number as a film is another title.
        assertEquals(shown, backdropFor(severance.copy(type = DiscoverType.MOVIE), withPictures, shown))
        assertEquals(shown, backdropFor(gorge, withPictures, shown))
        assertEquals("https://demo.invalid/own", backdropFor(gorge.copy(backdrop = "https://demo.invalid/own"), withPictures, shown))
        // A matching title without a picture of its own lends none.
        assertEquals(shown, backdropFor(severance, rows, shown))
    }

    // Focus on a request never fades the background out: the picture shown stays, or the same title's from the rows comes in.
    @OptIn(DelicateCoilApi::class)
    @Test fun focusedRequestKeepsOrBorrowsTheBackdrop() {
        val picture = { title: DiscoverTitle -> "https://demo.invalid/${title.key}" }
        val withPictures = rows.map { row -> row.copy(items = row.items.map { it.copy(backdrop = picture(it)) }) }
        // Spole names Heat as it was asked for; it is the same film as the popular card.
        val heatRequest = request(heat.tmdbId, "Heat (1995)", RequestStage.REQUESTED)
        // Every picture decodes at once, so the layer shows what it is given.
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>()).components {
            add(Fetcher.Factory<Uri> { _, _, _ -> Fetcher { ImageFetchResult(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImage(), false, DataSource.MEMORY) } })
        }.build())
        try {
            show(state(rows = withPictures, requests = listOf(gorge, heatRequest)))
            val dune = picture(withPictures.first().items.first())
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("backdrop-target:$dune").fetchSemanticsNodes().isNotEmpty() }
            // The Gorge is in no row: Dune's picture stays behind it.
            card("Dune: Part Two").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            card("The Gorge").assertIsFocused()
            compose.onNode(hasText("The Gorge") and inHero).assertIsDisplayed()
            compose.onNodeWithTag("backdrop-target:$dune").assertExists()
            // Heat is popular too: its picture comes in behind the request.
            card("The Gorge").performKeyInput { pressKey(Key.DirectionRight) }
            card("Heat (1995)").assertIsFocused()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("backdrop-target:${picture(heat)}").fetchSemanticsNodes().isNotEmpty() }
        } finally {
            SingletonImageLoader.reset()
        }
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

    // The profile chose the theme's own surface as background: the rows draw no title's picture behind them either.
    @Test fun themeBackgroundDrawsNoBackdrop() {
        val picture = "https://demo.invalid/dune-backdrop"
        val shown = BackdropState().apply { this.shown = picture }
        val withPicture = rows.map { row -> row.copy(items = row.items.map { it.copy(backdrop = picture) }) }
        var source by mutableStateOf(BackgroundSource.SPOTLIGHT)
        compose.setContent {
            StovaTheme(reduceMotion = true, appearance = AppearancePrefs(backgroundSource = source)) {
                DiscoverContent(state(rows = withPicture), {}, {}, {}, {}, backdrop = shown)
            }
        }
        compose.onNodeWithTag("backdrop-target:$picture").assertExists()
        compose.runOnIdle { source = BackgroundSource.THEME }
        compose.onNodeWithTag("backdrop-target:$picture").assertDoesNotExist()
    }
}
