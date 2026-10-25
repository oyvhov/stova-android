package app.stova.apps

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
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
class AppsScreenTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val banan = InstalledApp("banan/.Main", "banan", ".Main", "Banan", true)
    private val eple = InstalledApp("eple/.Main", "eple", ".Main", "Eple", true)

    private fun profiles() = ProfileRepository(
        JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), Dispatchers.Unconfined),
    )

    private fun show(profiles: ProfileRepository, initial: Profile, catalog: AppCatalog, art: AppArtCache = AppArtCache(context)) {
        val repo = AppRepository(catalog, profiles, MainScope())
        compose.setContent {
            val profile by remember { profiles.file.map { file -> file.profiles.first { it.id == initial.id } } }.collectAsState(initial)
            StovaTheme(appearance = profile.appearance) { AppsScreen(repo, profile, art, catalog) }
        }
    }

    @Test fun profileEditsUpdateGridWithoutRescanningPackages() {
        val profiles = profiles()
        val adult = runBlocking { profiles.create("A", AvatarColor.LIME, false) }
        val scans = java.util.concurrent.atomic.AtomicInteger()
        val catalog = object : AppCatalog by FakeAppCatalog(listOf(banan, eple)) {
            override suspend fun installed(): List<InstalledApp> {
                scans.incrementAndGet()
                return listOf(banan, eple)
            }
        }
        show(profiles, adult, catalog)
        compose.onNodeWithText("Banan").assertExists()
        assertEquals(1, scans.get())
        runBlocking { profiles.update(adult.id) { it.copy(name = "B", apps = it.apps.copy(hidden = setOf(banan.key))) } }
        compose.onNodeWithText("Skjulte appar (1)").assertExists()
        compose.onNodeWithText("Banan").assertDoesNotExist()
        assertEquals(1, scans.get())
    }

    @Test fun addToFavoritesFromMenu() {
        val profiles = profiles()
        val adult = runBlocking { profiles.create("A", AvatarColor.LIME, false) }
        show(profiles, adult, FakeAppCatalog(listOf(banan, eple)))
        compose.onNodeWithText("Banan").performTouchInput { longClick() }
        compose.onNodeWithText("Legg til i favorittar").performClick()
        compose.waitForIdle()
        assertEquals(listOf(banan.key), runBlocking { profiles.file.first().profiles.first().apps.favorites })
    }

    @Test fun menuKeyCanResetOnlyThisProfilesIcon() {
        val profiles = profiles()
        val icon = IconOverride("com.donnnno.arcticons", "vlc")
        val adult = runBlocking {
            val a = profiles.create("A", AvatarColor.LIME, false)
            profiles.update(a.id) { it.copy(apps = it.apps.copy(iconOverrides = mapOf(banan.key to icon))) }
        }
        val other = runBlocking {
            val b = profiles.create("B", AvatarColor.OCEAN, false)
            profiles.update(b.id) { it.copy(apps = it.apps.copy(iconOverrides = mapOf(banan.key to icon))) }
        }
        show(profiles, adult, FakeAppCatalog(listOf(banan)))
        val tile = compose.onNodeWithText("Banan")
        tile.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        tile.performKeyInput { pressKey(Key.Menu) }
        compose.onNodeWithText("Endre ikon").performClick()
        compose.onNodeWithText("Frå ikonpakke").assertIsDisplayed()
        compose.onNodeWithText("Tilbakestill").performClick()
        compose.waitForIdle()
        val loaded = runBlocking { profiles.current().profiles }
        assertTrue(loaded.first { it.id == adult.id }.apps.iconOverrides.isEmpty())
        assertEquals(icon, loaded.first { it.id == other.id }.apps.iconOverrides[banan.key])
    }

    @Test fun longOkMenuSelectsAndPersistsAnIcon() {
        val profiles = profiles()
        val adult = runBlocking { profiles.create("A", AvatarColor.LIME, false) }
        val source = FakeIconPackSource().apply { catalog = """<item drawable="vlc"/>""" }
        val art = AppArtCache(context, iconPacks = IconPackRepository(source, tmp.root.resolve("icons")))
        show(profiles, adult, FakeAppCatalog(listOf(banan)), art)
        compose.onNodeWithText("Banan").performTouchInput { longClick() }
        compose.onNodeWithText("Endre ikon").performClick()
        compose.onNodeWithText("Frå ikonpakke").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Søk etter ikon").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Søk etter ikon").performTextInput("vlc")
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("vlc") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("vlc") and !hasSetTextAction()).performClick()
        compose.waitUntil(5_000) { runBlocking { profiles.current().profiles.first().apps.iconOverrides[banan.key] != null } }
        assertEquals(IconOverride(source.packs.single().packageName, "vlc"), runBlocking { profiles.current().profiles.first().apps.iconOverrides[banan.key] })
        assertTrue(runBlocking { profiles.current().profiles.first().apps.favorites.isEmpty() })
    }

    @Test fun opensAtTopWithFavorites() {
        val profiles = profiles()
        val many = (1..6).map { InstalledApp("p$it/.Main", "p$it", ".Main", "App $it", true) }
        val adult = runBlocking {
            val a = profiles.create("A", AvatarColor.LIME, false)
            profiles.update(a.id) { it.copy(apps = it.apps.copy(favorites = many.take(2).map { app -> app.key })) }
        }
        // The real catalog answers from an IO thread after the first frame; so does this one.
        val gate = CompletableDeferred<Unit>()
        val catalog = object : AppCatalog by FakeAppCatalog(many) {
            override suspend fun installed(): List<InstalledApp> { gate.await(); return many }
        }
        show(profiles, adult, catalog)
        compose.waitForIdle()
        gate.complete(Unit)
        compose.onNodeWithText("Favorittar").assertIsDisplayed()
    }

    @Test fun clickLaunches() {
        val profiles = profiles()
        val adult = runBlocking { profiles.create("A", AvatarColor.LIME, false) }
        val catalog = FakeAppCatalog(listOf(banan))
        show(profiles, adult, catalog)
        compose.onNodeWithText("Banan").performClick()
        assertEquals(listOf(banan), catalog.launched)
    }

    @Test fun kidProfileHasNoMenuAndNoHiddenGroup() {
        val profiles = profiles()
        val kid = runBlocking {
            profiles.create("A", AvatarColor.LIME, false)
            val k = profiles.create("K", AvatarColor.ROSE, true)
            profiles.update(k.id) { it.copy(apps = it.apps.copy(kidAllowed = setOf(banan.key), hidden = setOf(eple.key))) }
        }
        show(profiles, kid, FakeAppCatalog(listOf(banan, eple)))
        compose.onNodeWithText("Banan").performTouchInput { longClick() }
        compose.onNodeWithText("Legg til i favorittar").assertDoesNotExist()
        compose.onNodeWithText("Skjulte appar", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Eple").assertDoesNotExist()
        assertTrue(true)
    }
}
