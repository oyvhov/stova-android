package app.stova.sonos

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.*
import app.stova.core.theme.StovaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun enabled() = assumeScreensEnabled()
    private fun render(scale: Float = 1f, group: Boolean = false, empty: Boolean = false) {
        val rooms = if (empty) emptyList() else sonosPreviewRooms()
        compose.setContent { StovaTheme(reduceMotion = true) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                SonosContent(SonosState(rooms, loading = false), rooms.firstOrNull()?.id, {}, {}, {}, {}, {},
                    artwork = { _, modifier -> SonosPreviewArtwork(modifier) }, onBrowseMusicAssistant = {})
                if (group) SonosGroupDialog(rooms.first(), rooms, false, false, {}, {})
            }
        } }
        if (group) compose.saveScreen("sonos-group", org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView)
        else compose.saveScreen(if (empty) "sonos-empty" else if (scale > 1f) "sonos-large" else "sonos-playing")
    }
    @Test fun groupedRooms() {
        val ids = listOf("media_player.room0", "media_player.room1")
        val members = kotlinx.serialization.json.JsonArray(ids.map { kotlinx.serialization.json.JsonPrimitive(it) })
        val rooms = sonosPreviewRooms().map { room -> if (room.id !in ids) room else room.copy(entity = room.entity.copy(
            attributes = kotlinx.serialization.json.JsonObject(room.entity.attributes + ("group_members" to members)))) }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosContent(SonosState(rooms, loading = false), rooms[1].id, {}, {}, {}, {}, {},
                artwork = { _, modifier -> SonosPreviewArtwork(modifier) })
        } }
        compose.saveScreen("sonos-grouped")
    }
    /** Every drawn form, lit as if playing, so the shapes can be judged side by side. */
    @Test fun speakerGallery() {
        val models = listOf("One SL", "Era 100", "Era 300", "Five", "Move 2", "Roam", "Beam (Gen 2)", "Arc Ultra",
            "Ray", "Playbar", "Playbase", "Sub (Gen 3)", "Sub Mini", "Amp", null)
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.foundation.layout.FlowRow(androidx.compose.ui.Modifier.fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(48.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                models.forEachIndexed { index, model ->
                    val speaker = sonosSpeaker(model, "Sonos")
                    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.width(150.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        SonosSpeakerArt(speaker, SonosRoomVisual(speaker, playing = index % 4 != 3, active = false),
                            androidx.compose.ui.Modifier.fillMaxWidth().height(104.dp), "gallery-$index", waves = true)
                        androidx.compose.material3.Text(speaker.label ?: "–", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } }
        compose.saveScreen("sonos-speakers")
    }
    /** Transparent logos sit whole on the plate; sleeves fill; a missing picture keeps the quiet mark. */
    @Test fun library() {
        val fake = app.stova.ha.FakeHaConnection()
        val room = sonosPreviewRooms().first()
        fake.entities.value = mapOf(room.id to room.entity)
        fake.registryValue = app.stova.ha.HaRegistry(mapOf(room.id to app.stova.ha.HaEntityInfo(platform = "sonos")))
        val connection = object : app.stova.ha.HaConnection by fake {
            override suspend fun browseMedia(entityId: String, contentType: String?, contentId: String?) =
                app.stova.core.storage.StovaJson.parseToJsonElement("""{"title":"Sonos","media_content_id":"root","media_content_type":"library","children":[
                {"title":"Favorites","media_class":"directory","media_content_id":"f","media_content_type":"favorites","can_expand":true,"thumbnail":"logo:rows"},
                {"title":"Radio","media_class":"app","media_content_id":"r","media_content_type":"app","can_expand":true,"thumbnail":"logo:play"},
                {"title":"Emby/Jellyfin","media_class":"directory","media_content_id":"e","media_content_type":"app","can_expand":true,"thumbnail":"https://brands.home-assistant.io/_/emby/logo.png"},
                {"title":"A Quiet Kind of Blue","media_class":"album","media_content_id":"a","media_content_type":"album","can_play":true,"thumbnail":"album"},
                {"title":"Line-in","media_class":"directory","media_content_id":"l","media_content_type":"app","can_expand":true}]}""").let {
                    (it as kotlinx.serialization.json.JsonObject) }
        }
        val repository = SonosRepository(connection) { true }
        kotlinx.coroutines.runBlocking { repository.observe().first { it.rooms.isNotEmpty() } }
        compose.setContent { StovaTheme(reduceMotion = true) {
            SonosLibrary(repository, room, false, false, {}, { path, modifier, fit, onLoad ->
                LaunchedEffect(path) { onLoad() }
                if (!fit) SonosPreviewArtwork(modifier)
                else androidx.compose.material3.Icon(when (path) { "logo:rows" -> app.stova.core.ui.StovaIcons.Rows; "logo:play" -> app.stova.core.ui.StovaIcons.Play
                    else -> app.stova.core.ui.StovaIcons.Link }, null, modifier, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            }) {}
        } }
        compose.saveScreen("sonos-library")
    }
    @Test fun nowPlaying() = render()
    @Test fun largeFont() = render(2f)
    @Test fun groupRooms() = render(group = true)
    @Test fun emptyRooms() = render(empty = true)
    @Test fun homeWidget() {
        val room = sonosPreviewRooms().first()
        compose.setContent { StovaTheme(reduceMotion = true) {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                app.stova.navigation.TopBar(app.stova.profiles.Profile("p", "Øyvind", app.stova.core.theme.AvatarColor.LIME), {}, {},
                    onSonos = {}, nowPlaying = { modifier ->
                        SonosNowPlaying(room, room, false, false, {}, {}, modifier, artwork = { _, art -> SonosPreviewArtwork(art) })
                    }, leading = {
                        app.stova.ha.ui.HomeStatusChips(listOf(app.stova.ha.ui.ChipContent(app.stova.ha.cards.CardType.LIGHT, "3 lys"),
                            app.stova.ha.ui.ChipContent(app.stova.ha.cards.CardType.CLIMATE, "21°")))
                    })
            }
        } }
        compose.saveScreen("sonos-home-widget")
    }
}
