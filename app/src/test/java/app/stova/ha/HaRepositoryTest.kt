package app.stova.ha

import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.ha.cards.*
import app.stova.profiles.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HaRepositoryTest {
    @get:Rule val dir = TemporaryFolder()
    private fun profiles() = ProfileRepository(JsonFileStore(File(dir.root, "profiles.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()))
    @Test fun watchFollowsActiveProfileCards() = runTest {
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection()
        val repo = HaRepository(socket, s, p, backgroundScope)
        val a = p.create("A", AvatarColor.LIME, false); val b = p.create("B", AvatarColor.ROSE, false)
        s.enter(a.id); runCurrent()
        repo.add(HaEntity("light.stove", "on", JsonObject(emptyMap()), 0, 0), CardType.LIGHT); runCurrent()
        assertEquals(setOf("light.stove"), socket.watches.last())
        s.enter(b.id); runCurrent(); assertEquals(emptySet<String>(), socket.watches.last())
        assertTrue(repo.cards.value.isEmpty())
    }
    @Test fun maxThreeOnHomeAndReorderPersist() = runTest {
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection(); val repo = HaRepository(socket, s, p, backgroundScope)
        s.enter(p.create("A", AvatarColor.LIME, false).id); runCurrent()
        repeat(4) { repo.add(HaEntity("sensor.$it", "1", JsonObject(emptyMap()),0,0), CardType.SENSOR); runCurrent() }
        val ids = repo.cards.value.map { it.id }
        ids.take(3).forEach { assertTrue(repo.setOnHome(it,true)); runCurrent() }
        assertFalse(repo.setOnHome(ids.last(),true)); assertEquals(3, repo.homeCards.value.size)
        repo.move(ids.last(), -1); repo.setWide(ids.last(),true); runCurrent()
        assertEquals(ids.last(), repo.cards.value[2].id); assertTrue(repo.cards.value[2].wide)
        repo.remove(ids[0]); runCurrent(); assertEquals(3, repo.cards.value.size)
    }
    @Test fun kidPerformIsNoop() = runTest {
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection(); val repo = HaRepository(socket,s,p,backgroundScope)
        val kid = Profile("k","K",AvatarColor.ROSE,kid=true)
        assertFalse(repo.perform(ServiceCall("light","toggle"),"light.stove",kid)); assertEquals(0,socket.calls)
    }
}
