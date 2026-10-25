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
    @Test fun optimisticToggleIsImmediatePendingAndRollsBackOnFailure() = runTest {
        val p=profiles(); val s=ProfileSession(p,backgroundScope); val socket=FakeHaConnection()
        val repo=HaRepository(socket,s,p,backgroundScope)
        val adult=p.create("A",AvatarColor.LIME,false); s.enter(adult.id); runCurrent()
        val light=HaEntity("light.a","off",JsonObject(emptyMap()),0,0)
        repo.add(light,CardType.LIGHT); runCurrent()
        socket.entities.value=mapOf(light.id to light);runCurrent()
        val answer=CompletableDeferred<Boolean>();socket.service={_,_,_,_->answer.await()}
        val call=backgroundScope.async {repo.perform(ServiceCall("light","toggle"),light.id,adult)}
        runCurrent();assertEquals("on",repo.entities.value[light.id]?.state);assertTrue(light.id in repo.pending.value)
        assertFalse(repo.perform(ServiceCall("light","toggle"),light.id,adult));assertEquals(1,socket.calls)
        answer.complete(false);runCurrent()
        assertFalse(call.await());assertEquals("off",repo.entities.value[light.id]?.state);assertTrue(repo.pending.value.isEmpty())
        assertEquals(HaFeedback.FAILED,repo.feedback.value)
    }
    @Test fun customSizeNameIconFavoritePersist() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val repo=HaRepository(FakeHaConnection(),s,p,backgroundScope)
        s.enter(p.create("A",AvatarColor.LIME,false).id);runCurrent()
        repo.add(HaEntity("light.a","off",JsonObject(emptyMap()),0,0),CardType.LIGHT);runCurrent()
        val id=repo.cards.value.single().id
        repo.configure(id,CardSize.L,"  Leselys  ","sun",true);runCurrent()
        val card=repo.cards.value.single()
        assertEquals(CardSize.L,card.size);assertEquals("Leselys",card.name);assertEquals("sun",card.icon);assertTrue(card.favorite)
        assertEquals(card,p.current().profiles.single().dashboard.single())
    }
    @Test fun successfulToggleWaitsForServerAndClearsPending() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope)
        val adult=p.create("A",AvatarColor.LIME,false);s.enter(adult.id);runCurrent()
        val light=HaEntity("light.a","off",JsonObject(emptyMap()),0,0)
        repo.add(light,CardType.LIGHT);runCurrent();socket.entities.value=mapOf(light.id to light);runCurrent()
        val job=backgroundScope.async {repo.perform(ServiceCall("light","toggle"),light.id,adult)}
        runCurrent();assertFalse(job.isCompleted);assertTrue(light.id in repo.pending.value)
        socket.entities.value=mapOf(light.id to light.copy(state="on"));runCurrent()
        assertTrue(job.await());assertTrue(repo.pending.value.isEmpty());assertEquals("on",repo.entities.value[light.id]?.state)
    }
    @Test fun movingCardsStaysWithinTheirVisibleRoomSection() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection().apply {
            registryValue=HaRegistry(mapOf("light.a" to HaEntityInfo(room="Stove"),"light.b" to HaEntityInfo(room="Gang"),"light.c" to HaEntityInfo(room="Stove")))
        }
        val repo=HaRepository(socket,s,p,backgroundScope);s.enter(p.create("A",AvatarColor.LIME,false).id);runCurrent()
        listOf("light.a","light.b","light.c").forEach {repo.add(HaEntity(it,"on",JsonObject(emptyMap()),0,0),CardType.LIGHT);runCurrent()}
        val last=repo.cards.value.last()
        repo.move(last.id,-1);runCurrent()
        assertEquals(listOf("light.c","light.a"),dashboardSections(repo.cards.value,socket.registryValue).first {it.room=="Stove"}.cards.map {it.entityId})
    }
}
