package app.stova.home

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.background.backgroundUrl
import app.stova.background.backgroundCanMove
import app.stova.core.theme.StovaTheme
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.profiles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HeroBackgroundChoicesTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private val item = MediaItem("f", "a", ItemType.MOVIE, "Fjordlys", genres = listOf("Drama"), art = ItemArt(backdrop = "backdrop"))

    @Test fun heroOffRemovesHeroAndLetsRowsStartHigh() {
        val row = HomeRowUi.Media(HomeRowKey.continueWatching("a"), account, MediaRowKind.CONTINUE, listOf(item), false)
        compose.setContent { StovaTheme(appearance = AppearancePrefs(heroSize = HeroSize.OFF, homeRows = HomeRows.SCROLLING), reduceMotion = true) {
            val context = LocalContext.current
            HomeContent(listOf(row), null, row.key, AppArtCache(context), PackageManagerAppCatalog(context,emptySet()),
                {}, {}, {}, {}, spotlight = SpotlightEntry(item,account,false))
        } }
        compose.onNodeWithText("Film · Drama").assertDoesNotExist()
        assertTrue(compose.onNodeWithText("Hald fram").getUnclippedBoundsInRoot().top < 40.dp)
    }

    @Test fun backgroundSourceAndMotionRespectOffAndReducedMotion() {
        val p = AppearancePrefs()
        assertEquals("spot", backgroundUrl(p,"spot","fixed"))
        assertEquals("fixed", backgroundUrl(p.copy(backgroundSource = BackgroundSource.FIXED),"spot","fixed"))
        assertNull(backgroundUrl(p.copy(backgroundSource = BackgroundSource.THEME),"spot","fixed"))
        assertFalse(backgroundCanMove(p,true))
        assertTrue(backgroundCanMove(p.copy(backgroundMotion=true),true))
        assertFalse(backgroundCanMove(p.copy(backgroundMotion=true,reduceMotion=true),true))
        assertFalse(backgroundCanMove(p.copy(backgroundMotion=true,heroSize=HeroSize.OFF),true))
        assertFalse(backgroundCanMove(p.copy(backgroundMotion=true),false))
    }

    @Test fun filteredSpotlightQueriesAndSearchCoverTheLibrary() {
        val film = spotlightCandidates(ServerKind.JELLYFIN,"u",SpotlightSource.MOVIES).first()
        assertTrue(film.contains("IncludeItemTypes=Movie&"))
        assertTrue(spotlightCandidates(ServerKind.EMBY,"u",SpotlightSource.SERIES).single().contains("IncludeItemTypes=Series&"))
        assertTrue(spotlightCandidates(ServerKind.JELLYFIN,"u",SpotlightSource.FAVORITES).first().contains("Filters=IsFavorite"))
        val search = titleSearchCandidates(ServerKind.JELLYFIN,"u","Fjord & fjell").first()
        assertTrue(search.contains("SearchTerm=Fjord+%26+fjell"))
        assertTrue(search.contains("Recursive=true"))
        val parsed = ItemParser.parseList("""[{"Id":"f","Name":"Film","Type":"Movie","UserData":{"IsFavorite":true}}]""",account).single()
        assertTrue(parsed.favorite)
    }
}
