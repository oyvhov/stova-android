package app.stova.home

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.*
import app.stova.media.*
import app.stova.media.model.*
import app.stova.profiles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The apps row can drop its names while media rows keep their titles, and the other way round. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class RowCardTitlesTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "U")
    // Neither has artwork, so each shows its name inside the placeholder as well as any caption below it.
    private val film = MediaItem("f", "a", ItemType.MOVIE, "Fjordlys")
    private val kodi = InstalledApp("kodi/.Main", "kodi", ".Main", "Kodi", true)

    private fun show(global: Boolean, apps: RowStyle) {
        val rows = listOf(
            HomeRowUi.Apps(HomeRowKey.APPS, listOf(kodi)),
            HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, listOf(film), false),
        )
        compose.setContent { StovaTheme(appearance = AppearancePrefs(homeRows = HomeRows.SCROLLING, showTitles = global), reduceMotion = true) {
            val ctx = LocalContext.current
            HomeContent(rows, null, HomeRowKey.APPS, AppArtCache(ctx), PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {},
                homePrefs = HomePrefs(rowFormats = mapOf(HomeRowKey.APPS to apps)))
        } }
        compose.waitForIdle()
    }

    private fun texts(value: String) = compose.onAllNodesWithText(value, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test fun appsRowCanHideNamesWhileMediaRowsKeepTitles() {
        show(global = true, apps = RowStyle(cardTitles = false))
        assertEquals("Only the placeholder name, no caption", 1, texts("Kodi"))
        assertEquals("Placeholder and caption", 2, texts("Fjordlys"))
    }

    @Test fun appsRowCanShowNamesWhileOtherRowsHaveNone() {
        show(global = false, apps = RowStyle(cardTitles = true))
        assertEquals(2, texts("Kodi"))
        assertEquals(1, texts("Fjordlys"))
    }

    @Test fun rowChoiceStepsAwayFromAndBackToTheGlobalChoice() {
        var style by mutableStateOf(RowStyle())
        compose.setContent { StovaTheme {
            androidx.compose.foundation.layout.Column {
                RowStyleChoices(style, RowFormat.WIDE, ArtworkSize.COMPACT, globalTitles = true) { style = it }
            }
        } }
        val choice = compose.onNode(hasText("Namn under korta") and hasClickAction())
        choice.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "På"))
        choice.performClick()
        assertEquals(false, style.cardTitles)
        choice.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Av"))
        choice.performClick()
        // Back to the global value inherits it again, so "Use defaults" and later global changes still apply.
        assertEquals(RowStyle(), style)
    }
}
