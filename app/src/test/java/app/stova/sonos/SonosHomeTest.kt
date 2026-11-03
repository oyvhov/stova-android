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
    @Test fun onlyTvAudioInTheSelectedRoomBlocksHomeArtwork() {
        val prefs = SonosPrefs(enabled = true, homeArtwork = true)
        listOf("""{"source":"TV","entity_picture":"/api/old-album"}""",
            """{"source":"tv","entity_picture":"/api/old-album"}""",
            """{"media_content_id":"x-sonos-htastream:RINCON_123:spdif","entity_picture":"/api/old-album"}""").forEach { attrs ->
            val tv = room.copy(entity = room.entity.copy(id = "media_player.soundbar", attributes = StovaJson.parseToJsonElement(attrs).jsonObject))
            assertEquals(room, sonosHomeRoom(prefs, adult, listOf(tv, room)))
            assertEquals(room, sonosHomeRoom(prefs.copy(preferredRoom = room.id), adult, listOf(tv, room)))
            assertNull(sonosHomeRoom(prefs.copy(preferredRoom = tv.id), adult, listOf(tv, room)))
        }
    }

    @Test fun selectedRoomAlsoFollowsTvAudioFromItsOwnGroupCoordinator() {
        val tv = room.copy(entity = room.entity.copy(id = "media_player.tv",
            attributes = StovaJson.parseToJsonElement("""{"source":"TV"}""").jsonObject))
        val grouped = room.copy(entity = room.entity.copy(id = "media_player.grouped",
            attributes = StovaJson.parseToJsonElement("""{"group_members":["media_player.tv","media_player.grouped"]}""").jsonObject))
        val prefs = SonosPrefs(enabled = true, homeArtwork = true, preferredRoom = grouped.id)
        assertNull(sonosHomeRoom(prefs, adult, listOf(room, tv, grouped)))
    }

    @Test fun widgetWaitsOneMinuteAfterPauseAndPrefersAnotherPlayingRoom() {
        val paused = room.copy(entity = room.entity.copy(state = "paused", lastChanged = 10_000))
        val other = room.copy(entity = room.entity.copy(id = "media_player.b"), name = "B")
        val prefs = SonosPrefs(enabled = true, preferredRoom = paused.id)
        assertEquals(paused, sonosWidgetRoom(prefs, SonosState(listOf(paused)), now = 69_999))
        assertNull(sonosWidgetRoom(prefs, SonosState(listOf(paused)), now = 70_000))
        assertEquals(other, sonosWidgetRoom(prefs, SonosState(listOf(paused, other)), now = 11_000))
    }
}
