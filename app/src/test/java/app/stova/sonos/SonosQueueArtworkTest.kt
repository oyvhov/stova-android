package app.stova.sonos

import app.stova.ha.HaEntity
import app.stova.ha.FakeHaConnection
import app.stova.ha.HaConnection
import app.stova.ha.HaEntityInfo
import app.stova.ha.HaRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    @Test fun nativeQueueIsEligibleWhenTheCurrentTrackHasAQueuePosition() {
        val playing = room(native.id, """{"media_content_id":"x-sonos-spotify:track:1","queue_position":2,"queue_size":3,"supported_features":131072}""")
        val target = sonosQueueTarget(playing, SonosState(listOf(playing)))!!
        assertEquals(playing.id, target.player)
        assertEquals("Q:0", target.queue)
        assertEquals("x-sonos-spotify:track:1", target.track)
        assertEquals(2, target.position)
    }

    @Test fun nativePredictionChecksTheCurrentItemAndPositionBeforeTakingTheNextPicture() {
        val target=SonosQueueTarget(native.id,"Q:0","track:1",1)
        val queue=SonosMedia("Q:0","playlist","",children=listOf(
            SonosMedia("track:1","music","First","/one"),SonosMedia("track:2","music","Second","/two")))
        val next=nextNativeSonosArtwork(target,queue,false)!!
        assertEquals("/two",next.path)
        assertTrue(next.matches(target.copy(track="track:2",position=2)))
        assertFalse(next.matches(target.copy(track="track:2",position=1)))
        assertNull(nextNativeSonosArtwork(target.copy(position=2),queue,false))
        assertNull(nextNativeSonosArtwork(target,queue.copy(id="other"),false))
        assertNull(nextNativeSonosArtwork(target,queue.copy(children=queue.children.reversed()),false))
    }

    @Test fun nativeEndOfQueueWrapsOnlyWhenRepeatAllIsOn() {
        val target=SonosQueueTarget(native.id,"Q:0","track:2",2)
        val queue=SonosMedia("Q:0","playlist","",children=listOf(
            SonosMedia("track:1","music","First","/one"),SonosMedia("track:2","music","Second","/two")))
        assertNull(nextNativeSonosArtwork(target,queue,false))
        assertTrue(nextNativeSonosArtwork(target,queue,true)!!.matches(target.copy(track="track:1",position=1)))
        assertNull(nextNativeSonosArtwork(target,queue.copy(children=listOf(queue.children[0].copy(artwork=null),queue.children[1])),true))
    }

    @Test fun nativeShuffleAndRepeatOneDoNotGuessWhichTrackComesNext() {
        for(extra in listOf("\"shuffle\":true","\"repeat\":\"one\"")) {
            val playing=room(native.id,"""{"media_content_id":"track:1","queue_position":1,"supported_features":131072,$extra}""")
            assertNull(sonosQueueTarget(playing,SonosState(listOf(playing))))
        }
    }

    @Test fun nativeQueueUsesHomeAssistantBrowseWithoutCallingMusicAssistant() = runBlocking {
        val playing=room(native.id,"""{"media_content_id":"track:1","queue_position":1,"supported_features":131072}""")
        val fake=FakeHaConnection()
        fake.entities.value=mapOf(playing.id to playing.entity)
        fake.registryValue=HaRegistry(mapOf(playing.id to HaEntityInfo(platform="sonos")))
        val requests=mutableListOf<Triple<String,String?,String?>>()
        val connection=object:HaConnection by fake {
            override suspend fun serviceResponse(domain:String,service:String,entityId:String):JsonObject? {
                fail("Native Sonos must not query another integration")
                return null
            }
            override suspend fun browseMedia(entityId:String,contentType:String?,contentId:String?):JsonObject {
                requests+=Triple(entityId,contentType,contentId)
                return json("""{"media_content_id":"Q:0","media_content_type":"playlist","children":[
                    {"media_content_id":"track:1","title":"First","thumbnail":"/one"},
                    {"media_content_id":"track:2","title":"Second","thumbnail":"/two"}]}""")
            }
        }
        val repository=SonosRepository(connection) {true}
        val state=repository.observe().first {it.rooms.isNotEmpty()}
        val expected=sonosQueueTarget(state.rooms.first(),state)!!
        assertEquals("/two",repository.nextArtwork(playing.id,expected)?.path)
        assertEquals(listOf(Triple(playing.id,"playlist","Q:0")),requests)
        assertNull(repository.nextArtwork(playing.id,expected.copy(track="other")))
        assertEquals(1,requests.size)
    }

    private fun room(id: String, attrs: String = "{}") = SonosRoom(HaEntity(id, "playing", json(attrs), 0, 0), id)
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
}
