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
    val posterUri: String?, val intentUri: String, val progress: Float?, val lastEngagement: Long)
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
            number(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS)))
    }
}
fun filterWatchNext(entries: List<WatchNextEntry>, selfPackage: String, limit: Int = 20): List<WatchNextEntry> =
    entries.filterNot { it.packageName == selfPackage || it.packageName == "app.stova" || it.packageName.startsWith("app.reelstack") }
        .sortedByDescending { it.lastEngagement }.take(limit)

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
