package app.stova.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.FavoriteTile
import app.stova.apps.InstalledApp
import app.stova.apps.PackageManagerAppCatalog
import app.stova.apps.ShortcutActionsMenu
import app.stova.core.theme.StovaTheme
import app.stova.media.model.MediaAccount
import app.stova.media.model.ServerKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ShortcutTileTest {
    @get:Rule val compose = createComposeRule()
    private val app = InstalledApp("demo/.Main", "demo", ".Main", "Demo", true)
    private val account = MediaAccount("acc", "p", ServerKind.JELLYFIN, "s", "Heim", "https://demo.invalid", "u1", "U")
    private val library = FavoriteTile.Library("library:acc:lib1", "Filmar", null, account, "lib1")
    private val scene = FavoriteTile.Scene("ha:scene.kino", "Kino", "scene.kino", "mdi:movie-open")

    private fun render(onClick: (FavoriteTile) -> Unit = {}, menu: MutableList<FavoriteTile> = mutableListOf()) {
        val row = HomeRowUi.Apps(HomeRowKey.APPS, listOf(app), tiles = listOf(FavoriteTile.App(app), library, scene))
        compose.setContent {
            val context = LocalContext.current
            StovaTheme(reduceMotion = true) {
                HomeContent(listOf(row), null, HomeRowKey.APPS, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                    {}, {}, {}, {}, canEditRows = true, onShortcutClick = onClick,
                    shortcutActions = { tile, _, dismiss -> menu += tile; ShortcutActionsMenu(tile, {}, {}, {}, dismiss) })
            }
        }
    }

    // Libraries and scenes sit in the favourites row as tiles of their own, named like the apps.
    @Test fun librariesAndScenesAreTilesInTheRow() {
        val pressed = mutableListOf<FavoriteTile>()
        render(onClick = { pressed += it })
        compose.onNodeWithContentDescription("Demo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filmar").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Kino").assertIsDisplayed().performClick()
        assertEquals(listOf<FavoriteTile>(library, scene), pressed)
    }

    @Test fun holdingOkOnALibraryGivesItsOwnPanel() {
        val menu = mutableListOf<FavoriteTile>()
        render(menu = menu)
        compose.onNodeWithContentDescription("Filmar").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithContentDescription("Filmar").performKeyInput { pressKey(Key.Menu) }
        compose.onNodeWithText("Bibliotek · i favorittane").assertIsDisplayed()
        compose.onNodeWithText("Fjern frå favorittar").assertIsDisplayed()
        compose.onNodeWithText("Opne").assertIsDisplayed()
        assertEquals(listOf<FavoriteTile>(library), menu)
    }

    @Test fun aScenePanelRunsInsteadOfOpening() {
        compose.setContent { StovaTheme(reduceMotion = true) { ShortcutActionsMenu(scene, {}, {}, {}, {}) } }
        compose.onNodeWithText("Smarthus · i favorittane").assertIsDisplayed()
        compose.onNodeWithText("Køyr").assertIsDisplayed()
    }

    // A scene tile draws Home Assistant's own icon for it, from the bundled icon font.
    @Test fun aSceneTileShowsItsHomeAssistantIcon() {
        render()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("ha-icon", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
