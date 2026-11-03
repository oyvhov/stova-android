package app.stova.sonos

import app.stova.core.storage.StovaJson
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SonosSpeakerTest {
    private fun shape(model: String?, manufacturer: String? = "Sonos") = sonosSpeaker(model, manufacturer).shape

    @Test fun recognisesCurrentModelsAndTheirOlderNamesAndGenerations() {
        mapOf(
            "One" to SonosShape.ONE, "One SL" to SonosShape.ONE, "Sonos One (Gen 2)" to SonosShape.ONE, "Play:1" to SonosShape.ONE,
            "Era 100" to SonosShape.ERA_100, "Era 100 SL" to SonosShape.ERA_100, "Era 300" to SonosShape.ERA_300,
            "Five" to SonosShape.FIVE, "Play:5 (Gen 2)" to SonosShape.FIVE, "PLAY:5" to SonosShape.FIVE, "Play:3" to SonosShape.FIVE,
            "Beam" to SonosShape.BEAM, "Beam (Gen 2)" to SonosShape.BEAM, "Arc" to SonosShape.ARC, "Arc Ultra" to SonosShape.ARC,
            "Ray" to SonosShape.RAY, "Playbar" to SonosShape.PLAYBAR, "Playbase" to SonosShape.PLAYBASE,
            "Move" to SonosShape.MOVE, "Move 2" to SonosShape.MOVE, "Roam" to SonosShape.ROAM, "Roam SL" to SonosShape.ROAM,
            "Roam 2" to SonosShape.ROAM, "Sub" to SonosShape.SUB, "Sub (Gen 3)" to SonosShape.SUB, "Sub 4" to SonosShape.SUB,
            "Sub Mini" to SonosShape.SUB_MINI, "Amp" to SonosShape.COMPONENT, "Connect:Amp" to SonosShape.COMPONENT,
            "Port" to SonosShape.COMPONENT, "Connect" to SonosShape.COMPONENT, "ZP120" to SonosShape.COMPONENT,
        ).forEach { (model, expected) -> assertEquals(model, expected, shape(model)) }
    }

    @Test fun labelKeepsTheReportedNameWithoutTheBrandPrefix() {
        assertEquals("One (Gen 2)", sonosSpeaker("Sonos One (Gen 2)", "Sonos").label)
        assertEquals("Era 300", sonosSpeaker(" Era  300 ", "Sonos").label)
    }

    @Test fun unknownOrMissingModelsGetTheNeutralShapeAndAnHonestLabel() {
        assertEquals(SonosSpeaker(SonosShape.UNKNOWN, null), sonosSpeaker(null, null))
        assertEquals(SonosSpeaker(SonosShape.UNKNOWN, null), sonosSpeaker("  ", "Sonos"))
        // A name that merely starts like a known one is not that speaker.
        assertEquals(SonosShape.UNKNOWN, shape("Arcade 9000"))
        assertEquals(SonosShape.UNKNOWN, shape("Oneplus"))
        val ikea = sonosSpeaker("SYMFONISK Bookshelf", "Sonos")
        assertEquals(SonosShape.UNKNOWN, ikea.shape)
        assertEquals("SYMFONISK Bookshelf", ikea.label)
        // Another maker's "One" is not a Sonos One.
        assertEquals(SonosShape.UNKNOWN, shape("One", "Acme"))
        assertEquals(SonosShape.ONE, shape("One", null))
    }

    private fun entity(id: String, attrs: String = "{}", state: String = "playing") =
        HaEntity(id, state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0)

    @Test fun roomTakesTheModelFromTheDeviceRegistryNeverFromItsName() {
        val registry = parseRegistry(JsonArray(emptyList()), StovaJson.parseToJsonElement(
            """[{"id":"d1","model":"Era 300","manufacturer":"Sonos"},{"id":"d2"}]""").jsonArray,
            StovaJson.parseToJsonElement("""[{"entity_id":"media_player.beam","platform":"sonos","device_id":"d1"},
                {"entity_id":"media_player.arc","platform":"sonos","device_id":"d2"}]""").jsonArray)
        val rooms = sonosRooms(listOf(entity("media_player.beam", """{"friendly_name":"Beam"}"""),
            entity("media_player.arc", """{"friendly_name":"Arc"}""")), registry)
        assertEquals(SonosShape.UNKNOWN, rooms.first { it.id == "media_player.arc" }.speaker.shape)
        val era = rooms.first { it.id == "media_player.beam" }
        assertEquals(SonosShape.ERA_300, era.speaker.shape)
        assertEquals("Era 300", era.speaker.label)
    }

    @Test fun activityFollowsActualMusicNotTvPauseOrStillness() {
        val playing = SonosRoom(entity("media_player.a"), "A")
        assertTrue(sonosSpeakerActive(playing, resumed = true, still = false))
        assertFalse(sonosSpeakerActive(playing, resumed = false, still = false))
        assertFalse(sonosSpeakerActive(playing, resumed = true, still = true))
        assertFalse(sonosSpeakerActive(playing.copy(entity = playing.entity.copy(state = "paused")), true, false))
        assertFalse(sonosSpeakerActive(playing.copy(entity = playing.entity.copy(state = "unavailable")), true, false))
        assertFalse(sonosSpeakerActive(SonosRoom(entity("media_player.a", """{"source":"TV"}"""), "A"), true, false))
        assertFalse(sonosSpeakerActive(null, true, false))
    }

    @Test fun musicAssistantPlaybackKeepsTheNativeSpeakerIdentity() {
        val native = SonosRoom(entity("media_player.stova", state = "idle"), "Stova", model = "Five", manufacturer = "Sonos")
        val ma = SonosRoom(entity("media_player.ma_stova", """{"active_queue":"q"}"""), "Stova (MA)")
        val state = SonosState(listOf(native), loading = false, musicAssistant = mapOf(native.id to ma))
        assertEquals(SonosShape.FIVE, sonosRoomVisual(native, state, resumed = true, still = false).speaker.shape)
        assertTrue(sonosRoomVisual(native, state, resumed = true, still = false).active)
    }

    @Test fun groupMemberShowsItsOwnModelAndTheGroupsMusic() {
        val coordinator = SonosRoom(entity("media_player.a", """{"group_members":["media_player.a","media_player.b"]}"""), "A", model = "Arc")
        val member = SonosRoom(entity("media_player.b", """{"group_members":["media_player.a","media_player.b"]}""", "idle"), "B", model = "Sub Mini")
        val state = SonosState(listOf(coordinator, member), loading = false)
        val visual = sonosRoomVisual(member, state, resumed = true, still = false)
        assertEquals(SonosShape.SUB_MINI, visual.speaker.shape)
        assertTrue(visual.active)
    }
}
