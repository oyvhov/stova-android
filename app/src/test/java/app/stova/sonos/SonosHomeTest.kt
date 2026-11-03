package app.stova.sonos

import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.ha.HaEntity
import app.stova.profiles.*
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class SonosHomeTest {
    private val adult = Profile("a", "A", AvatarColor.LIME)
    private val room = SonosRoom(HaEntity("media_player.a", "playing", StovaJson.parseToJsonElement(
        """{"entity_picture":"/api/art/a"}""").jsonObject, 0, 0), "A")
    @Test fun homeArtworkIsOptInAndNeverAppliesToChildrenOrThemeOnlyProfiles() {
        assertNull(sonosHomeRoom(SonosPrefs(enabled = true), adult, listOf(room)))
        val prefs = SonosPrefs(enabled = true, homeArtwork = true)
        assertEquals(room, sonosHomeRoom(prefs, adult, listOf(room)))
        assertNull(sonosHomeRoom(prefs, adult.copy(kid = true), listOf(room)))
        assertNull(sonosHomeRoom(prefs, adult.copy(appearance = adult.appearance.copy(backgroundSource = BackgroundSource.THEME)), listOf(room)))
    }
    @Test fun pausedUnavailableOrArtlessPlayersFallBackToTheUsualHome() {
        val prefs = SonosPrefs(enabled = true, homeArtwork = true)
        listOf("paused", "idle", "unavailable").forEach { state ->
            assertNull(sonosHomeRoom(prefs, adult, listOf(room.copy(entity = room.entity.copy(state = state)))))
        }
        assertNull(sonosHomeRoom(prefs, adult, listOf(room.copy(entity = room.entity.copy(attributes = StovaJson.parseToJsonElement("{}").jsonObject)))))
    }
    @Test fun lastSelectedRoomWinsWhenSeveralRoomsPlayDifferentMusic() {
        val other = room.copy(entity = room.entity.copy(id = "media_player.b"), name = "B")
        assertEquals(other, sonosHomeRoom(SonosPrefs(enabled = true, homeArtwork = true, preferredRoom = other.id), adult, listOf(room, other)))
    }
    @Test fun musicWidgetSkipsTheTvEvenWhenItIsThePreferredRoom() {
        val tv = room.copy(entity = room.entity.copy(id = "media_player.tv", attributes = StovaJson.parseToJsonElement("""{"source":"TV"}""").jsonObject))
        val prefs = SonosPrefs(enabled = true, preferredRoom = tv.id)
        assertEquals(room, sonosWidgetRoom(prefs, SonosState(listOf(tv, room))))
        assertNull(sonosWidgetRoom(prefs, SonosState(listOf(tv))))
        assertNull(sonosWidgetRoom(prefs.copy(showNowPlaying = false), SonosState(listOf(room))))
    }
    @Test fun tvAudioBlocksHomeArtworkEvenWithStaleAlbumMetadataAndOtherMusicPlaying() {
        val prefs = SonosPrefs(enabled = true, homeArtwork = true)
        listOf("""{"source":"TV","entity_picture":"/api/old-album"}""",
            """{"source":"tv","entity_picture":"/api/old-album"}""",
            """{"media_content_id":"x-sonos-htastream:RINCON_123:spdif","entity_picture":"/api/old-album"}""").forEach { attrs ->
            val tv = room.copy(entity = room.entity.copy(id = "media_player.soundbar", attributes = StovaJson.parseToJsonElement(attrs).jsonObject))
            assertNull(sonosHomeRoom(prefs, adult, listOf(room, tv)))
        }
    }
}
