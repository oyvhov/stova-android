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
    @Test fun alarmUnavailableAndRevokedPermissionNeverSend() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val adult=p.create("A",AvatarColor.LIME,false);s.enter(adult.id);runCurrent()
        val alarm=HaEntity("alarm_control_panel.home","disarmed",JsonObject(emptyMap()),0,0)
        val missing=HaEntity("light.a","unavailable",JsonObject(emptyMap()),0,0)
        repo.add(alarm,CardType.SENSOR);repo.add(missing,CardType.LIGHT);runCurrent()
        socket.entities.value=mapOf(alarm.id to alarm,missing.id to missing);runCurrent()
        assertFalse(repo.perform(ServiceCall("alarm_control_panel","alarm_arm_away"),alarm.id,adult))
        assertFalse(repo.perform(ServiceCall("light","toggle"),missing.id,adult))
        p.update(adult.id) {it.copy(kid=true,kidSmartHomeControl=false)};runCurrent()
        assertFalse(repo.perform(ServiceCall("light","toggle"),missing.id,adult))
        assertEquals(0,socket.calls)
    }
    @Test fun sceneReceiptBelongsToItsCardAndExpiresAfterTwoSeconds() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val adult=p.create("A",AvatarColor.LIME,false);s.enter(adult.id);runCurrent()
        val scene=HaEntity("scene.night","off",JsonObject(emptyMap()),0,0)
        repo.add(scene,CardType.SCENE);runCurrent();socket.entities.value=mapOf(scene.id to scene);runCurrent()
        assertTrue(repo.perform(ServiceCall("scene","turn_on"),scene.id,adult));runCurrent()
        assertTrue(scene.id in repo.completed.value)
        advanceTimeBy(1999);runCurrent();assertTrue(scene.id in repo.completed.value)
        advanceTimeBy(1);runCurrent();assertFalse(scene.id in repo.completed.value)
    }
    @Test fun coverPositionWaitsForActualPositionEvent() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val adult=p.create("A",AvatarColor.LIME,false);s.enter(adult.id);runCurrent()
        val cover=HaEntity("cover.curtain","open",kotlinx.serialization.json.buildJsonObject {put("current_position",kotlinx.serialization.json.JsonPrimitive(50))},0,0)
        repo.add(cover,CardType.COVER);runCurrent();socket.entities.value=mapOf(cover.id to cover);runCurrent()
        val call=ServiceCall("cover","set_cover_position",kotlinx.serialization.json.buildJsonObject {put("position",kotlinx.serialization.json.JsonPrimitive(60))})
        val job=backgroundScope.async {repo.perform(call,cover.id,adult)};runCurrent()
        assertFalse(job.isCompleted);assertTrue(cover.id in repo.pending.value)
        socket.entities.value=mapOf(cover.id to optimisticEntity(cover,call));runCurrent()
        assertTrue(job.await());assertTrue(repo.pending.value.isEmpty())
    }
    // Review finding 1: HA reports brightness 64 for a predicted 63.75; the wait ran its full 2 s, and an
    // adjustment inside it was refused as "busy" and shown as failed.
    @Test fun integerBrightnessConfirmsAtOnceSoTheNextAdjustmentIsAccepted() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val adult=p.create("A",AvatarColor.LIME,false);s.enter(adult.id);runCurrent()
        fun light(brightness:Int)=HaEntity("light.ph","on",kotlinx.serialization.json.buildJsonObject {put("brightness",kotlinx.serialization.json.JsonPrimitive(brightness))},0,0)
        repo.add(light(51),CardType.LIGHT);runCurrent();socket.entities.value=mapOf("light.ph" to light(51));runCurrent()
        val first=backgroundScope.async {repo.perform(brightnessCall(25),"light.ph",adult)};runCurrent()
        socket.entities.value=mapOf("light.ph" to light(64));runCurrent()
        assertTrue(first.isCompleted);assertTrue(first.await());assertTrue(repo.pending.value.isEmpty())
        val second=backgroundScope.async {repo.perform(brightnessCall(35),"light.ph",adult)};runCurrent()
        socket.entities.value=mapOf("light.ph" to light(89));runCurrent()
        assertTrue(second.await())
    }

    // Review finding 5: one registry list timing out replaced the rooms with an empty registry.
    @Test fun aPartlyFailedRegistryKeepsTheRoomsWeHave() = runTest {
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val rooms=HaRegistry(mapOf("light.a" to HaEntityInfo(room="Stove")))
        socket.registryValue=rooms;socket.status.value=HaStatus.DISCONNECTED;runCurrent()
        socket.status.value=HaStatus.CONNECTED;runCurrent()
        assertEquals(rooms,repo.registry.value)
        socket.registryValue=null;socket.status.value=HaStatus.DISCONNECTED;runCurrent()
        socket.status.value=HaStatus.CONNECTED;runCurrent()
        assertEquals(rooms,repo.registry.value)
    }

    @Test fun watchFollowsActiveProfileCards() = runTest {
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection()
        val repo = HaRepository(socket, s, p, backgroundScope).also { it.showingDashboard(true) }
        val a = p.create("A", AvatarColor.LIME, false); val b = p.create("B", AvatarColor.ROSE, false)
        s.enter(a.id); runCurrent()
        repo.add(HaEntity("light.stove", "on", JsonObject(emptyMap()), 0, 0), CardType.LIGHT); runCurrent()
        assertEquals(setOf("light.stove"), socket.watches.last())
        s.enter(b.id); runCurrent(); assertEquals(emptySet<String>(), socket.watches.last())
        assertTrue(repo.cards.value.isEmpty())
    }
    @Test fun maxThreeOnHomeAndReorderPersist() = runTest {
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection(); val repo = HaRepository(socket, s, p, backgroundScope).also { it.showingDashboard(true) }
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
        val p = profiles(); val s = ProfileSession(p, backgroundScope); val socket = FakeHaConnection(); val repo = HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
        val kid = Profile("k","K",AvatarColor.ROSE,kid=true)
        assertFalse(repo.perform(ServiceCall("light","toggle"),"light.stove",kid)); assertEquals(0,socket.calls)
    }
    @Test fun optimisticToggleIsImmediatePendingAndRollsBackOnFailure() = runTest {
        val p=profiles(); val s=ProfileSession(p,backgroundScope); val socket=FakeHaConnection()
        val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
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
        val p=profiles();val s=ProfileSession(p,backgroundScope);val socket=FakeHaConnection();val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) }
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
        val repo=HaRepository(socket,s,p,backgroundScope).also { it.showingDashboard(true) };s.enter(p.create("A",AvatarColor.LIME,false).id);runCurrent()
        listOf("light.a","light.b","light.c").forEach {repo.add(HaEntity(it,"on",JsonObject(emptyMap()),0,0),CardType.LIGHT);runCurrent()}
        val last=repo.cards.value.last()
        repo.move(last.id,-1);runCurrent()
        assertEquals(listOf("light.c","light.a"),dashboardSections(repo.cards.value,socket.registryValue!!).first {it.room=="Stove"}.cards.map {it.entityId})
    }
}
