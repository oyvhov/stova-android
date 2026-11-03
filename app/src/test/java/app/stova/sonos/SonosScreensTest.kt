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
