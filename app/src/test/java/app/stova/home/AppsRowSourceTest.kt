package app.stova.home

import app.stova.apps.AppRepository
import app.stova.apps.FakeAppCatalog
import app.stova.apps.InstalledApp
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfileSession
import app.stova.profiles.ProfilesFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppsRowSourceTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun app(pkg: String, label: String, tv: Boolean) = InstalledApp("$pkg/.Main", pkg, ".Main", label, tv)
    private val nrk = app("no.nrk.tv", "NRK TV", true)
    private val emby = app("tv.emby.embyatv", "Emby", true)
    private val phone = app("com.example.phone", "Aaa phone app", false)

    // On the Shield the front page had no apps at all: a new profile has no favourites.
    @Test fun withoutFavouritesHomeShowsTheTvAppsFirst() = runTest(UnconfinedTestDispatcher()) {
        val profiles = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val session = ProfileSession(profiles, backgroundScope).also { it.enter(profiles.create("A", AvatarColor.LIME, false).id) }
        val repo = AppRepository(FakeAppCatalog(listOf(phone, nrk, emby)), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val row = AppsRowSource(repo, session).rows(session.active.value!!).first().single() as HomeRowUi.Apps
        assertEquals(false, row.favorites)
        assertEquals(listOf(emby, nrk, phone), row.apps)
    }

    @Test fun favouritesReplaceTheFullList() = runTest(UnconfinedTestDispatcher()) {
        val profiles = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val id = profiles.create("A", AvatarColor.LIME, false).id
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(nrk.key))) }
        val session = ProfileSession(profiles, backgroundScope).also { it.enter(id) }
        val repo = AppRepository(FakeAppCatalog(listOf(phone, nrk, emby)), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val row = AppsRowSource(repo, session).rows(session.active.value!!).first().single() as HomeRowUi.Apps
        assertEquals(true, row.favorites)
        assertEquals(listOf(nrk), row.apps)
    }
}
