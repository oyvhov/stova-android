package app.stova.sonos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SonosLibraryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun backFromAFailedFolderReturnsToThePreviousListing() {
        val fake = FakeHaConnection()
        val room = SonosRoom(HaEntity("media_player.a", "playing", buildJsonObject { put("supported_features", 131072) }, 0, 0), "A")
        fake.entities.value = mapOf(room.id to room.entity)
        fake.registryValue = HaRegistry(mapOf(room.id to HaEntityInfo(platform = "sonos")))
        val connection = object : HaConnection by fake {
            override suspend fun browseMedia(entityId: String, contentType: String?, contentId: String?): JsonObject? =
                if (contentId != null) null else StovaJson.parseToJsonElement("""{"title":"Music","media_content_id":"root","media_content_type":"library","children":[
                    {"title":"Favourites","media_content_id":"favorites","media_content_type":"library","can_expand":true}]}""").jsonObject
        }
        val repository = SonosRepository(connection) { true }
        runBlocking { repository.observe().first { it.rooms.isNotEmpty() } }
        var left = false
        compose.setContent { StovaTheme(reduceMotion = true) { SonosLibrary(repository, room, false, false, {}, { _, _, _, _ -> }) { left = true } } }
        compose.onNodeWithText("Favourites").performClick()
        compose.onNodeWithText("Kunne ikkje hente musikken. Prøv att.").assertIsDisplayed()
        compose.onNodeWithText("Tilbake").performClick()
        assertFalse(left)
        compose.onNodeWithText("Favourites").assertIsDisplayed()
    }

    private fun media(json: String) = sonosMedia(StovaJson.parseToJsonElement(json).jsonObject)

    @Test fun logosAndFolderIconsAreFittedWholeButAlbumArtFillsItsSquare() {
        assertTrue(media("""{"title":"Emby","media_class":"directory","thumbnail":"https://brands.home-assistant.io/_/emby/logo.png"}""").logoArtwork)
        assertTrue(media("""{"title":"Camera","media_class":"directory","thumbnail":"/api/brands/integration/camera/icon.png"}""").logoArtwork)
        assertTrue(media("""{"title":"Radio","media_class":"app","thumbnail":"/api/media_player_proxy/r"}""").logoArtwork)
        // A brand logo stays whole even if a source calls it an album.
        assertTrue(media("""{"title":"X","media_class":"album","thumbnail":"https://brands.home-assistant.io/_/x/logo.png"}""").logoArtwork)
        assertFalse(media("""{"title":"Blue","media_class":"album","thumbnail":"/api/media_player_proxy/a"}""").logoArtwork)
        assertFalse(media("""{"title":"Mix","media_class":"playlist","thumbnail":"/api/media_player_proxy/p"}""").logoArtwork)
        assertEquals("album", media("""{"title":"Blue","media_class":"album"}""").mediaClass)
    }

    private fun library(json: String, loaded: Set<String>, seen: MutableMap<String, Boolean>) {
        val fake = FakeHaConnection()
        val room = SonosRoom(HaEntity("media_player.a", "playing", buildJsonObject { put("supported_features", 131072) }, 0, 0), "A")
        fake.entities.value = mapOf(room.id to room.entity)
        fake.registryValue = HaRegistry(mapOf(room.id to HaEntityInfo(platform = "sonos")))
        val connection = object : HaConnection by fake {
            override suspend fun browseMedia(entityId: String, contentType: String?, contentId: String?): JsonObject? =
                StovaJson.parseToJsonElement(json).jsonObject
        }
        val repository = SonosRepository(connection) { true }
        runBlocking { repository.observe().first { it.rooms.isNotEmpty() } }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosLibrary(repository, room, false, false, {}, { path, _, fit, onLoad ->
                seen[path] = fit
                if (path in loaded) androidx.compose.runtime.LaunchedEffect(path) { onLoad() }
            }) {}
        } }
        compose.waitForIdle()
    }

    @Test fun aLoadedPictureHidesTheFallbackButAMissingOrFailedOneKeepsIt() {
        val seen = mutableMapOf<String, Boolean>()
        library("""{"title":"Music","media_content_id":"root","media_content_type":"library","children":[
            {"title":"Emby","media_class":"directory","media_content_id":"e","media_content_type":"app","can_expand":true,"thumbnail":"/logo.png"},
            {"title":"Blue","media_class":"album","media_content_id":"b","media_content_type":"album","can_play":true,"thumbnail":"/album.jpg"},
            {"title":"Empty","media_class":"directory","media_content_id":"n","media_content_type":"app","can_expand":true}]}""",
            loaded = setOf("/logo.png"), seen = seen)
        assertEquals(mapOf("/logo.png" to true, "/album.jpg" to false), seen)
        compose.onNodeWithTag("sonos-library-fallback-0", useUnmergedTree = true).assertDoesNotExist()
        // Not (yet) loaded, or failed: the neutral mark stays, so no tile is ever blank.
        compose.onNodeWithTag("sonos-library-fallback-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("sonos-library-fallback-2", useUnmergedTree = true).assertExists()
    }
}
