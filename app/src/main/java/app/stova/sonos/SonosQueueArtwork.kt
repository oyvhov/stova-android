package app.stova.sonos

import app.stova.ha.text
import app.stova.ha.cards.number
import kotlinx.serialization.json.JsonObject

internal data class SonosQueueTarget(val player: String, val queue: String, val track: String, val position: Int? = null)

internal data class SonosQueuedArtwork(val target: SonosQueueTarget, val path: String) {
    fun matches(current: SonosQueueTarget?) = target == current
}

internal fun sonosQueueTarget(room: SonosRoom, state: SonosState, requirePlaying: Boolean = true): SonosQueueTarget? {
    val native = coordinator(room, state.rooms) ?: return null
    if (!room.available || room.tvAudio || !native.available || native.tvAudio) return null
    val player = sonosPlaybackRoom(room, state) ?: return null
    if (player.tvAudio || !player.available || (requirePlaying && !player.playing)) return null
    val track = player.entity.attributes.text("media_content_id")?.takeIf(String::isNotBlank) ?: return null
    if (player.id == native.id) {
        if (!native.supports(131072) || native.shuffle || native.repeat == "one") return null
        val position = native.entity.attributes.number("queue_position")?.toInt()?.takeIf { it > 0 } ?: return null
        return SonosQueueTarget(native.id, "Q:0", track, position)
    }
    val queue = player.entity.attributes.text("active_queue")?.takeIf(String::isNotBlank) ?: return null
    return SonosQueueTarget(player.id, queue, track)
}

/** Sonos reports a one-based queue position. A stale or reordered queue must never predict a cover. */
internal fun nextNativeSonosArtwork(target: SonosQueueTarget, queue: SonosMedia, repeat: Boolean): SonosQueuedArtwork? {
    val index = target.position?.minus(1)?.takeIf { it >= 0 } ?: return null
    if (queue.id != target.queue || queue.children.getOrNull(index)?.id != target.track) return null
    val nextIndex = if (index == queue.children.lastIndex && repeat) 0 else index + 1
    val next = queue.children.getOrNull(nextIndex) ?: return null
    if (next.id.isBlank() || next.artwork.isNullOrBlank()) return null
    return SonosQueuedArtwork(target.copy(track = next.id, position = nextIndex + 1), next.artwork)
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
