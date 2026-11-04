package app.stova.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.discover.DiscoverTitle
import app.stova.discover.DiscoverType
import app.stova.discover.SeerrStatus
import app.stova.discover.asMediaItem
import app.stova.media.MediaRowKind
import app.stova.media.model.ServerKind
import app.stova.media.seerrAccount
import app.stova.profiles.SpoleLink
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home's Oppdag row: Seerr's titles follow a tile that opens the page, and a card in Seerr cannot be played. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeContentDiscoverTest {
    @get:Rule val compose = createComposeRule()

    private val account = seerrAccount("p1", SpoleLink(ServerKind.EMBY, "eilev"))
    private val heat = DiscoverTitle(DiscoverType.MOVIE, 949, "Heat", 1995, null, null, null, SeerrStatus.UNKNOWN, 8.3, emptyList())
    private val row = HomeRowUi.Media(HomeRowKey.DISCOVER, account, MediaRowKind.DISCOVER, listOf(heat.asMediaItem(account.id)), offline = false)

    private fun show(onOpenDiscover: () -> Unit = {}) = compose.setContent {
        StovaTheme(reduceMotion = true) {
            val context = LocalContext.current
            HomeContent(listOf(row), null, HomeRowKey.DISCOVER, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), {}, {}, {}, {},
                canEditRows = true, onMediaAction = { _, _ -> }, onOpenDiscover = onOpenDiscover)
        }
    }

    @Test fun discoverRowStartsWithAnOpenTile() {
        var opened = 0
        show { opened++ }
        compose.onNode(hasContentDescription("Sjå alt") and hasClickAction()).assertIsFocused().performClick()
        assertEquals(1, opened)
    }

    // The row has no source to choose, so its header names none, though a row's style shows the source by default.
    @Test fun theOppdagHeaderNamesNoSource() {
        show()
        compose.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsDisplayed()
        compose.onAllNodesWithText("Seerr").assertCountEquals(0)
    }

    @Test fun longPressOnADiscoverCardHasNoPlay() {
        show()
        compose.onNode(hasContentDescription("Sjå alt") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.Menu) }
        // The menu is open, without play and without what Spole does with a title in the library; its
        // details are Stova's own Oppdag page, not Spole's.
        compose.onNodeWithText("Sjå i Oppdag").assertExists()
        compose.onAllNodesWithText("Detaljar i Spole").assertCountEquals(0)
        compose.onAllNodesWithText("Spel").assertCountEquals(0)
        compose.onAllNodesWithText("Merk som sett").assertCountEquals(0)
    }

    // Back from the page lands where the remote was: the tile reports itself as «Alle appar» does, and Home
    // drawn again with that target puts focus on it.
    @Test fun focusReturnsToTheOppdagTile() {
        var focused: FocusedTarget? = null
        var visit by mutableIntStateOf(0)
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                val context = LocalContext.current
                key(visit) {
                    HomeContent(listOf(row), focused, HomeRowKey.DISCOVER, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                        {}, { focused = it }, {}, {})
                }
            }
        }
        compose.onNode(hasContentDescription("Sjå alt") and hasClickAction()).assertIsFocused()
        assertEquals(FocusedTarget.App("Sjå alt"), focused)
        // Home is drawn again, as after Back from the page.
        compose.runOnIdle { visit++ }
        compose.onNode(hasContentDescription("Sjå alt") and hasClickAction()).assertIsFocused()
        compose.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsNotFocused()
    }
}
