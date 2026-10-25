package app.stova.apps

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FakeAppCatalog(initial: List<InstalledApp>) : AppCatalog {
    val apps = MutableStateFlow(initial)
    val launched = mutableListOf<InstalledApp>()
    var scans = 0
    override suspend fun installed(): List<InstalledApp> { scans++; return apps.value }
    override fun changes(): Flow<Unit> = apps.drop(1).map { }
    override fun launch(app: InstalledApp): Boolean { launched += app; return true }
    override fun openAppInfo(packageName: String) = Unit
    override fun isInstalled(packageName: String): Boolean = apps.value.any { it.packageName == packageName }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun app(p: String) = InstalledApp("$p/.Main", p, ".Main", p.uppercase(), true)

    @Test fun uninstalledFavoriteDisappearsAndIsPruned() = runTest(UnconfinedTestDispatcher()) {
        val a = app("a"); val b = app("b")
        val profiles = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val id = profiles.create("A", AvatarColor.LIME, false).id
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(a.key, b.key))) }
        val catalog = FakeAppCatalog(listOf(a, b))
        val repo = AppRepository(catalog, profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val arranged = repo.arranged(profiles.file.map { file -> file.profiles.first() })
            .stateIn(backgroundScope, SharingStarted.Eagerly, null)
        assertEquals(listOf(a, b), arranged.value?.favorites)

        catalog.apps.value = listOf(a)

        assertEquals(listOf(a), arranged.value?.favorites)
        assertEquals(listOf(a.key), profiles.file.first().profiles.first().apps.favorites)
    }

    @Test fun emptyScanNeverPrunes() = runTest(UnconfinedTestDispatcher()) {
        val a = app("a")
        val profiles = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val id = profiles.create("A", AvatarColor.LIME, false).id
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(a.key))) }
        val catalog = FakeAppCatalog(emptyList())
        val repo = AppRepository(catalog, profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.arranged(profiles.file.map { file -> file.profiles.first() }).stateIn(backgroundScope, SharingStarted.Eagerly, null)
        assertEquals(listOf(a.key), profiles.file.first().profiles.first().apps.favorites)
    }

    @Test fun toggleAndMoveFavorites() = runTest(UnconfinedTestDispatcher()) {
        val profiles = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val id = profiles.create("A", AvatarColor.LIME, false).id
        val repo = AppRepository(FakeAppCatalog(emptyList()), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.toggleFavorite(id, "x/.M"); repo.toggleFavorite(id, "y/.M"); repo.moveFavorite(id, "y/.M", -1)
        assertEquals(listOf("y/.M", "x/.M"), profiles.file.first().profiles.first().apps.favorites)
        repo.toggleFavorite(id, "x/.M")
        assertEquals(listOf("y/.M"), profiles.file.first().profiles.first().apps.favorites)
    }

    // Lean core 4: the Home row, each Apps page and the icon-pack watcher each ran their own scan and callback.
    @Test fun homeAndTheAppsPageShareOneScan() = runTest(UnconfinedTestDispatcher()) {
        val a = app("a")
        val profiles = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        profiles.create("A", AvatarColor.LIME, false)
        val catalog = FakeAppCatalog(listOf(a))
        val repo = AppRepository(catalog, profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val profile = profiles.file.map { file -> file.profiles.first() }
        val home = repo.arranged(profile).stateIn(backgroundScope, SharingStarted.Eagerly, null)
        val page = repo.arranged(profile).stateIn(backgroundScope, SharingStarted.Eagerly, null)
        assertEquals(listOf(a), home.value?.all)
        assertEquals(listOf(a), page.value?.all)
        assertEquals(1, catalog.scans)
    }

    // Lean core 6: every appearance step re-sorted the apps with a fresh Collator on Main.
    @Test fun appearanceEditsDoNotRearrangeTheApps() = runTest(UnconfinedTestDispatcher()) {
        val a = app("a")
        val profiles = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val id = profiles.create("A", AvatarColor.LIME, false).id
        val repo = AppRepository(FakeAppCatalog(listOf(a)), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        var emissions = 0
        backgroundScope.launch { repo.arranged(profiles.file.map { file -> file.profiles.first() }).collect { emissions++ } }
        assertEquals(1, emissions)
        profiles.update(id) { it.copy(appearance = it.appearance.copy(showDate = true)) }
        profiles.update(id) { it.copy(appearance = it.appearance.copy(showTitles = true)) }
        profiles.update(id) { it.copy(appearance = it.appearance.copy(showDate = false)) }
        profiles.update(id) { it.copy(appearance = it.appearance.copy(showTitles = false)) }
        profiles.update(id) { it.copy(appearance = it.appearance.copy(clock = app.stova.profiles.ClockMode.H12)) }
        assertEquals("five appearance edits", 1, emissions)
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(a.key))) }
        assertEquals("one app edit", 2, emissions)
    }
}
