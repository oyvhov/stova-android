package app.stova.home

import app.stova.apps.InstalledApp
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import app.stova.profiles.Season
import app.stova.profiles.SeasonChoice
import app.stova.profiles.season
import app.stova.profiles.Profile
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
import kotlinx.coroutines.test.advanceUntilIdle
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
    @Test fun returningHomeUsesTheLastRowAndLeavingTheProfileForgetsIt() = runTest(UnconfinedTestDispatcher()) {
        val session = session(backgroundScope, StartFocus.FAVORITE_APPS)
        val later = HomeRowKey("later")
        val vm = HomeViewModel(session,listOf(source(HomeRowUi.Apps(HomeRowKey.APPS,listOf(app)),HomeRowUi.Apps(later,listOf(app)))))
        vm.focusRow(later)
        assertEquals(later,vm.initialFocus.value)
        session.leave()
        assertEquals(null,vm.initialFocus.value)
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

    @Test fun calendarAndManualSeasonChangesReloadRowsAndSpotlightWithoutSavingTheOverlay() = runTest(UnconfinedTestDispatcher()) {
        val repo = ProfileRepository(JsonFileStore(tmp.root.resolve("seasons.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val id = repo.create("A", AvatarColor.LIME, false).id
        val session = ProfileSession(repo, backgroundScope).also { it.enter(id) }
        val rowSeasons = mutableListOf<app.stova.profiles.Season>()
        val spotSeasons = mutableListOf<app.stova.profiles.Season>()
        // The media source: it follows the season, after a run of changes has settled.
        val source = object : HomeRowSource {
            override val followsSeason = true
            override fun rows(profile: Profile) = flowOf(emptyList<HomeRowUi>()).also { rowSeasons += profile.appearance.season() }
        }
        fun settle() = advanceTimeBy(SEASON_SETTLE_MILLIS + 1)
        val vm = HomeViewModel(session, listOf(source)) { profile -> spotSeasons += profile.appearance.season(); flowOf(emptyList()) }
        backgroundScope.launch { vm.spotlight.collect {} }
        vm.setCalendarDate(java.time.LocalDate.of(2026,10,25)); settle()
        assertEquals(Season.HALLOWEEN,rowSeasons.last())
        vm.setCalendarDate(java.time.LocalDate.of(2026,11,2)); settle()
        assertEquals(Season.NONE,rowSeasons.last())
        assertEquals(rowSeasons,spotSeasons)
        val count = rowSeasons.size
        repo.update(id) { it.copy(appearance=it.appearance.copy(showTitles=true)) }; settle()
        assertEquals(count,rowSeasons.size)
        repo.update(id) { it.copy(appearance=SeasonChoice.CHRISTMAS.applyTo(it.appearance)) }; settle()
        assertEquals(Season.CHRISTMAS,rowSeasons.last())
        assertEquals(rowSeasons,spotSeasons)
    }

    @Test fun enteringASeasonStartsWithItsLeadingTitleEvenIfTheOldPoolWasMidRotation() = runTest(UnconfinedTestDispatcher()) {
        val vm = HomeViewModel(session(backgroundScope, StartFocus.FIRST_MEDIA_ROW), emptyList()) { profile ->
            flowOf(if(profile.appearance.season()==Season.HALLOWEEN) listOf(entry("halloween"),entry("ordinary"))
                else listOf(entry("first"),entry("second")))
        }
        vm.setCalendarDate(java.time.LocalDate.of(2026,11,2))
        backgroundScope.launch { vm.spotlight.collect {} }
        advanceTimeBy(SPOTLIGHT_MILLIS+1)
        assertEquals("second",vm.spotlight.value?.item?.id)
        vm.setCalendarDate(java.time.LocalDate.of(2026,10,25))
        assertEquals("halloween",vm.spotlight.value?.item?.id)
    }

    // The Shield froze when a fast press through Theme reached Halloween or Christmas: every season
    // change restarted every source, the app scan and the media refresh included, once per press.
    @Test fun aSeasonBurstRestartsOnlyTheMediaSourceAndOnlyOnce() = runTest(UnconfinedTestDispatcher()) {
        val repo = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val id = repo.create("A", AvatarColor.LIME, false).id
        val session = ProfileSession(repo, backgroundScope).also { it.enter(id) }
        var appStarts = 0; var mediaStarts = 0
        val apps = HomeRowSource { appStarts++; flowOf(listOf(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app)))) }
        val media = object : HomeRowSource {
            override val followsSeason = true
            override fun rows(profile: Profile) = kotlinx.coroutines.flow.flow { mediaStarts++; emit(emptyList<HomeRowUi>()) }
        }
        val vm = HomeViewModel(session, listOf(apps, media))
        vm.setCalendarDate(java.time.LocalDate.of(2026, 6, 1))
        advanceUntilIdle()
        val settled = mediaStarts
        for (choice in listOf(SeasonChoice.CHRISTMAS, SeasonChoice.HALLOWEEN,
                SeasonChoice.NONE, SeasonChoice.CHRISTMAS)) {
            repo.update(id) { it.copy(appearance = choice.applyTo(it.appearance)) }
            advanceTimeBy(120)
        }
        advanceUntilIdle()
        assertEquals(1, appStarts)
        assertEquals(settled + 1, mediaStarts)
        assertEquals(listOf(HomeRowKey.APPS), vm.rows.value!!.map { it.key })
    }
}
