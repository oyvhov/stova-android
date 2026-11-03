package app.stova.sonos

import app.stova.core.storage.StovaJson
import app.stova.ha.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SonosRepositoryTest {
    @Test fun sonosFavouritePlaysItsOpaqueIdAndTypeOnTheGroupCoordinator() = runBlocking {
        val connection = FakeHaConnection()
        val members = """["media_player.a","media_player.b"]"""
        connection.entities.value = listOf(room("media_player.a", members), room("media_player.b", members)).associateBy { it.id }
        connection.registryValue = HaRegistry(connection.entities.value.keys.associateWith { HaEntityInfo(platform = "sonos") })
        val repo = SonosRepository(connection) { true }
        repo.observe().first { it.rooms.isNotEmpty() }
        val favourite = SonosMedia("FV:2/31", "favorite_item_id", "My radio", playable = true)
        var target: String? = null
        var payload: JsonObject? = null
        connection.service = { domain, service, id, data ->
            assertEquals("media_player", domain); assertEquals("play_media", service)
            target = id; payload = data; true
        }
        assertTrue(repo.perform("media_player.b", SonosAction.PlayMedia(favourite)))
        assertEquals("media_player.a", target)
        assertEquals("FV:2/31", payload?.get("media_content_id")?.jsonPrimitive?.content)
        assertEquals("favorite_item_id", payload?.get("media_content_type")?.jsonPrimitive?.content)
        assertFalse(repo.perform("media_player.b", SonosAction.PlayMedia(favourite.copy(playable = false))))
        assertEquals(1, connection.calls)
    }
    @Test fun failedUngroupStopsBeforeMovingOtherRoomsAndStaleMembershipIsRejected() = runBlocking {
        val connection = FakeHaConnection()
        connection.allStates = listOf(room("media_player.a", """["media_player.a","media_player.b"]"""), room("media_player.b"), room("media_player.c"))
        connection.entities.value = connection.allStates.associateBy { it.id }
        connection.registryValue = HaRegistry(connection.allStates.associate { it.id to HaEntityInfo(platform = "sonos") })
        val repo = SonosRepository(connection) { true }
        repo.observe().first { it.rooms.isNotEmpty() }
        assertFalse(repo.group("media_player.a", setOf("media_player.a", "media_player.c"), setOf("media_player.a")))
        assertEquals(0, connection.calls)
        val sent = mutableListOf<String>()
        connection.service = { _, service, _, _ -> sent += service; false }
        assertFalse(repo.group("media_player.a", setOf("media_player.a", "media_player.c"), setOf("media_player.a", "media_player.b")))
        assertEquals(listOf("unjoin"), sent)
    }
    @Test fun unsupportedCommandsAndOfflineRoomsDoNotSendAnything() = runBlocking {
        val connection = FakeHaConnection()
        connection.entities.value = mapOf("media_player.a" to room("media_player.a").copy(attributes = JsonObject(emptyMap())))
        connection.registryValue = HaRegistry(mapOf("media_player.a" to HaEntityInfo(platform = "sonos")))
        val repo = SonosRepository(connection) { true }
        repo.observe().first { it.rooms.isNotEmpty() }
        assertFalse(repo.perform("media_player.a", SonosAction.PlayPause))
        connection.status.value = HaStatus.DISCONNECTED
        assertFalse(repo.perform("media_player.a", SonosAction.Volume(25)))
        assertEquals(0, connection.calls)
    }
    private fun room(id: String, group: String = "[]") = HaEntity(id, "playing",
        StovaJson.parseToJsonElement("""{"supported_features":1048575,"group_members":$group,"volume_level":0.2}""").jsonObject, 0, 0)
    @Test fun disabledModuleDoesNotTouchConnection() = runBlocking {
        val connection = FakeHaConnection()
        val repo = SonosRepository(connection) { false }
        assertTrue(repo.observe().first().rooms.isEmpty())
        assertFalse(repo.perform("media_player.a", SonosAction.PlayPause))
        assertNull(repo.browse("media_player.a"))
        assertEquals(0, connection.holders + connection.registryRequests + connection.stateRequests + connection.calls)
    }
    @Test fun transportCommandsTargetCoordinatorAndVolumeTargetsSelectedRoom() = runBlocking {
        val connection = FakeHaConnection()
        val group = """["media_player.a","media_player.b"]"""
        connection.allStates = listOf(room("media_player.a", group), room("media_player.b", group))
        connection.entities.value = connection.allStates.associateBy { it.id }
        connection.registryValue = HaRegistry(connection.allStates.associate { it.id to HaEntityInfo(platform = "sonos") })
        val repo = SonosRepository(connection) { true }
        repo.observe().first { it.rooms.isNotEmpty() }
        val sent = mutableListOf<Triple<String, String, JsonObject>>()
        connection.service = { _, service, id, data -> sent += Triple(id, service, data); true }
        assertTrue(repo.perform("media_player.b", SonosAction.PlayPause))
        assertTrue(repo.perform("media_player.b", SonosAction.Volume(35)))
        assertEquals("media_player.a", sent[0].first)
        assertEquals("media_pause", sent[0].second)
        assertEquals("media_player.b", sent[1].first)
        assertEquals(0.35, sent[1].third["volume_level"]!!.jsonPrimitive.double, 0.0001)
        assertEquals(0, connection.holders)
    }
    @Test fun unknownPlayerAndDisabledModuleCannotSendCommandsFromAnOldSnapshot() = runBlocking {
        val connection = FakeHaConnection()
        connection.allStates = listOf(room("media_player.a"))
        connection.entities.value = connection.allStates.associateBy { it.id }
        connection.registryValue = HaRegistry(mapOf("media_player.a" to HaEntityInfo(platform = "sonos")))
        var enabled = true
        val repo = SonosRepository(connection) { enabled }
        repo.observe().first { it.rooms.isNotEmpty() }
        assertFalse(repo.perform("media_player.unknown", SonosAction.PlayPause))
        enabled = false
        assertFalse(repo.perform("media_player.a", SonosAction.PlayPause))
        assertEquals(0, connection.calls)
    }
}
