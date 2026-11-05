package app.stova.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class FavoriteMoveUiTest {
    @get:Rule val compose=createComposeRule()
    private val apps=(1..5).map { InstalledApp("p$it/.Main","p$it",".Main","App $it",true) }
    private val catalog=FakeAppCatalog(apps)
    private var saved: List<String>?=null
    private fun show() {
        compose.setContent { StovaTheme(reduceMotion=true) {
            val context=LocalContext.current
            HomeContent(listOf(HomeRowUi.Apps(HomeRowKey.APPS,apps)),null,HomeRowKey.APPS,
                AppArtCache(context),catalog,{},{},{},{},canEditRows=true,
                appActions={app,rows,dismiss,move ->
                    AppActionsMenu(app,true,false,{},move,{},{},dismiss,onOpen={},rowActions=rows)
                },onFavoriteOrderChange={saved=it})
        }}
        compose.onNode(hasContentDescription("App 1") and hasClickAction()).assertIsFocused()
            .performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Opne").performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Flytt").assertIsFocused().performKeyInput {pressKey(Key.Enter)}
        compose.onNode(hasContentDescription("App 1") and hasClickAction()).assertIsFocused()
    }
    @Test fun homeMovesSeveralPlacesThenSavesWithoutReopeningTheMenu() {
        show()
        repeat(3) {
            compose.onNode(hasContentDescription("App 1") and hasClickAction()).performKeyInput {pressKey(Key.DirectionRight)}
            compose.onNode(hasContentDescription("App 1") and hasClickAction()).assertIsFocused()
        }
        assertNull(saved); assertTrue(catalog.launched.isEmpty())
        compose.onNode(hasContentDescription("App 1") and hasClickAction()).performKeyInput {pressKey(Key.Enter)}
        assertEquals(listOf(apps[1].key,apps[2].key,apps[3].key,apps[0].key,apps[4].key),saved)
        compose.onNodeWithText("← → Flytt · OK Ferdig · Tilbake Avbryt").assertDoesNotExist()
        assertTrue(catalog.launched.isEmpty())
    }
    @Test fun backCancelsSeveralMovesAndReturnsFocusToTheSameCard() {
        show()
        repeat(3) {compose.onNode(hasContentDescription("App 1") and hasClickAction()).performKeyInput {pressKey(Key.DirectionRight)}}
        compose.onNode(hasContentDescription("App 1") and hasClickAction()).performKeyInput {pressKey(Key.Back)}
        compose.onNode(hasContentDescription("App 1") and hasClickAction()).assertIsFocused()
        val first=compose.onNode(hasContentDescription("App 1") and hasClickAction()).getUnclippedBoundsInRoot()
        val second=compose.onNode(hasContentDescription("App 2") and hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue(first.left<second.left); assertNull(saved); assertTrue(catalog.launched.isEmpty())
    }
}
