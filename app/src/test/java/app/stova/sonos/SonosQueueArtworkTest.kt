package app.stova.sonos

import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SonosQueueArtworkTest {
    private val native = room("media_player.sonos")
    private val assistant = room("media_player.ma", """{"active_queue":"queue","media_content_id":"library://track/1"}""")

    @Test fun nextImageIsScopedToTheActivePlayerQueueAndTrack() {
        val state = SonosState(listOf(native), musicAssistant = mapOf(native.id to assistant))
        val target = sonosQueueTarget(native, state)!!
        val response = json("""{"media_player.ma":{"queue_id":"queue","current_item":{"media_item":{"uri":"library://track/1"}},"next_item":{"media_item":{"uri":"library://track/2","image":"/art/2"}}}}""")
        val next = nextSonosArtwork(target, response)!!
        assertEquals("/art/2", next.path)
        assertFalse(next.matches(target))
        assertTrue(next.matches(target.copy(track = "library://track/2")))
        assertFalse(next.matches(target.copy(track = "library://track/2", queue = "other")))
        assertNull(nextSonosArtwork(target.copy(track = "library://track/3"), response))
        assertNull(nextSonosArtwork(target.copy(queue = "other"), response))
    }

    @Test fun noQueueLookupForNativeTvPausedOrUnavailablePlayback() {
        assertNull(sonosQueueTarget(native, SonosState(listOf(native))))
        val tv = room(native.id, """{"source":"TV"}""")
        assertNull(sonosQueueTarget(tv, SonosState(listOf(tv), musicAssistant = mapOf(tv.id to assistant))))
        for (status in listOf("paused", "unavailable")) {
            val inactive = assistant.copy(entity = assistant.entity.copy(state = status))
            val state = SonosState(listOf(native), musicAssistant = mapOf(native.id to inactive))
            assertNull(sonosQueueTarget(native, state))
        }
    }

    @Test fun missingArtworkIsNotGuessed() {
        val target = sonosQueueTarget(native, SonosState(listOf(native), musicAssistant = mapOf(native.id to assistant)))!!
        val response = json("""{"media_player.ma":{"queue_id":"queue","current_item":{"media_item":{"uri":"library://track/1"}},"next_item":{"media_item":{"uri":"library://track/2","image":null}}}}""")
        assertNull(nextSonosArtwork(target, response))
    }

    private fun room(id: String, attrs: String = "{}") = SonosRoom(HaEntity(id, "playing", json(attrs), 0, 0), id)
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
}
