package app.stova.watchnext

import android.content.ContentResolver
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

data class WatchNextEntry(val id: Long, val packageName: String, val title: String, val subtitle: String?,
    val posterUri: String?, val intentUri: String, val progress: Float?, val lastEngagement: Long,
    /** Android's content ratings as the app wrote them, such as `com.android.tv/US_MV/US_MV_PG13`; most apps write none. */
    val contentRating: String? = null)
sealed interface WatchNextResult {
    data class Available(val entries: List<WatchNextEntry>) : WatchNextResult
    data class Unavailable(val reason: Reason) : WatchNextResult
    enum class Reason { NO_PERMISSION, BLOCKED }
}
fun mapWatchNext(cursor: Cursor): List<WatchNextEntry> = buildList {
    fun text(column: String): String? = cursor.getColumnIndex(column).takeIf { it >= 0 }?.let { if (cursor.isNull(it)) null else cursor.getString(it) }
    fun number(column: String): Long = text(column)?.toLongOrNull() ?: 0
    while (cursor.moveToNext()) {
        val pkg = text(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME) ?: continue
        val title = text(TvContract.WatchNextPrograms.COLUMN_TITLE) ?: continue
        val intent = text(TvContract.WatchNextPrograms.COLUMN_INTENT_URI) ?: continue
        val position = number(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS)
        val duration = number(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS)
        add(WatchNextEntry(number(BaseColumns._ID), pkg, title,
            text(TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE), text(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI), intent,
            if (position > 0 && duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else null,
            number(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
            text(TvContract.WatchNextPrograms.COLUMN_CONTENT_RATING)))
    }
}

/** Display ages for the rating tokens Android's systems share, as conservative as the hero's: M is 15, MA and R are 18. */
private val ratingTokenAges = mapOf("ALL" to 0, "G" to 0, "Y" to 0, "C" to 0, "P" to 0, "Y7" to 7, "PG" to 10, "PG13" to 13,
    "M" to 15, "AV" to 15, "MA" to 18, "R" to 18, "NC17" to 18, "X" to 18)

/**
 * The age an app's content ratings amount to. Each rating is `domain/system/system_token[/subratings]`, several
 * are separated by commas, and the oldest wins. A token this cannot read makes the whole answer unknown.
 */
fun watchNextAge(contentRating: String?): Int? {
    val ratings = contentRating.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (ratings.isEmpty()) return null
    return ratings.maxOf { rating ->
        val parts = rating.split('/')
        if (parts.size < 3) return null
        val token = parts[2].removePrefix(parts[1] + "_").uppercase(java.util.Locale.ROOT)
        token.toIntOrNull() ?: ratingTokenAges[token] ?: return null
    }
}

/** The profile's age limit, as the hero keeps it: with a limit, a title needs a known rating within it. */
fun withinAge(entries: List<WatchNextEntry>, maxAge: Int?): List<WatchNextEntry> =
    if (maxAge == null) entries else entries.filter { watchNextAge(it.contentRating)?.let { age -> age <= maxAge } == true }
fun filterWatchNext(entries: List<WatchNextEntry>, selfPackage: String, limit: Int = 20): List<WatchNextEntry> =
    entries.filterNot { it.packageName == selfPackage || it.packageName == "app.stova" || it.packageName.startsWith("app.reelstack") }
        .sortedByDescending { it.lastEngagement }
        // The same film showed three times on the Shield (2026-11-01): several apps for one Jellyfin server
        // each put it here. A title is shown once, from the app that saw it last.
        .distinctBy { it.title.trim().lowercase() to it.subtitle?.trim()?.lowercase() }
        .take(limit)

fun launchIntentFor(entry: WatchNextEntry): Intent? = try {
    if (entry.packageName.isBlank()) null else {
        val intent = Intent.parseUri(entry.intentUri, Intent.URI_INTENT_SCHEME)
        if (intent.component?.packageName?.let { it != entry.packageName } == true || intent.selector != null) null
        else intent.setPackage(entry.packageName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
} catch (_: Exception) { null }

class WatchNextReader(private val resolver: ContentResolver, private val hasPermission: () -> Boolean, private val selfPackage: String) {
    private val mutableStatus = MutableStateFlow<WatchNextResult>(WatchNextResult.Unavailable(WatchNextResult.Reason.NO_PERMISSION))
    val status: StateFlow<WatchNextResult> = mutableStatus
    @Volatile private var denied = false
    fun permissionDenied() { denied = true; mutableStatus.value = WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED) }
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun refresh() { requests.tryEmit(Unit) }
    suspend fun read(): WatchNextResult = withContext(Dispatchers.IO) {
        val result = if (!hasPermission()) WatchNextResult.Unavailable(if (denied) WatchNextResult.Reason.BLOCKED else WatchNextResult.Reason.NO_PERMISSION)
        else try {
            resolver.query(TvContract.WatchNextPrograms.CONTENT_URI, null, null, null, null)?.use { cursor ->
                WatchNextResult.Available(filterWatchNext(mapWatchNext(cursor), selfPackage))
            } ?: WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED)
        } catch (_: SecurityException) { WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED) }
          catch (_: IllegalArgumentException) { WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED) }
        mutableStatus.value = result
        result
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WatchNextResult> {
        val changes = callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) { trySend(Unit) }
            }
            val registered = try { resolver.registerContentObserver(TvContract.WatchNextPrograms.CONTENT_URI, true, observer); true }
                catch (_: SecurityException) { false }
            trySend(Unit)
            awaitClose { if (registered) resolver.unregisterContentObserver(observer) }
        }
        return requests.onStart { emit(Unit) }.flatMapLatest {
            // Keep listening for a permission grant, without observing a provider we cannot read.
            if (hasPermission()) changes.mapLatest { read() } else flow { emit(read()) }
        }
    }
}
