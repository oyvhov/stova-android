package app.stova.home

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

/** Home's Oppdag row: Seerr's titles end in a tile that opens the page, and a card in Seerr cannot be played. */
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

    @Test fun discoverRowEndsWithAnOpenTile() {
        var opened = 0
        show { opened++ }
        // Right from the last card reaches the tile, as «Alle appar» ends the apps.
        compose.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNode(hasContentDescription("Oppdag") and hasClickAction()).assertIsFocused().performClick()
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
        compose.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.Menu) }
        // The menu is open, without play and without what Spole does with a title in the library.
        compose.onNodeWithText("Detaljar i Spole").assertExists()
        compose.onAllNodesWithText("Spel").assertCountEquals(0)
        compose.onAllNodesWithText("Merk som sett").assertCountEquals(0)
    }
}
