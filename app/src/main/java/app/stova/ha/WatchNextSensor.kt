package app.stova.ha

import app.stova.watchnext.WatchNextEntry
import app.stova.watchnext.WatchNextResult
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** The entity the wall dashboard reads. Its id, name and attributes are a contract: do not rename them. */
const val WATCH_NEXT_ENTITY = "sensor.stova_watch_next"
private const val WATCH_NEXT_PACKAGES = 12

/**
 * The `POST /api/states` body for [entries]: the newest title as the state (Home Assistant keeps 255
 * characters, `none` for an empty list), the newest entry as `latest`, and the newest of each app in
 * `by_package`, at most twelve apps.
 */
internal fun watchNextSensorBody(entries: List<WatchNextEntry>): JsonObject {
    val newest = entries.sortedByDescending { it.lastEngagement }
    val latest = newest.firstOrNull()
    return buildJsonObject {
        put("state", latest?.title?.let { it.take(if (it.length > 255 && it[254].isHighSurrogate()) 254 else 255) } ?: "none")
        putJsonObject("attributes") {
            put("friendly_name", "Shield · Fortsett å sjå")
            put("icon", "mdi:television-play")
            put("latest", latest?.let(::watchNextJson) ?: JsonNull)
            putJsonObject("by_package") {
                newest.distinctBy { it.packageName }.take(WATCH_NEXT_PACKAGES).forEach { put(it.packageName, watchNextJson(it)) }
            }
        }
    }
}

private fun watchNextJson(entry: WatchNextEntry) = buildJsonObject {
    put("package", entry.packageName)
    put("title", entry.title)
    put("episode", entry.subtitle)
    // A content:// poster is only readable on the TV itself.
    put("poster", entry.posterUri?.takeIf { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) })
    put("progress", entry.progress?.let { (it * 10_000).roundToInt() / 10_000.0 })
    put("position_ms", entry.positionMs)
    put("duration_ms", entry.durationMs)
    put("engaged_at", entry.lastEngagement.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() })
}

/**
 * Keeps [WATCH_NEXT_ENTITY] in Home Assistant in step with Android TV's Watch Next list, so the wall dashboard
 * can show what is watched in NRK TV and other apps. It runs for as long as the process does, also while
 * another app is in front, and posts through the REST API rather than waking the socket.
 *
 * [address] is null while Home Assistant is not set up or signed out: then neither Watch Next nor Home
 * Assistant is asked anything. Otherwise the list is posted ~2 s after it settles, once for each address
 * and each [reconnected] (a state posted this way is gone after Home Assistant restarts), and never twice
 * in a row unchanged. A failed post is dropped quietly and goes again with the next change.
 */
class WatchNextSensor(
    private val http: OkHttpClient,
    private val address: Flow<HaAddress?>,
    private val token: suspend () -> String,
    private val watchNext: Flow<WatchNextResult>,
    private val reconnected: Flow<Unit>,
    private val debounceMillis: Long = 2_000,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    @OptIn(FlowPreview::class)
    suspend fun run() = address.distinctUntilChanged().collectLatest { at ->
        if (at == null) return@collectLatest
        val bodies = watchNext.mapNotNull { (it as? WatchNextResult.Available)?.entries }
            .map(::watchNextSensorBody).distinctUntilChanged().debounce(debounceMillis)
        var posted: Pair<Int, JsonObject>? = null
        combine(reconnected.runningFold(0) { round, _ -> round + 1 }, bodies) { round, body -> round to body }.collect { attempt ->
            if (attempt != posted && post(at, attempt.second)) posted = attempt
        }
    }

    private suspend fun post(at: HaAddress, body: JsonObject): Boolean = try {
        val request = Request.Builder().url("${at.base}/api/states/$WATCH_NEXT_ENTITY")
            .header("Authorization", "Bearer ${token()}")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        withContext(io) { http.newCall(request).execute().use { it.isSuccessful } }
    } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
}
