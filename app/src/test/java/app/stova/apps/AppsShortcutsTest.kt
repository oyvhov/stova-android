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
import app.stova.media.model.MediaAccount
import app.stova.media.model.ServerKind
import app.stova.profiles.Profile
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    private fun show(kid: Boolean = false): Pair<ProfileRepository, Profile> {
        val profiles = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), Dispatchers.Unconfined))
        val created = runBlocking {
            profiles.create("A", AvatarColor.LIME, false).let { adult -> if (kid) profiles.create("B", AvatarColor.LIME, true) else adult }
        }
        val account = MediaAccount("acc", created.id, ServerKind.JELLYFIN, "s", "Heim", "https://demo.invalid", "u1", "U")
        val library = FavoriteTile.Library(ShortcutKey.Library("acc", "lib1").key, "Filmar", null, account, "lib1")
        val scene = FavoriteTile.Scene(ShortcutKey.Ha("scene.kino").key, "Kino", "scene.kino")
        val shortcuts = ShortcutCatalog({ flowOf(listOf(account)) }, flowOf(true), { listOf(library) }, { listOf(scene) })
        val catalog = FakeAppCatalog(listOf(app))
        val repo = AppRepository(catalog, profiles, MainScope())
        compose.setContent {
            val profile by remember { profiles.file.map { file -> file.profiles.first { it.id == created.id } } }.collectAsState(created)
            StovaTheme(appearance = profile.appearance) { AppsScreen(repo, profile, AppArtCache(context), catalog, shortcuts) }
        }
        return profiles to created
    }

    // A library is added from its group on the Apps page, as an app is, and joins the favourites.
    @Test fun aLibraryIsAddedToTheFavouritesFromTheAppsPage() {
        val (profiles, created) = show()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bibliotek").fetchSemanticsNodes().isNotEmpty() }
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

    @Test fun kidsSeeOnlyApps() {
        show(kid = true)
        compose.waitForIdle()
        compose.onNodeWithText("Bibliotek").assertDoesNotExist()
        compose.onNodeWithText("Scenar og skript").assertDoesNotExist()
    }
}
