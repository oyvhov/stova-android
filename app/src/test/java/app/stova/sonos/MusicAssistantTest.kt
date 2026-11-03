package app.stova.sonos

import app.stova.core.storage.StovaJson
import app.stova.ha.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class MusicAssistantTest {
    private fun player(id: String, extra: String = "") = HaEntity(id, "playing", StovaJson.parseToJsonElement(
        """{"supported_features":3145727,"friendly_name":"Stova","volume_level":0.3$extra}""").jsonObject, 0, 0)
    @Test fun explicitlyLinkedMusicAssistantStaysOutOfRoomListAndReceivesItsOwnPlayback() = runBlocking {
        val fake = FakeHaConnection()
        fake.entities.value = listOf(player("media_player.sonos"), player("media_player.ma", ""","active_queue":"sonos-123","media_title":"From Music Assistant"""")).associateBy { it.id }
        fake.registryValue = HaRegistry(mapOf("media_player.sonos" to HaEntityInfo(platform = "sonos"), "media_player.ma" to HaEntityInfo(platform = "music_assistant")))
        val repo = SonosRepository(fake, musicAssistantLinks = { mapOf("media_player.sonos" to "media_player.ma") }) { true }
        val state = repo.observe().first { it.rooms.isNotEmpty() }
        assertEquals(listOf("media_player.sonos"), state.rooms.map { it.id })
        assertEquals("media_player.ma", state.musicAssistant["media_player.sonos"]?.id)
        assertEquals("From Music Assistant", sonosPlaybackRoom(state.rooms.single(), state)?.title)
        val targets = mutableListOf<String>()
        fake.service = { _, _, id, _ -> targets += id; true }
        assertTrue(repo.perform("media_player.sonos", SonosAction.Next))
        assertTrue(repo.perform("media_player.sonos", SonosAction.Volume(24)))
        assertTrue(repo.perform("media_player.ma", SonosAction.PlayMedia(SonosMedia("library://track/42", "track", "Track", playable = true))))
        assertTrue(repo.perform("media_player.sonos", SonosAction.PlayMedia(SonosMedia("FV:2/31", "favorite_item_id", "Radio", playable = true))))
        assertEquals(listOf("media_player.ma", "media_player.sonos", "media_player.ma", "media_player.sonos"), targets)
    }
    @Test fun sameNamesDoNotLinkPlayersAndAnInvalidPlatformCannotBeMapped() = runBlocking {
        val fake = FakeHaConnection()
        fake.entities.value = listOf(player("media_player.sonos"), player("media_player.cast")).associateBy { it.id }
        fake.registryValue = HaRegistry(mapOf("media_player.sonos" to HaEntityInfo(platform = "sonos"), "media_player.cast" to HaEntityInfo(platform = "cast")))
        val repo = SonosRepository(fake, musicAssistantLinks = { mapOf("media_player.sonos" to "media_player.cast") }) { true }
        assertTrue(repo.observe().first { it.rooms.isNotEmpty() }.musicAssistant.isEmpty())
        assertFalse(repo.perform("media_player.cast", SonosAction.Next))
    }

    @Test fun tvAudioOverridesAStaleMusicAssistantQueueAndArtwork() {
        val tv = SonosRoom(player("media_player.sonos", ""","source":"TV","entity_picture":"/old.jpg""""), "Soundbar")
        val ma = SonosRoom(player("media_player.ma", ""","active_queue":"q","entity_picture":"/ma.jpg""""), "Music")
        val state = SonosState(listOf(tv), musicAssistant = mapOf(tv.id to ma))
        assertEquals(tv, sonosPlaybackRoom(tv, state))
        val profile = app.stova.profiles.Profile("a", "A", app.stova.core.theme.AvatarColor.LIME)
        assertNull(sonosHomeRoom(SonosPrefs(enabled = true, homeArtwork = true), profile, state.rooms, state.musicAssistant))
    }
    @Test fun removingALinkImmediatelyPreventsCommandsFromTheOldLibrary() = runBlocking {
        val fake = FakeHaConnection()
        fake.entities.value = listOf(player("media_player.sonos"), player("media_player.ma", ""","active_queue":"q"""")).associateBy { it.id }
        fake.registryValue = HaRegistry(mapOf("media_player.sonos" to HaEntityInfo(platform = "sonos"), "media_player.ma" to HaEntityInfo(platform = "music_assistant")))
        var links = mapOf("media_player.sonos" to "media_player.ma")
        val repo = SonosRepository(fake, musicAssistantLinks = { links }) { true }
        repo.observe().first { it.rooms.isNotEmpty() }
        links = emptyMap()
        assertFalse(repo.perform("media_player.ma", SonosAction.Next))
        assertEquals(0, fake.calls)
    }

    // Found on the Shield: the registry had no names, so the linking dialog showed raw entity ids.
    private fun named(id: String, friendly: String?) = HaEntity(id, "idle", StovaJson.parseToJsonElement(
        if (friendly == null) "{}" else """{"friendly_name":"$friendly"}""").jsonObject, 0, 0)

    @Test fun linkingNamesUseFriendlyNamesWhenTheRegistryHasNone() {
        val registry = HaRegistry(mapOf(
            "media_player.kjokken1" to HaEntityInfo(platform = "sonos"),
            "media_player.sonos_lydplanke" to HaEntityInfo(platform = "sonos", name = "Stova"),
            "media_player.lydplanke_2" to HaEntityInfo(platform = "music_assistant"),
            "media_player.gone" to HaEntityInfo(platform = "music_assistant", originalName = "Gamal")))
        val states = listOf(named("media_player.kjokken1", "Kjøkken"), named("media_player.sonos_lydplanke", "Sonos Lydplanke"),
            named("media_player.lydplanke_2", "Lydplanke")).associateBy { it.id }
        val names = musicAssistantNames(registry, states, registry.entities.keys)
        assertEquals("Kjøkken", names["media_player.kjokken1"])
        // A name the person gave in HA still wins over the device's own name.
        assertEquals("Stova", names["media_player.sonos_lydplanke"])
        assertEquals("Lydplanke", names["media_player.lydplanke_2"])
        assertEquals("Gamal", names["media_player.gone"])
        assertEquals("media_player.unknown", musicAssistantNames(HaRegistry(), emptyMap(), listOf("media_player.unknown"))["media_player.unknown"])
    }

    @Test fun sameFriendlyNamesStayDistinguishableByModelThenId() {
        val registry = HaRegistry(mapOf(
            "media_player.a" to HaEntityInfo(platform = "sonos", model = "Play:1"),
            "media_player.b" to HaEntityInfo(platform = "sonos", model = "Era 100"),
            "media_player.ma1" to HaEntityInfo(platform = "music_assistant"),
            "media_player.ma2" to HaEntityInfo(platform = "music_assistant")))
        val states = listOf(named("media_player.a", "Kjøkken"), named("media_player.b", "Kjøkken"),
            named("media_player.ma1", "Kjøkken"), named("media_player.ma2", "Kjøkken")).associateBy { it.id }
        val names = musicAssistantNames(registry, states, registry.entities.keys)
        assertEquals("Kjøkken · Play:1", names["media_player.a"])
        assertEquals("Kjøkken · Era 100", names["media_player.b"])
        assertEquals("Kjøkken · media_player.ma1", names["media_player.ma1"])
        assertEquals("Kjøkken · media_player.ma2", names["media_player.ma2"])
    }

    @Test fun aSavedLinkIsKeptAndShownByNameEvenBeforeStatesArrive() {
        val registry = HaRegistry(mapOf("media_player.sonos" to HaEntityInfo(platform = "sonos"),
            "media_player.ma" to HaEntityInfo(platform = "music_assistant")))
        val links = mapOf("media_player.sonos" to "media_player.ma")
        assertEquals("media_player.ma", musicAssistantNames(registry, emptyMap(), registry.entities.keys)[links["media_player.sonos"]])
        assertEquals("Stova MA", musicAssistantNames(registry, mapOf("media_player.ma" to named("media_player.ma", "Stova MA")),
            registry.entities.keys)[links["media_player.sonos"]])
    }
}
