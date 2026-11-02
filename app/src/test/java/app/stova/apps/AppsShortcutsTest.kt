package app.stova.apps

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.discover.DiscoverRepository
import app.stova.discover.SeerrAccess
import app.stova.discover.SeerrSpole
import app.stova.discover.SpoleDiscover
import app.stova.discover.SpoleDiscoverFetch
import app.stova.media.model.MediaAccount
import app.stova.media.model.ServerKind
import app.stova.profiles.Profile
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AppsShortcutsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    private val app = InstalledApp("demo/.Main", "demo", ".Main", "Demo", true)
    // Discover as the launcher has it: the repository asks Spole, and the page gets what the person may do.
    private val discover = DiscoverRepository(SpoleDiscoverFetch(SpoleDiscover(context)))
    private val link = SpoleLink(ServerKind.EMBY, "eilev")
    private var spole: SeerrSpole? = null

    /** With [seerr], a Spole that says so has been asked as the profile's person before the page opens. */
    private fun show(kid: Boolean = false, seerr: String? = null, onOpenDiscover: () -> Unit = {}): Pair<ProfileRepository, Profile> {
        val profiles = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), Dispatchers.Unconfined))
        val created = runBlocking {
            profiles.create("A", AvatarColor.LIME, false).let { adult -> if (kid) profiles.create("B", AvatarColor.LIME, true) else adult }
        }
        if (seerr != null) {
            spole = SeerrSpole.install(context, seerr)
            runBlocking { discover.refresh(created.id, link) }
        }
        val account = MediaAccount("acc", created.id, ServerKind.JELLYFIN, "s", "Heim", "https://demo.invalid", "u1", "U")
        val library = FavoriteTile.Library(ShortcutKey.Library("acc", "lib1").key, "Filmar", null, account, "lib1")
        val scene = FavoriteTile.Scene(ShortcutKey.Ha("scene.kino").key, "Kino", "scene.kino")
        val eilev = FavoriteTile.SpoleProfile(ShortcutKey.SpoleProfile(ServerKind.EMBY, "eilev").key, "Eilev", "http://emby.invalid/eilev.png", ServerKind.EMBY, "eilev")
        val shortcuts = ShortcutCatalog({ flowOf(listOf(account)) }, flowOf(true), { listOf(library) }, { listOf(scene) }, { listOf(eilev) })
        val catalog = FakeAppCatalog(listOf(app))
        val repo = AppRepository(catalog, profiles, MainScope())
        compose.setContent {
            val profile by remember { profiles.file.map { file -> file.profiles.first { it.id == created.id } } }.collectAsState(created)
            val access = remember { discover.state(created.id) }.collectAsState().value?.access ?: SeerrAccess.NONE
            StovaTheme(appearance = profile.appearance) {
                AppsScreen(repo, profile, AppArtCache(context), catalog, shortcuts, discoverAccess = access, onOpenDiscover = onOpenDiscover)
            }
        }
        return profiles to created
    }

    // A library is added from its group on the Apps page, as an app is, and joins the favourites.
    @Test fun aLibraryIsAddedToTheFavouritesFromTheAppsPage() {
        val (profiles, created) = show()
        // Libraries and scenes come after the apps, so the page is scrolled to them once they load.
        compose.waitUntil(5_000) { runCatching { compose.onNode(hasScrollAction()).performScrollToNode(hasText("Bibliotek")) }.isSuccess }
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Kino"))
        compose.onNodeWithText("Scenar og skript").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Filmar"))
        compose.onNodeWithContentDescription("Filmar").performTouchInput { longClick() }
        compose.onNodeWithText("Legg til i favorittar").performClick()
        compose.waitForIdle()
        val key = ShortcutKey.Library("acc", "lib1").key
        compose.waitUntil(5_000) { runBlocking { profiles.file.first() }.profiles.first { it.id == created.id }.apps.favorites.contains(key) }
        assertEquals(Shortcut("Filmar"), runBlocking { profiles.file.first() }.profiles.first { it.id == created.id }.apps.shortcuts[key])
        // The page had no favourites; now the library is one, at the top.
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Favorittar").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("Filmar").onFirst().assertIsDisplayed()
    }

    // Spole's profiles are offered too: a tile per person, with their picture, opening Spole as them.
    @Test fun aSpoleProfileIsAddedToTheFavouritesWithItsPicture() {
        val (profiles, created) = show()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Spole-profilar").fetchSemanticsNodes().isNotEmpty() }
        // The people come before the apps.
        val top = { text: String -> compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Alle appar"))
        assertTrue(top("Spole-profilar") < top("Alle appar"))
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Eilev"))
        compose.onNodeWithText("Spole-profilar").assertExists()
        compose.onNode(hasTestTag("spole-avatar") and hasAnyAncestor(hasContentDescription("Eilev")), useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Eilev").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Legg til i favorittar").performClick()
        compose.waitForIdle()
        val key = ShortcutKey.SpoleProfile(ServerKind.EMBY, "eilev").key
        compose.waitUntil(5_000) { runBlocking { profiles.file.first() }.profiles.first { it.id == created.id }.apps.favorites.contains(key) }
        assertEquals(Shortcut("Eilev", image = "http://emby.invalid/eilev.png"),
            runBlocking { profiles.file.first() }.profiles.first { it.id == created.id }.apps.shortcuts[key])
    }

    @Test fun kidsSeeOnlyApps() {
        show(kid = true)
        compose.waitForIdle()
        compose.onNodeWithText("Bibliotek").assertDoesNotExist()
        compose.onNodeWithText("Scenar og skript").assertDoesNotExist()
        compose.onNodeWithText("Spole-profilar").assertDoesNotExist()
    }

    // With Seerr there for the person, the page has its own way to Discover, so it stays reachable with Home's row hidden:
    // a section after Spole's people with one tile as wide as an app's banner, reached with the D-pad and opened with OK.
    @Test fun discoverTileShowsWithAccess() {
        var opened = 0
        show(seerr = "browse", onOpenDiscover = { opened++ })
        val tile = hasContentDescription("Oppdag") and hasClickAction()
        val eilev = hasContentDescription("Eilev") and hasClickAction()
        val demo = hasContentDescription("Demo") and hasClickAction()
        compose.waitUntil(5_000) { compose.onAllNodes(eilev).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollAction()).performScrollToNode(tile)
        val heading = hasText("Oppdag") and !hasClickAction()
        compose.onNode(heading).assertExists()
        assertTrue(compose.onNode(heading).fetchSemanticsNode().boundsInRoot.bottom <= compose.onNode(tile).fetchSemanticsNode().boundsInRoot.top)
        val width = compose.onNode(tile).fetchSemanticsNode().size.width
        compose.onNode(hasScrollAction()).performScrollToNode(demo)
        assertEquals(compose.onNode(demo).fetchSemanticsNode().size.width, width)
        // Down from Spole's person lands on it, and Down again on the first of all the apps.
        compose.onNode(hasScrollAction()).performScrollToNode(eilev)
        compose.onNode(eilev).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNode(eilev).assertIsFocused().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        compose.onNode(tile).assertIsFocused().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(1, opened)
        compose.onNode(tile).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        compose.onNode(demo).assertIsFocused()
    }

    // A child has no Discover: not even a Spole saying the person may ask for titles puts the tile on a child's page.
    @Test fun kidSeesNoDiscoverTile() {
        val (_, kid) = show(kid = true, seerr = "personal")
        assertEquals(SeerrAccess.PERSONAL, discover.state(kid.id).value?.access)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Alle appar").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Oppdag").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Oppdag").assertCountEquals(0)
    }

    // Seerr not there for the person: no section and no tile. Once Spole says it is, the way to Discover comes.
    @Test fun noAccessNoTile() {
        val (_, created) = show(seerr = "none")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Spole-profilar").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Oppdag").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Oppdag").assertCountEquals(0)
        spole!!.seerr = "browse"
        runBlocking { discover.refresh(created.id, link, force = true) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Oppdag") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
    }
}
