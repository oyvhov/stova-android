package app.stova.ha

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.ha.cards.*
import app.stova.profiles.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HaDemandTest {
    @get:Rule val dir = TemporaryFolder()

    @Test fun homeWatchesOnlyItsChipsAndDashboardWatchesEveryCard() = runTest {
        val profiles = ProfileRepository(JsonFileStore(dir.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val person = profiles.create("A", AvatarColor.LIME, false)
        profiles.update(person.id) { it.copy(dashboard = (0..4).map { n -> CardConfig("$n", CardType.LIGHT, "light.$n", onHome = n == 0) }) }
        val session = ProfileSession(profiles, backgroundScope)
        val connection = FakeHaConnection()
        val repository = HaRepository(connection, session, profiles, backgroundScope)
        runCurrent()
        assertEquals(0, connection.registryRequests)
        session.enter(person.id); runCurrent()
        assertEquals(setOf("light.0"), connection.watches.last())
        repository.showingDashboard(true); runCurrent()
        assertEquals((0..4).map { "light.$it" }.toSet(), connection.watches.last())
        repository.showingDashboard(false); runCurrent()
        profiles.update(person.id) { it.copy(appearance = it.appearance.copy(showHaChips = false)) }; runCurrent()
        assertEquals(emptySet<String>(), connection.watches.last())
    }

    @Test fun registryIsReusedAcrossReconnectAndAddFlowForSixHours() = runTest {
        val profiles = ProfileRepository(JsonFileStore(dir.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher(testScheduler)))
        val session = ProfileSession(profiles, backgroundScope)
        val connection = FakeHaConnection().apply { registryValue = HaRegistry(mapOf("light.a" to HaEntityInfo(room = "Stove"))) }
        val repository = HaRepository(connection, session, profiles, backgroundScope, clock = { testScheduler.currentTime })
        repository.showingDashboard(true); runCurrent()
        assertEquals(1, connection.registryRequests)
        connection.status.value = HaStatus.DISCONNECTED; runCurrent()
        connection.status.value = HaStatus.CONNECTED; runCurrent()
        repository.loadRegistry()
        assertEquals(1, connection.registryRequests)
        advanceTimeBy(6 * 60 * 60 * 1_000L)
        repository.loadRegistry()
        assertEquals(2, connection.registryRequests)
    }
}
