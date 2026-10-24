package app.stova.home

import app.stova.apps.InstalledApp
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfileSession
import app.stova.profiles.ProfilesFile
import app.stova.profiles.StartFocus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        assertEquals(listOf(x, HomeRowKey.APPS), vm.rows.value.map { it.key })
        assertEquals(HomeRowKey.APPS, vm.initialFocus.value)
    }
}
