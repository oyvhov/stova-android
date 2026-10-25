package app.stova.home

import app.stova.apps.InstalledApp
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfileSession
import app.stova.profiles.ProfilesFile
import app.stova.profiles.SpotlightTime
import app.stova.profiles.StartFocus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private val app = InstalledApp("a/.Main", "a", ".Main", "A", true)

    @Before fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun reset() = Dispatchers.resetMain()

    private suspend fun TestScope.session(scope: CoroutineScope, startFocus: StartFocus, rowOrder: List<HomeRowKey> = emptyList()): ProfileSession {
        val repo = ProfileRepository(
            JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)),
        )
        val id = repo.create("A", AvatarColor.LIME, false).id
        repo.update(id) { it.copy(home = it.home.copy(startFocus = startFocus, rowOrder = rowOrder)) }
        return ProfileSession(repo, scope).also { it.enter(id) }
    }

    private fun source(vararg rows: HomeRowUi) = HomeRowSource { flowOf(rows.toList()) }

    // Review Important 3: "not loaded yet" is not "empty", so the empty state never flashes.
    @Test fun rowsAreNullUntilSourcesAnswer() = runTest(UnconfinedTestDispatcher()) {
        val silent = HomeRowSource { kotlinx.coroutines.flow.flow { kotlinx.coroutines.awaitCancellation() } }
        val vm = HomeViewModel(session(backgroundScope, StartFocus.FIRST_MEDIA_ROW), listOf(silent))
        assertEquals(null, vm.rows.value)
    }

    @Test fun startFocusFallsBackToAppsWithoutMediaRows() = runTest(UnconfinedTestDispatcher()) {
        val vm = HomeViewModel(session(backgroundScope, StartFocus.FIRST_MEDIA_ROW), listOf(source(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app)))))
        assertEquals(HomeRowKey.APPS, vm.initialFocus.value)
    }

    @Test fun firstMediaRowWinsWhenPresent() = runTest(UnconfinedTestDispatcher()) {
        val x = HomeRowKey("x")
        val vm = HomeViewModel(
            session(backgroundScope, StartFocus.FIRST_MEDIA_ROW),
            listOf(source(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app))), source(HomeRowUi.Apps(x, listOf(app)))),
        )
        assertEquals(x, vm.initialFocus.value)
    }

    @Test fun rowsFollowProfileOrder() = runTest(UnconfinedTestDispatcher()) {
        val x = HomeRowKey("x")
        val vm = HomeViewModel(
            session(backgroundScope, StartFocus.FAVORITE_APPS, rowOrder = listOf(x)),
            listOf(source(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app))), source(HomeRowUi.Apps(x, listOf(app)))),
        )
        assertEquals(listOf(x, HomeRowKey.APPS), vm.rows.value!!.map { it.key })
        assertEquals(HomeRowKey.APPS, vm.initialFocus.value)
    }

    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Heim", "http://jf", "u", "U")
    private fun entry(id: String) = SpotlightEntry(MediaItem(id, "a", ItemType.MOVIE, id, art = ItemArt(backdrop = "b-$id")), account, false)
    private suspend fun TestScope.spotlightVm(vararg ids: String): HomeViewModel {
        val vm = HomeViewModel(session(backgroundScope, StartFocus.FIRST_MEDIA_ROW), emptyList()) { flowOf(ids.map(::entry)) }
        backgroundScope.launch { vm.spotlight.collect {} }
        return vm
    }

    @Test fun spotlightRotatesThroughTheLibraryPool() = runTest(UnconfinedTestDispatcher()) {
        val vm = spotlightVm("a", "b")
        vm.setSpotlightRotation(true)
        assertEquals("a", vm.spotlight.value?.item?.id)
        advanceTimeBy(SPOTLIGHT_MILLIS + 1); assertEquals("b", vm.spotlight.value?.item?.id)
        advanceTimeBy(SPOTLIGHT_MILLIS); assertEquals("a", vm.spotlight.value?.item?.id)
    }

    @Test fun spotlightHoldsWhenRotationIsOff() = runTest(UnconfinedTestDispatcher()) {
        val vm = spotlightVm("a", "b")
        vm.setSpotlightRotation(false)
        advanceTimeBy(SPOTLIGHT_MILLIS * 5); assertEquals("a", vm.spotlight.value?.item?.id)
    }

    @Test fun spotlightTimingFollowsActiveProfileAndChangesWithoutReloadingPool() = runTest(UnconfinedTestDispatcher()) {
        val repo = ProfileRepository(JsonFileStore(tmp.root.resolve("timing.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val id = repo.create("A", AvatarColor.LIME, false).id
        val session = ProfileSession(repo, backgroundScope).also { it.enter(id) }
        var loads = 0
        val vm = HomeViewModel(session, emptyList()) { loads++; flowOf(listOf(entry("a"), entry("b"))) }
        backgroundScope.launch { vm.spotlight.collect {} }
        for (time in listOf(SpotlightTime.FAST, SpotlightTime.SLOW, SpotlightTime.NORMAL)) {
            repo.update(id) { it.copy(appearance = it.appearance.copy(spotlightTime = time)) }
            val before = vm.spotlight.value
            advanceTimeBy(time.millis - 1)
            assertEquals(before, vm.spotlight.value)
            advanceTimeBy(2)
            org.junit.Assert.assertNotEquals(before, vm.spotlight.value)
        }
        assertEquals(1, loads)
    }

    @Test fun focusingACardLeavesTheSpotlightAlone() = runTest(UnconfinedTestDispatcher()) {
        val vm = spotlightVm("a", "b")
        vm.focus(FocusedTarget.Media(MediaItem("ep", "a", ItemType.EPISODE, "Episode", art = ItemArt(backdrop = "b-ep")), account))
        assertEquals("a", vm.spotlight.value?.item?.id)
    }
}
