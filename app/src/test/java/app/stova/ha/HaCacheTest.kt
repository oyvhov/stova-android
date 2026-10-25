package app.stova.ha

import app.stova.core.storage.*
import app.stova.core.theme.AvatarColor
import app.stova.ha.cards.*
import app.stova.profiles.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HaCacheTest {
    @get:Rule val dir = TemporaryFolder()
    private val file get() = File(dir.root, "ha-cache.json")
    private fun TestScope.store() = JsonFileStore(file, HaSnapshot.serializer(), HaSnapshot(), StandardTestDispatcher(testScheduler))
    private fun entity(state: String) = HaEntity("weather.home", state, buildJsonObject {
        put("temperature", 4); put("friendly_name", "Heime")
        put("access_token", "secret-token"); put("entity_picture", "/image?token=secret-token")
        put("unused", "private")
    }, 100, 200)

    @Test fun diskCacheIsVisibleWhileSocketIsDisconnectedAndFollowsProfile() = runTest {
        val profiles = ProfileRepository(JsonFileStore(File(dir.root, "profiles.json"), ProfilesFile.serializer(), ProfilesFile(), StandardTestDispatcher(testScheduler)))
        val a = profiles.create("A", AvatarColor.LIME, false)
        val b = profiles.create("B", AvatarColor.ROSE, false)
        profiles.update(a.id) { it.copy(dashboard = listOf(CardConfig("w", CardType.WEATHER, "weather.home", onHome = true))) }
        store().update { HaSnapshot(a.id, mapOf("weather.home" to entity("cloudy"))) }
        val session = ProfileSession(profiles, backgroundScope)
        val socket = FakeHaConnection().apply { status.value = HaStatus.DISCONNECTED }
        val repo = HaRepository(socket, session, profiles, backgroundScope, store())
        session.enter(a.id); runCurrent()
        assertEquals("cloudy", repo.entities.value["weather.home"]?.state)
        assertEquals(4, repo.entities.value["weather.home"]?.attributes?.get("temperature")?.jsonPrimitive?.int)
        assertEquals(200L, repo.entities.value["weather.home"]?.lastUpdated)
        assertEquals(1, repo.homeCards.value.size)
        profiles.update(a.id) { it.copy(dashboard = emptyList()) }; runCurrent()
        assertTrue(repo.entities.value.isEmpty())
        advanceTimeBy(5_000); runCurrent()
        assertTrue(store().current().entities.isEmpty())
        session.enter(b.id); runCurrent()
        assertTrue(repo.entities.value.isEmpty())
    }

    @Test fun freshValuesPersistOnlyNeededAttributesAndThrottleLatestWrite() = runTest {
        val profiles = ProfileRepository(JsonFileStore(File(dir.root, "profiles.json"), ProfilesFile.serializer(), ProfilesFile(), StandardTestDispatcher(testScheduler)))
        val a = profiles.create("A", AvatarColor.LIME, false)
        profiles.update(a.id) { it.copy(dashboard = listOf(CardConfig("w", CardType.WEATHER, "weather.home"))) }
        val session = ProfileSession(profiles, backgroundScope)
        val socket = FakeHaConnection()
        val repo = HaRepository(socket, session, profiles, backgroundScope, store())
        session.enter(a.id); runCurrent()
        socket.entities.value = mapOf("weather.home" to entity("cloudy"), "sensor.unused" to entity("unused").copy(id = "sensor.unused"))
        runCurrent()
        assertEquals("cloudy", store().current().entities["weather.home"]?.state)
        assertEquals(setOf("weather.home"), store().current().entities.keys)
        assertFalse(file.readText().contains("secret-token"))
        assertFalse(file.readText().contains("unused"))
        assertEquals(setOf("temperature", "friendly_name"), store().current().entities.getValue("weather.home").attributes.keys)
        advanceTimeBy(1_000)
        socket.entities.value = mapOf("weather.home" to entity("rainy")); runCurrent()
        socket.entities.value = mapOf("weather.home" to entity("sunny")); runCurrent()
        assertEquals("sunny", repo.entities.value["weather.home"]?.state)
        assertEquals("cloudy", store().current().entities["weather.home"]?.state)
        advanceTimeBy(3_999); runCurrent()
        assertEquals("cloudy", store().current().entities["weather.home"]?.state)
        advanceTimeBy(1); runCurrent()
        assertEquals("sunny", store().current().entities["weather.home"]?.state)
        socket.entities.value = emptyMap(); runCurrent()
        assertTrue(repo.entities.value.isEmpty())
        advanceTimeBy(5_000); runCurrent()
        assertTrue(store().current().entities.isEmpty())
    }
}
