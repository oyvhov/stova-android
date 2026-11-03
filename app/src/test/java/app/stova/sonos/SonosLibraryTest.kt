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
        compose.setContent { StovaTheme(reduceMotion = true) { SonosLibrary(repository, room, false, false, {}, { _, _ -> }) { left = true } } }
        compose.onNodeWithText("Favourites").performClick()
        compose.onNodeWithText("Kunne ikkje hente musikken. Prøv att.").assertIsDisplayed()
        compose.onNodeWithText("Tilbake").performClick()
        assertFalse(left)
        compose.onNodeWithText("Favourites").assertIsDisplayed()
    }
}
