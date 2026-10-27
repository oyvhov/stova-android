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

    // A library or a scene sits among the apps in the order chosen, and only while it can work.
    @Test fun librariesAndScenesJoinTheAppsInTheChosenOrder() = runTest(UnconfinedTestDispatcher()) {
        val profiles = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val id = profiles.create("A", AvatarColor.LIME, false).id
        val library = app.stova.apps.ShortcutKey.Library("acc", "lib1").key
        val scene = app.stova.apps.ShortcutKey.Ha("scene.kino").key
        profiles.update(id) { it.copy(apps = it.apps.copy(favorites = listOf(library, nrk.key, scene),
            shortcuts = mapOf(library to app.stova.apps.Shortcut("Filmar", "http://jf/i"), scene to app.stova.apps.Shortcut("Kino")))) }
        val session = ProfileSession(profiles, backgroundScope).also { it.enter(id) }
        val repo = AppRepository(FakeAppCatalog(listOf(phone, nrk, emby)), profiles, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        val account = app.stova.media.model.MediaAccount("acc", id, app.stova.media.model.ServerKind.JELLYFIN, "s", "Heim", "http://jf", "u1", "U")
        val ha = kotlinx.coroutines.flow.MutableStateFlow(true)
        val accounts = kotlinx.coroutines.flow.MutableStateFlow(listOf(account))
        val source = AppsRowSource(repo, session, accounts = { accounts }, haReady = ha)

        var row = source.rows(session.active.value!!).first().single() as HomeRowUi.Apps
        assertEquals(listOf(library, nrk.key, scene), row.tiles.map { it.key })
        val tile = row.tiles.first() as app.stova.apps.FavoriteTile.Library
        assertEquals(listOf("Filmar", "http://jf/i", "lib1", account), listOf(tile.label, tile.image, tile.libraryId, tile.account))
        assertEquals("scene.kino", (row.tiles.last() as app.stova.apps.FavoriteTile.Scene).entityId)

        // The account removed and Home Assistant gone: only the app is left.
        accounts.value = emptyList(); ha.value = false
        row = source.rows(session.active.value!!).first().single() as HomeRowUi.Apps
        assertEquals(listOf(nrk.key), row.tiles.map { it.key })
    }

    @Test fun kidsGetOnlyApps() {
        val library = app.stova.apps.ShortcutKey.Library("acc", "lib1").key
        val prefs = app.stova.profiles.AppPrefs(favorites = listOf(library, nrk.key), shortcuts = mapOf(library to app.stova.apps.Shortcut("Barn")))
        val account = app.stova.media.model.MediaAccount("acc", "p", app.stova.media.model.ServerKind.JELLYFIN, "s", "Heim", "http://jf", "u1", "U")
        assertEquals(listOf(nrk.key), app.stova.apps.favoriteTiles(prefs, listOf(nrk), listOf(account), haReady = true, kid = true).map { it.key })
        assertEquals(listOf(library, nrk.key), app.stova.apps.favoriteTiles(prefs, listOf(nrk), listOf(account), haReady = true, kid = false).map { it.key })
    }
}
