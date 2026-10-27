package app.stova.apps

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutTest {
    @get:Rule val tmp = TemporaryFolder()
    private val a = InstalledApp("a/.Main", "a", ".Main", "A", true)
    private val library = ShortcutKey.Library("2b1c-acc", "f137a2").key
    private val scene = ShortcutKey.Ha("scene.kino").key

    private fun TestScope.profiles() = ProfileRepository(
        JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))

    @Test fun shortcutKeysNeverLookLikeApps() {
        assertEquals("library:2b1c-acc:f137a2", library)
        assertEquals(ShortcutKey.Library("2b1c-acc", "f137a2"), ShortcutKey.parse(library))
        assertEquals("ha:scene.kino", scene)
        assertEquals(ShortcutKey.Ha("scene.kino"), ShortcutKey.parse(scene))
        assertNull(ShortcutKey.parse(a.key))
        assertNull(ShortcutKey.parse("library:only-one-part"))
        assertNull(ShortcutKey.parse("ha:no-domain"))
        assertTrue(isShortcutKey(library) && isShortcutKey(scene))
        assertFalse(isShortcutKey(a.key))
    }

    // Tidying favourites follows renamed apps and drops uninstalled ones; a library or a scene is neither.
    @Test fun tidyingAppsLeavesShortcutsAlone() = runTest(UnconfinedTestDispatcher()) {
        val profiles = profiles()
        val id = profiles.create("A", AvatarColor.LIME, false).id
        val b = InstalledApp("b/.Main", "b", ".Main", "B", true)
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(a.key, library, b.key, scene))) }
        val catalog = FakeAppCatalog(listOf(a, b))
        val repo = AppRepository(catalog, profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val arranged = repo.arranged(profiles.file.map { it.profiles.first() }).stateIn(backgroundScope, SharingStarted.Eagerly, null)
        assertEquals(listOf(a, b), arranged.value?.favorites)

        catalog.apps.value = listOf(a)

        assertEquals(listOf(a.key, library, scene), profiles.file.first().profiles.first().apps.favorites)
        repo.pruneMissing(id, setOf(a.key))
        assertEquals(listOf(a.key, library, scene), profiles.file.first().profiles.first().apps.favorites)
    }

    @Test fun addingAShortcutKeepsItsNameAndRemovingItForgetsIt() = runTest(UnconfinedTestDispatcher()) {
        val profiles = profiles()
        val id = profiles.create("A", AvatarColor.LIME, false).id
        val repo = AppRepository(FakeAppCatalog(listOf(a)), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.toggleFavorite(id, a.key)
        repo.addShortcut(id, library, Shortcut("Filmar", "http://jf/Items/f137a2/Images/Primary?tag=t1"))
        repo.addShortcut(id, library, Shortcut("Filmar", "http://jf/Items/f137a2/Images/Primary?tag=t1"))
        var prefs = profiles.file.first().profiles.first().apps
        assertEquals(listOf(a.key, library), prefs.favorites)
        assertEquals("Filmar", prefs.shortcuts[library]?.label)

        // A fresh list from the server renames what is saved, and never adds what was not chosen.
        repo.refreshShortcuts(id, mapOf(library to Shortcut("Film", "http://jf/new"), scene to Shortcut("Kino")))
        prefs = profiles.file.first().profiles.first().apps
        assertEquals(Shortcut("Film", "http://jf/new"), prefs.shortcuts[library])
        assertFalse(scene in prefs.shortcuts)

        repo.toggleFavorite(id, library)
        prefs = profiles.file.first().profiles.first().apps
        assertEquals(listOf(a.key), prefs.favorites)
        assertTrue(prefs.shortcuts.isEmpty())
    }
}
