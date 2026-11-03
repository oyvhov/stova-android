package app.stova.sonos

import app.stova.ha.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

@Serializable data class SonosPrefs(val enabled: Boolean = false, val showHomeButton: Boolean = true, val backgroundArt: Boolean = true,
    val homeArtwork: Boolean = false, val preferredRoom: String? = null, val showNowPlaying: Boolean = true,
    val musicAssistant: Boolean = false, val musicAssistantPlayers: Map<String, String> = emptyMap())

/** [model] and [manufacturer] come from the HA device registry, never from the room's name. */
data class SonosRoom(val entity: HaEntity, val name: String, val area: String? = null,
    val model: String? = null, val manufacturer: String? = null) {
    val id get() = entity.id
    val speaker get() = sonosSpeaker(model, manufacturer)
    val available get() = entity.state !in setOf("unavailable", "unknown", "")
    val playing get() = entity.state == "playing"
    val title get() = entity.attributes.text("media_title")
    val artist get() = entity.attributes.text("media_artist")
    val album get() = entity.attributes.text("media_album_name")
    val artwork get() = entity.attributes.text("entity_picture_local") ?: entity.attributes.text("entity_picture")
    val volume get() = entity.attributes.number("volume_level")?.times(100)?.roundToInt()?.coerceIn(0, 100)
    val muted get() = (entity.attributes["is_volume_muted"] as? JsonPrimitive)?.booleanOrNull == true
    val members get() = entity.attributes.stringList("group_members").ifEmpty { listOf(id) }
    val coordinatorId get() = members.first()
    val source get() = entity.attributes.text("source")
    /** TV can retain the previous album's picture; never infer music from artwork alone. */
    val tvAudio get() = source?.trim().equals("TV", ignoreCase = true) ||
        entity.attributes.text("media_content_id")?.startsWith("x-sonos-htastream:", ignoreCase = true) == true
    val sources get() = entity.attributes.stringList("source_list")
    val duration get() = entity.attributes.number("media_duration")?.toInt()?.coerceAtLeast(0) ?: 0
    val shuffle get() = (entity.attributes["shuffle"] as? JsonPrimitive)?.booleanOrNull == true
    val repeat get() = entity.attributes.text("repeat") ?: "off"
    fun supports(bit: Int) = ((entity.attributes["supported_features"] as? JsonPrimitive)?.intOrNull ?: 0) and bit != 0
    fun position(now: Long): Int {
        val position = entity.attributes.number("media_position") ?: 0.0
        val updated = entity.attributes.text("media_position_updated_at")?.let { runCatching { haMillis(it) }.getOrNull() }
        val elapsed = if (playing && updated != null) ((now.toDouble() - updated) / 1000).coerceAtLeast(0.0) else 0.0
        return (position + elapsed).coerceIn(0.0, duration.toDouble()).toInt()
    }
}
private fun JsonObject.number(key: String) = (get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite)
private fun JsonObject.stringList(key: String) = (get(key) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

fun sonosRooms(states: List<HaEntity>, registry: HaRegistry): List<SonosRoom> = states.mapNotNull { entity ->
    val info = registry.entities[entity.id] ?: return@mapNotNull null
    if (info.platform != "sonos" || info.disabled || !entity.id.startsWith("media_player.")) return@mapNotNull null
    SonosRoom(entity, info.name?.takeIf(String::isNotBlank) ?: entity.attributes.text("friendly_name") ?: info.originalName ?: entity.id, info.room,
        info.model, info.manufacturer)
}.sortedBy { it.name.lowercase() }

fun coordinator(room: SonosRoom, rooms: List<SonosRoom>) = rooms.find { it.id == room.coordinatorId }
data class SonosGroupPlan(val coordinator: String, val join: List<String>, val leave: List<String>)
data class SonosGroupDraft(val anchorId: String, val initial: Set<String>, val selected: Set<String> = initial) {
    fun toggle(id: String) = if (id == anchorId) this else
        copy(selected = if (id in selected) selected - id else selected + id)

    fun canApply(anchor: SonosRoom, rooms: List<SonosRoom>) =
        anchor.id == anchorId && anchor.members.toSet() == initial && selected != initial && groupPlan(anchor, rooms, selected) != null
}

fun groupPlan(anchor: SonosRoom, rooms: List<SonosRoom>, selected: Set<String>): SonosGroupPlan? {
    if (anchor.id != anchor.coordinatorId || anchor.id !in selected || !anchor.available) return null
    val known = rooms.filter { it.available }.map { it.id }.toSet()
    if (!known.containsAll(selected)) return null
    val current = anchor.members.toSet()
    val leave = (current - selected).toList()
    if (!known.containsAll(leave)) return null
    return SonosGroupPlan(anchor.id, (selected - current).sorted(), leave.sorted())
}

data class SonosState(val rooms: List<SonosRoom> = emptyList(), val loading: Boolean = true, val failed: Boolean = false,
    val musicAssistant: Map<String, SonosRoom> = emptyMap())

/** Only an explicitly linked MA queue controls playback. Native TV audio always wins. */
fun sonosPlaybackRoom(room: SonosRoom, state: SonosState): SonosRoom? {
    val native = coordinator(room, state.rooms) ?: return null
    if (room.tvAudio || native.tvAudio || !room.available) return native
    return state.musicAssistant[native.id]?.takeIf { it.available && !it.entity.attributes.text("active_queue").isNullOrBlank() } ?: native
}
data class SonosMedia(val id: String, val type: String, val title: String, val artwork: String? = null,
    val playable: Boolean = false, val expandable: Boolean = false, val children: List<SonosMedia> = emptyList(),
    val mediaClass: String = "") {
    /** Brand logos, source and folder icons are often transparent: show them whole, never cropped. */
    val logoArtwork get() = artwork != null && (artwork.contains("brands.home-assistant.io") || artwork.contains("/api/brands/") ||
        mediaClass !in pictureClasses)
}
/** Classes whose thumbnail is a picture (a sleeve, a photo, a still) that may fill its square. */
private val pictureClasses = setOf("album", "artist", "playlist", "track", "podcast", "episode", "movie", "tv_show", "season", "video", "music")
fun sonosMedia(json: JsonObject, depth: Int = 0): SonosMedia = SonosMedia(
    json.text("media_content_id").orEmpty(), json.text("media_content_type").orEmpty(), json.text("title").orEmpty(), json.text("thumbnail"),
    (json["can_play"] as? JsonPrimitive)?.booleanOrNull == true, (json["can_expand"] as? JsonPrimitive)?.booleanOrNull == true,
    if (depth == 0) (json["children"] as? JsonArray)?.filterIsInstance<JsonObject>()?.map { sonosMedia(it, 1) }.orEmpty() else emptyList(),
    json.text("media_class").orEmpty())

sealed interface SonosAction {
    data object PlayPause : SonosAction
    data object Previous : SonosAction
    data object Next : SonosAction
    data class Volume(val percent: Int) : SonosAction
    data class Mute(val muted: Boolean) : SonosAction
    data class Seek(val seconds: Int) : SonosAction
    data class Source(val name: String) : SonosAction
    data class Shuffle(val enabled: Boolean) : SonosAction
    data class Repeat(val mode: String) : SonosAction
    data class PlayMedia(val media: SonosMedia, val enqueue: Boolean = false) : SonosAction
}
