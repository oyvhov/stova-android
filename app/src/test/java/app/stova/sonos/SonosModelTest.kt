package app.stova.sonos

import app.stova.core.storage.StovaJson
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SonosModelTest {
    @Test fun inlineGroupDraftKeepsAnchorAndRejectsAChangedGroup() {
        val anchor = SonosRoom(HaEntity("media_player.a", "playing", kotlinx.serialization.json.JsonObject(emptyMap()), 0, 0), "A")
        val other = anchor.copy(entity = anchor.entity.copy(id = "media_player.b"), name = "B")
        val draft = SonosGroupDraft(anchor.id, setOf(anchor.id))
        assertEquals(draft, draft.toggle(anchor.id))
        assertFalse(draft.canApply(anchor, listOf(anchor, other)))
        val edited = draft.toggle(other.id)
        assertTrue(edited.canApply(anchor, listOf(anchor, other)))
        assertFalse(edited.canApply(anchor, listOf(anchor)))
        val changed = anchor.copy(entity = anchor.entity.copy(attributes = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"group_members":["media_player.a","media_player.b"]}""").jsonObject))
        assertFalse(edited.canApply(changed, listOf(changed, other)))
    }
    @Test fun oldHouseholdFilesKeepSonosDisabled() {
        val settings = StovaJson.decodeFromString<app.stova.settings.HouseholdSettings>("{}")
        assertFalse(settings.sonos.enabled)
    }
    @Test fun mediaBrowserRetainsArtworkAndCapabilitiesWithoutAssumingEveryItemCanPlay() {
        val parsed = sonosMedia(StovaJson.parseToJsonElement("""{"title":"Music","can_expand":true,"children":[
            {"title":"Favourites","media_content_id":"favorites","media_content_type":"library","can_expand":true},
            {"title":"Radio","media_content_id":"FV:2/31","media_content_type":"favorite_item_id","can_play":true,"thumbnail":"/api/media_player_proxy/a"}]}""").jsonObject)
        assertFalse(parsed.children[0].playable)
        assertTrue(parsed.children[0].expandable)
        assertEquals("/api/media_player_proxy/a", parsed.children[1].artwork)
    }
    private fun entity(id: String, attrs: String, state: String = "playing") =
        HaEntity(id, state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0)

    @Test fun onlySonosPlatformIsIncludedEvenWithAnUnrelatedPlayerNamedSonos() {
        val registry = parseRegistry(JsonArray(emptyList()), JsonArray(emptyList()),
            StovaJson.parseToJsonElement("""[{"entity_id":"media_player.living","platform":"sonos"},{"entity_id":"media_player.sonos_tv","platform":"cast"}]""").jsonArray)
        val rooms = sonosRooms(listOf(entity("media_player.living", "{}"), entity("media_player.sonos_tv", "{}")), registry)
        assertEquals(listOf("media_player.living"), rooms.map { it.id })
    }

    @Test fun groupedRoomUsesCoordinatorMetadataButKeepsItsOwnVolume() {
        val rooms = listOf(
            SonosRoom(entity("media_player.a", """{"media_title":"Song","volume_level":0.2,"group_members":["media_player.a","media_player.b"]}"""), "A"),
            SonosRoom(entity("media_player.b", """{"volume_level":0.6,"group_members":["media_player.a","media_player.b"]}"""), "B"))
        assertEquals("Song", coordinator(rooms[1], rooms)?.title)
        assertEquals(60, rooms[1].volume)
        assertEquals("media_player.a", rooms[1].coordinatorId)
    }

    @Test fun groupingRetainsCoordinatorAndRejectsUnknownRooms() {
        val rooms = listOf(SonosRoom(entity("media_player.a", "{}"), "A"), SonosRoom(entity("media_player.b", "{}"), "B"))
        assertNull(groupPlan(rooms[0], rooms, setOf("media_player.b")))
        assertNull(groupPlan(rooms[0], rooms, setOf("media_player.a", "media_player.fake")))
        val plan = groupPlan(rooms[0], rooms, setOf("media_player.a", "media_player.b"))!!
        assertEquals(listOf("media_player.b"), plan.join)
        assertTrue(plan.leave.isEmpty())
    }

    @Test fun progressUsesMediaTimestampAndClampsToDuration() {
        val room = SonosRoom(entity("media_player.a", """{"media_position":20,"media_duration":100,"media_position_updated_at":"2026-11-03T12:00:00+00:00"}"""), "A")
        assertEquals(25, room.position(java.time.Instant.parse("2026-11-03T12:00:05Z").toEpochMilli()))
        assertEquals(100, room.position(Long.MAX_VALUE))
        assertEquals(20, room.copy(entity = room.entity.copy(state = "paused")).position(Long.MAX_VALUE))
    }
}
