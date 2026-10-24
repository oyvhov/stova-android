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

    private fun show(profiles: ProfileRepository, initial: Profile, catalog: AppCatalog) {
        val repo = AppRepository(catalog, profiles, MainScope())
        compose.setContent {
            val profile by remember { profiles.file.map { file -> file.profiles.first { it.id == initial.id } } }.collectAsState(initial)
            StovaTheme { AppsScreen(repo, profile, AppArtCache(context), catalog) }
        }
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
