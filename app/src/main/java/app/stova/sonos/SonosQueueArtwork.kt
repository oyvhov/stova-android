package app.stova.sonos

import app.stova.ha.text
import kotlinx.serialization.json.JsonObject

internal data class SonosQueueTarget(val player: String, val queue: String, val track: String)

internal data class SonosQueuedArtwork(val target: SonosQueueTarget, val path: String) {
    fun matches(current: SonosQueueTarget?) = target == current
}

internal fun sonosQueueTarget(room: SonosRoom, state: SonosState, requirePlaying: Boolean = true): SonosQueueTarget? {
    val native = coordinator(room, state.rooms) ?: return null
    if (!room.available || room.tvAudio || !native.available || native.tvAudio) return null
    val player = sonosPlaybackRoom(room, state) ?: return null
    if (player.id == native.id || player.tvAudio || (requirePlaying && !player.playing)) return null
    val queue = player.entity.attributes.text("active_queue")?.takeIf(String::isNotBlank) ?: return null
    val track = player.entity.attributes.text("media_content_id")?.takeIf(String::isNotBlank) ?: return null
    return SonosQueueTarget(player.id, queue, track)
}

internal fun nextSonosArtwork(target: SonosQueueTarget, response: JsonObject): SonosQueuedArtwork? {
    val queue = response[target.player] as? JsonObject ?: return null
    if (queue.text("queue_id") != target.queue) return null
    fun item(key: String) = (queue[key] as? JsonObject)?.get("media_item") as? JsonObject
    if (item("current_item")?.text("uri") != target.track) return null
    val next = item("next_item") ?: return null
    val track = next.text("uri")?.takeIf { it.isNotBlank() && it != target.track } ?: return null
    val path = next.text("image")?.takeIf(String::isNotBlank) ?: return null
    return SonosQueuedArtwork(target.copy(track = track), path)
}
