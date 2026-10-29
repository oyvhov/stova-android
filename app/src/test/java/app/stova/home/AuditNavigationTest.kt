package app.stova.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import app.stova.media.model.*
import app.stova.media.ui.ItemActionsMenu
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class AuditNavigationTest {
    @get:Rule val compose=createComposeRule()
    private val app=InstalledApp("demo/.Main","demo",".Main","Min app",true)

    @Test fun partiallyWatchedMovieOffersResumeWithRemainingMinutes() {
        var played=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            ItemActionsMenu(MediaItem("f","a",ItemType.MOVIE,"Film",runtimeMinutes=160,progress=.1f),{played++},{},{})
        }}
        compose.onNodeWithText("Hald fram · 144 min att").assertIsFocused().performKeyInput {pressKey(Key.Enter)}
        assertEquals(1,played)
    }
    @Test fun resumeWithoutRuntimeDoesNotInventRemainingTime() {
        compose.setContent {StovaTheme(reduceMotion=true) {
            ItemActionsMenu(MediaItem("f","a",ItemType.EPISODE,"Episode",progress=.3f),{},{},{})
        }}
        compose.onNodeWithText("Hald fram").assertIsFocused()
    }
    @Test fun completedMovieOffersPlay() {
        compose.setContent {StovaTheme(reduceMotion=true) {
            ItemActionsMenu(MediaItem("f","a",ItemType.MOVIE,"Film",runtimeMinutes=160,progress=1f),{},{},{})
        }}
        compose.onNodeWithText("Spel").assertIsFocused()
    }
    @Test fun appMenuStartsOnOpenAndKeepsArtworkCompact() {
        var opened=0;var removed=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            AppActionsMenu(app,true,false,{removed++},{},{},{},{},onOpen={opened++},
                art={Box(Modifier.fillMaxSize().testTag("menu-art"))})
        }}
        compose.onNodeWithText("Opne").assertIsFocused().assertIsDisplayed()
        val art=compose.onNodeWithTag("menu-art").getUnclippedBoundsInRoot()
        assertTrue((art.bottom-art.top).value<=72f)
        compose.onNodeWithText("Opne").performKeyInput {pressKey(Key.Enter)}
        assertEquals(1,opened);assertEquals(0,removed)
    }
    @Test fun allAppsIsReachableAfterTheLastFavouriteWithTheRemote() {
        var opened=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            val context=LocalContext.current
            HomeContent(listOf(HomeRowUi.Apps(HomeRowKey.APPS,listOf(app))),null,HomeRowKey.APPS,
                AppArtCache(context),PackageManagerAppCatalog(context,emptySet()),{opened++},{},{},{})
        }}
        compose.onNode(hasContentDescription("Min app") and hasClickAction()).assertIsFocused()
            .performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNode(hasContentDescription("Alle appar") and hasClickAction()).assertIsFocused()
            .performKeyInput {pressKey(Key.Enter)}
        assertEquals(1,opened)
    }
}
