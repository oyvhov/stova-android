package app.stova.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The sheets that ask Seerr for a title and take a request back: what they offer, what they send, and what they will not. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class RequestSheetTest {
    @get:Rule val compose = createComposeRule()

    private val heat = DiscoverTitle(DiscoverType.MOVIE, 949, "Heat", 1995, null, null, null, SeerrStatus.UNKNOWN, null, emptyList())
    private val thrones = DiscoverTitle(DiscoverType.SERIES, 1399, "Game of Thrones", 2011, null, null, null, SeerrStatus.PARTLY, null, emptyList())

    private fun details(title: DiscoverTitle, seasons: List<DiscoverSeason> = emptyList()) =
        DiscoverDetails(title, null, null, null, canRequest = true, requestId = null, requestStage = null, trailer = null, seasons = seasons)

    private fun season(number: Int, status: Int, name: String = "Sesong $number") = DiscoverSeason(number, name, 10, seerrStatus(status), null)

    // Season 1 is in the library (5), 2 and 3 and the specials Seerr knows nothing of (1); Seerr sends the specials first.
    private val seasons = listOf(season(0, 1, "Spesialar"), season(1, 5), season(2, 1), season(3, 1))

    private val sent = mutableListOf<List<Int>>()
    private var dismissed = 0
    private var open by mutableStateOf(true)

    private fun show(details: DiscoverDetails) = compose.setContent {
        StovaTheme(reduceMotion = true) {
            if (open) RequestSheet(details, onSend = { sent += it }, onDismiss = { open = false; dismissed++ })
        }
    }

    private fun row(name: String) = compose.onNode(hasText(name) and hasClickAction())

    @Test fun movieSheetSendsWithoutSeasons() {
        show(details(heat))
        compose.onNodeWithText("Heat").assertExists()
        compose.onNodeWithText("1995").assertExists()
        compose.onNodeWithText("Legg filmen til i biblioteket ditt.").assertExists()
        compose.onAllNodesWithText("Sesongar").assertCountEquals(0)
        compose.onNodeWithText("Send førespurnad").assertIsFocused().performClick()
        assertEquals(listOf(emptyList<Int>()), sent)
    }

    @Test fun seriesSheetPreselectsRequestableSeasons() {
        show(details(thrones, seasons))
        compose.onNodeWithText("Send førespurnad · 2 sesongar").assertIsFocused()
        row("Sesong 2").assertIsSelected()
        row("Sesong 3").assertIsSelected()
        // The specials are never chosen from the start, and a season in the library says so instead of being chosen.
        row("Spesialar").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        compose.onNode(hasText("Sesong 1") and hasText("I biblioteket")).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        // The seasons in order, with the specials last though Seerr sent them first.
        val tops = listOf("Sesong 1", "Sesong 2", "Sesong 3", "Spesialar").map { row(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Send førespurnad · 2 sesongar").performClick()
        assertEquals(listOf(listOf(2, 3)), sent)
    }

    @Test fun sendIsDisabledWithNoSeasonChosen() {
        show(details(thrones, seasons))
        row("Sesong 2").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Send førespurnad · 1 sesong").assertIsEnabled()
        row("Sesong 3").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Send førespurnad · 0 sesongar").assertIsNotEnabled()
        assertTrue(sent.isEmpty())
    }

    @Test fun unavailableSeasonsCannotBeToggled() {
        show(details(thrones, seasons))
        row("Sesong 1").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Send førespurnad · 2 sesongar").assertExists()
        row("Sesong 1").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        // The specials can be chosen by hand.
        row("Spesialar").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Send førespurnad · 3 sesongar").performClick()
        assertEquals(listOf(listOf(0, 2, 3)), sent)
    }

    // Back and «Avbryt» close the sheet and send nothing.
    @Test fun backClosesTheSheetWithoutSending() {
        show(details(heat))
        compose.onNodeWithText("Send førespurnad").assertIsFocused().performKeyInput { pressKey(Key.Back) }
        compose.onAllNodesWithText("Send førespurnad").assertCountEquals(0)
        assertEquals(1, dismissed)
        compose.runOnIdle { open = true }
        compose.onNodeWithText("Avbryt").performClick()
        compose.onAllNodesWithText("Send førespurnad").assertCountEquals(0)
        assertEquals(2, dismissed)
        assertTrue(sent.isEmpty())
    }

    @Test fun withdrawSheetConfirmsOrKeeps() {
        var confirmed = 0
        var kept = 0
        compose.setContent {
            StovaTheme(reduceMotion = true) { WithdrawSheet("Heat", onConfirm = { confirmed++ }, onDismiss = { kept++ }) }
        }
        compose.onNodeWithText("Trekk tilbake «Heat»?").assertExists()
        compose.onNodeWithText("Førespurnaden blir fjerna frå Seerr. Det som alt er lasta ned, blir verande.").assertExists()
        compose.onNodeWithText("Behald").performClick()
        assertEquals(0 to 1, confirmed to kept)
        compose.onNodeWithText("Trekk tilbake").performClick()
        assertEquals(1 to 1, confirmed to kept)
    }
}
