package app.stova.sonos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import app.stova.core.storage.StovaJson
import app.stova.core.theme.*
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*

/** Emulator-only, isolated artwork and controls: no container, disk writes, account or service. */
class SonosPreviewActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) {
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocales(android.os.LocaleList(java.util.Locale.forLanguageTag("nn")))
        super.attachBaseContext(base.createConfigurationContext(config))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
        setContent { StovaTheme(reduceMotion = intent.getBooleanExtra("reduceMotion", false)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,
                if (intent.getBooleanExtra("largeFont", false)) 2f else 1f)) {
                var rooms by remember { mutableStateOf(sonosPreviewRooms()) }
                var selected by remember { mutableStateOf(rooms.first().id) }
                var group by remember { mutableStateOf(false) }
                var immersive by remember { mutableStateOf(intent.getBooleanExtra("immersive", false)) }
                val mode = intent.getStringExtra("mode")
                val shown = if (mode == "empty" || mode == "error") emptyList() else rooms
                val state = SonosState(shown, loading = false, failed = mode == "error")
                SonosContent(state, selected, { selected = it }, { action ->
                    rooms = rooms.map { room -> if (room.id != selected) room else when (action) {
                        SonosAction.PlayPause -> room.copy(entity = room.entity.copy(state = if (room.playing) "paused" else "playing"))
                        is SonosAction.Volume -> room.copy(entity = room.entity.copy(attributes = JsonObject(room.entity.attributes + ("volume_level" to JsonPrimitive(action.percent / 100.0)))))
                        SonosAction.Next, SonosAction.Previous -> {
                            val alternate = room.artwork == "preview-art"
                            room.copy(entity = room.entity.copy(attributes = JsonObject(room.entity.attributes + mapOf(
                                "entity_picture" to JsonPrimitive(if (alternate) "preview-art-next" else "preview-art"),
                                "media_title" to JsonPrimitive(if (alternate) "Under a Copper Sky" else "A Quiet Kind of Blue"),
                            ))))
                        }
                        else -> room
                    } }
                }, { group = true }, {}, {}, onBack = { finish() }, immersive = immersive, onImmersive = { immersive = it },
                    loadArtwork = { path, modifier, ready, _ ->
                        LaunchedEffect(path) {
                            if (path == "preview-art-next") kotlinx.coroutines.delay(700)
                            ready()
                        }
                        SonosPreviewArtwork(modifier, alternate = path == "preview-art-next")
                    })
                if (group) {
                    val anchor = coordinator(rooms.first { it.id == selected }, rooms)!!
                    SonosGroupDialog(anchor, rooms, false, false, { members ->
                        val groupIds = listOf(anchor.id) + (members - anchor.id).sorted()
                        rooms = rooms.map { room -> room.copy(entity = room.entity.copy(attributes = JsonObject(room.entity.attributes +
                            ("group_members" to JsonArray((if (room.id in members) groupIds else listOf(room.id)).map(::JsonPrimitive)))))) }
                        group = false
                    }, { group = false })
                }
            }
        } }
    }
}

/** Models as HA's device registry reports them; the last room has none, so it gets the neutral speaker. */
internal fun sonosPreviewRooms(): List<SonosRoom> = listOf("Stova" to "Arc", "Kjøkken" to "Era 100", "Soverom" to "One SL",
    "Terrasse" to "Move 2", "Kontor" to null).mapIndexed { index, (name, model) ->
    SonosRoom(HaEntity("media_player.room$index", if (index < 2) "playing" else "paused", StovaJson.parseToJsonElement("""{
      "media_title":"A Quiet Kind of Blue", "media_artist":"Evening Orchestra", "media_album_name":"Between the hills",
      "entity_picture":"preview-art", "volume_level":0.24, "media_position":112, "media_duration":284,
      "supported_features":3145727, "source":"Music", "source_list":["TV","Line-in"],
      "group_members":["media_player.room$index"]
    }""").jsonObject, 0, 0), name, model = model, manufacturer = model?.let { "Sonos" })
}

/** A fixed illustrative album sleeve for screenshots, using the existing theme's paint colours. */
@Composable internal fun SonosPreviewArtwork(modifier: Modifier, alternate: Boolean = false) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(if (alternate) StovaColors.Caution else StovaColors.Mist, StovaColors.Ink)))
        drawCircle(if (alternate) StovaColors.Mist else StovaColors.Caution, size.minDimension * .18f,
            Offset(size.width * if (alternate) .38f else .68f, size.height * .32f))
        val hills = Path().apply {
            moveTo(0f, size.height * .66f)
            cubicTo(size.width * .2f, size.height * .3f, size.width * .4f, size.height * .92f, size.width * .7f, size.height * .58f)
            quadraticTo(size.width * .9f, size.height * .46f, size.width, size.height * .66f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(hills, StovaColors.Surface)
        val ridge = Path().apply {
            moveTo(0f, size.height * .85f)
            quadraticTo(size.width * .5f, size.height * .58f, size.width, size.height * .84f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(ridge, StovaColors.Ink)
    }
}
