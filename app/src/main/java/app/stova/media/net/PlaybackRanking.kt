package app.stova.media.net

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.*

internal data class PlaybackPage(val items: List<JsonObject>, val total: Int?)

internal fun playbackPage(body: String): PlaybackPage {
    val root = Json.parseToJsonElement(body)
    val items = root as? JsonArray ?: (root as? JsonObject)?.field("Items") as? JsonArray
        ?: error("Missing playback items")
    return PlaybackPage(items.map { it.jsonObject }, (root as? JsonObject)?.text("TotalRecordCount")?.toIntOrNull())
}

/** No series-level PlayCount: servers disagree on its meaning. Sum the user's episode counts instead. */
internal class PlaybackRanking {
    private data class Score(var count: Long = 0, var lastPlayed: Instant = Instant.MIN)
    private val seen = mutableSetOf<String>()
    private val scores = mutableMapOf<String, Score>()

    fun add(items: List<JsonObject>): Boolean {
        var advanced = false
        for (item in items) {
            val id = item.text("Id") ?: continue
            if (!seen.add(id)) continue
            advanced = true
            val key = when (item.text("Type")?.lowercase()) {
                "movie" -> id
                "episode" -> item.text("SeriesId") ?: continue
                else -> continue
            }
            val user = item.obj("UserData")
            val count = user.text("PlayCount")?.toLongOrNull()?.takeIf { it > 0 } ?: continue
            val score = scores.getOrPut(key) { Score() }
            score.count += count
            val lastPlayed = user.text("LastPlayedDate")?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
            if (lastPlayed != null && lastPlayed > score.lastPlayed) score.lastPlayed = lastPlayed
        }
        return advanced
    }

    fun topIds(limit: Int): List<String> = scores.entries
        .sortedWith(compareByDescending<Map.Entry<String, Score>> { it.value.count }
            .thenByDescending { it.value.lastPlayed }.thenBy { it.key })
        .take(limit).map { it.key }
}
