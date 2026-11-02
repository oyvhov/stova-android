package app.stova.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.background.BackdropState
import app.stova.core.theme.StovaTheme
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.BackgroundSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A title's page on Oppdag: what its card showed at once, then Seerr's facts, the actions the person may take, and related titles. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class DiscoverTitleContentTest {
    @get:Rule val compose = createComposeRule()

    private val heat = DiscoverTitle(DiscoverType.MOVIE, 949, "Heat", 1995, "Ein gjeng ranarar og politimannen som jagar dei.", null, null,
        SeerrStatus.UNKNOWN, 8.3, listOf("Action", "Krim"))
    private val ronin = DiscoverTitle(DiscoverType.MOVIE, 8195, "Ronin", 1998, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    private val thrones = DiscoverTitle(DiscoverType.SERIES, 1399, "Game of Thrones", 2011, null, null, null, SeerrStatus.PARTLY, 8.5, emptyList())

    private fun details(title: DiscoverTitle = heat, canRequest: Boolean = true, requestId: Int? = null, stage: RequestStage? = null,
        seasons: List<DiscoverSeason> = emptyList()) =
        DiscoverDetails(title, "Ein Los Angeles-kriminalsaga.", 170, 88, canRequest, requestId, stage, null, seasons)

    private fun season(number: Int, status: SeerrStatus) = DiscoverSeason(number, "Sesong $number", 10, status, null)

    private fun show(title: DiscoverTitle = heat, details: DiscoverDetails? = null, related: List<DiscoverRow> = emptyList(),
        access: SeerrAccess = SeerrAccess.PERSONAL, onRequest: () -> Unit = {}, onWithdraw: () -> Unit = {}, onOpenSpole: () -> Unit = {},
        onOpenTitle: (DiscoverTitle) -> Unit = {}) = compose.setContent {
        StovaTheme(reduceMotion = true) {
            DiscoverTitleContent(title, details, related, access, busy = false, onRequest, onWithdraw, onOpenSpole, onOpenTitle)
        }
    }

    private fun card(name: String) = compose.onNode(hasContentDescription(name) and hasClickAction())

    @Test fun opensWithTheCardWhileDetailsLoad() {
        var opened = 0
        show(details = null, onOpenSpole = { opened++ })
        compose.onNodeWithText("Heat").assertIsDisplayed()
        // Until Seerr's page is here only Spole can be asked, and OK does that at once.
        compose.onNodeWithText("Opne i Spole").assertIsFocused().performClick()
        assertEquals(1, opened)
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.onAllNodesWithText("Trekk tilbake").assertCountEquals(0)
    }

    @Test fun movieNotInLibraryCanBeRequested() {
        var requested = 0
        show(details = details(), onRequest = { requested++ })
        // The facts line has the year, the running time, the genres and the critics, with nothing left out between them.
        compose.onNode(hasText("1995 · 170 min · Action · Krim", substring = true) and hasText("88 % kritikarar", substring = true))
            .assertIsDisplayed()
        compose.onNodeWithText("Ein Los Angeles-kriminalsaga.").assertIsDisplayed()
        compose.onNodeWithText("Be om").assertIsFocused().performClick()
        assertEquals(1, requested)
        compose.onAllNodesWithText("Trekk tilbake").assertCountEquals(0)
        compose.onNodeWithText("Opne i Spole").assertExists()
    }

    @Test fun availableTitleHasNoRequestButton() {
        val available = heat.copy(status = SeerrStatus.AVAILABLE)
        show(title = available, details = details(available))
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.onNodeWithText("I biblioteket").assertIsDisplayed()
        compose.onNodeWithText("Opne i Spole").assertIsFocused()
    }

    @Test fun seriesWithoutRequestableSeasonsHasNoRequestButton() {
        var current by mutableStateOf(details(thrones, seasons = listOf(season(1, SeerrStatus.AVAILABLE), season(2, SeerrStatus.UNKNOWN))))
        compose.setContent {
            StovaTheme(reduceMotion = true) { DiscoverTitleContent(thrones, current, emptyList(), SeerrAccess.PERSONAL, false, {}, {}, {}, {}) }
        }
        // A series asks for seasons: with one still to get the button chooses them.
        compose.onNodeWithText("Vel sesongar").assertIsDisplayed()
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.runOnIdle { current = current.copy(seasons = listOf(season(1, SeerrStatus.AVAILABLE), season(2, SeerrStatus.AVAILABLE))) }
        compose.onAllNodesWithText("Vel sesongar").assertCountEquals(0)
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.onNodeWithText("Opne i Spole").assertExists()
    }

    @Test fun browseAccessHidesRequestActions() {
        var access by mutableStateOf(SeerrAccess.PERSONAL)
        var current by mutableStateOf(details())
        compose.setContent {
            StovaTheme(reduceMotion = true) { DiscoverTitleContent(current.title, current, emptyList(), access, false, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Be om").assertIsDisplayed()
        // A person who may only look sees the title and can open it in Spole, but asks for nothing: no film, and no season.
        compose.runOnIdle { access = SeerrAccess.BROWSE }
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.runOnIdle { current = details(thrones, seasons = listOf(season(1, SeerrStatus.UNKNOWN))) }
        compose.onNodeWithText("Game of Thrones").assertIsDisplayed()
        compose.onAllNodesWithText("Vel sesongar").assertCountEquals(0)
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.onNodeWithText("Opne i Spole").assertIsDisplayed()
    }

    @Test fun ownRequestCanBeWithdrawn() {
        var withdrawn = 0
        val coming = heat.copy(status = SeerrStatus.PROCESSING)
        show(title = coming, details = details(coming, canRequest = false, requestId = 42, stage = RequestStage.DOWNLOADING),
            onWithdraw = { withdrawn++ })
        // The person's own request says how far it has come, rather than what Seerr says of the title.
        compose.onNodeWithText("Lastar ned").assertIsDisplayed()
        compose.onAllNodesWithText("På veg").assertCountEquals(0)
        compose.onAllNodesWithText("Be om").assertCountEquals(0)
        compose.onNodeWithText("Trekk tilbake").assertIsFocused().performClick()
        assertEquals(1, withdrawn)
    }

    @Test fun relatedRowsOpenAnotherTitle() {
        val opened = mutableListOf<DiscoverTitle>()
        show(details = details(), related = listOf(DiscoverRow(DiscoverRowKind.SIMILAR, listOf(ronin)),
            DiscoverRow(DiscoverRowKind.RECOMMENDED, listOf(thrones))), onOpenTitle = { opened += it })
        compose.onNodeWithText("Liknande").assertExists()
        compose.onNodeWithText("Tilrådd frå denne").assertExists()
        // Down from the actions reaches the first related row, and OK opens that title.
        compose.onNodeWithText("Be om").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("Ronin").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        card("Game of Thrones").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        card("Ronin").assertIsFocused().performClick()
        assertEquals(listOf(ronin), opened)
        // Up from the related rows goes back to the actions.
        card("Ronin").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithText("Be om").assertIsFocused()
    }

    // The profile chose the theme's own surface as background: Oppdag draws no title's picture behind it either.
    @Test fun themeBackgroundDrawsNoBackdrop() {
        val picture = "https://demo.invalid/heat-backdrop"
        val shown = BackdropState().apply { this.shown = picture }
        var source by mutableStateOf(BackgroundSource.SPOTLIGHT)
        compose.setContent {
            StovaTheme(reduceMotion = true, appearance = AppearancePrefs(backgroundSource = source)) {
                DiscoverTitleContent(heat.copy(backdrop = picture), null, emptyList(), SeerrAccess.PERSONAL, false, {}, {}, {}, {}, backdrop = shown)
            }
        }
        compose.onNodeWithTag("backdrop-target:$picture").assertExists()
        compose.runOnIdle { source = BackgroundSource.THEME }
        compose.onNodeWithTag("backdrop-target:$picture").assertDoesNotExist()
    }
}
