package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

/**
 * Home Assistant's Sonos browser (homeassistant/components/sonos/media_browser.py) answers the
 * Favorites node *and* every favourites folder with `media_content_id=""` and
 * `media_content_type="favorites"`. A folder that comes back with its parent's identity is still a
 * new level: Back must go up one level at a time.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SonosLibraryNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val root = """{"title":"Sonos","media_content_id":"","media_content_type":"root","can_expand":true,"children":[
        {"title":"Favorites","media_content_id":"","media_content_type":"favorites","can_expand":true}]}"""
    private val favorites = """{"title":"Favorites","media_content_id":"","media_content_type":"favorites","can_expand":true,"children":[
        {"title":"Tracks","media_content_id":"object.item.audioItem.musicTrack","media_content_type":"favorites_folder","can_expand":true},
        {"title":"Playlists","media_content_id":"object.container.playlistContainer","media_content_type":"favorites_folder","can_expand":true}]}"""
    private fun folder(title: String, item: String) = """{"title":"$title","media_content_id":"","media_content_type":"favorites","can_expand":true,"children":[
        {"title":"$item","media_content_id":"FV:2/$item","media_content_type":"favorite_item_id","can_play":true}]}"""

    private var failNextFolder = false
    private var leftLibrary = 0
    private val played = mutableListOf<SonosAction>()

    private fun open() {
        val fake = FakeHaConnection()
        val room = SonosRoom(HaEntity("media_player.a", "playing", buildJsonObject { put("supported_features", 131072 or 512) }, 0, 0), "A")
        fake.entities.value = mapOf(room.id to room.entity)
        fake.registryValue = HaRegistry(mapOf(room.id to HaEntityInfo(platform = "sonos")))
        val connection = object : HaConnection by fake {
            override suspend fun browseMedia(entityId: String, contentType: String?, contentId: String?): JsonObject? {
                val json = when (contentType) {
                    null -> root
                    "favorites" -> favorites
                    "favorites_folder" -> if (failNextFolder) { failNextFolder = false; return null }
                        else if (contentId == "object.item.audioItem.musicTrack") folder("Tracks", "Northern Lights") else folder("Playlists", "2023")
                    else -> return null
                }
                return StovaJson.parseToJsonElement(json).jsonObject
            }
        }
        val repository = SonosRepository(connection) { true }
        runBlocking { repository.observe().first { it.rooms.isNotEmpty() } }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosLibrary(repository, room, false, false, { played += it }, { _, _, _, _ -> }) { leftLibrary++ }
        } }
    }
    private fun systemBack() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun showingFavorites() {
        compose.onNodeWithText("Tracks").assertIsDisplayed()
        compose.onNodeWithText("Playlists").assertIsDisplayed()
    }

    @Test fun backKeyLeavesAFavouritesFolderOneLevelAtATime() {
        open()
        compose.onNodeWithText("Favorites").performClick()
        compose.onNodeWithText("Tracks").performClick()
        compose.onNodeWithText("Northern Lights").assertIsDisplayed()
        systemBack()
        showingFavorites()
        systemBack()
        compose.onNodeWithText("Favorites").assertIsDisplayed()
        compose.onNodeWithText("Tracks").assertDoesNotExist()
        assertEquals(0, leftLibrary)
        systemBack()
        assertEquals(1, leftLibrary)
    }

    @Test fun headerBackAfterPlayingFromAPlaylistFolderReturnsToFavourites() {
        open()
        compose.onNodeWithText("Favorites").performClick()
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText("2023").performClick()
        compose.onNodeWithText("Spel").performClick()
        assertEquals("FV:2/2023", (played.single() as SonosAction.PlayMedia).media.id)
        compose.onNodeWithText("Tilbake").performClick()
        showingFavorites()
        assertEquals(0, leftLibrary)
    }

    @Test fun aRetriedFolderIsAddedOnTopWithoutReplacingItsParents() {
        open()
        compose.onNodeWithText("Favorites").performClick()
        failNextFolder = true
        compose.onNodeWithText("Tracks").performClick()
        compose.onNodeWithText("Kunne ikkje hente musikken. Prøv att.").assertIsDisplayed()
        compose.onNodeWithText("Prøv igjen").performClick()
        compose.onNodeWithText("Northern Lights").assertIsDisplayed()
        systemBack()
        showingFavorites()
        systemBack()
        compose.onNodeWithText("Favorites").assertIsDisplayed()
        assertEquals(0, leftLibrary)
    }
}
