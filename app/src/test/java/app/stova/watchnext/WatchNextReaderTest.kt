package app.stova.watchnext
import android.database.MatrixCursor
import android.provider.BaseColumns
import app.stova.TV_QUALIFIERS
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class WatchNextReaderTest {
 private val context:android.content.Context=androidx.test.core.app.ApplicationProvider.getApplicationContext()
    private fun cursor(vararg rows: Array<Any?>) = MatrixCursor(arrayOf(BaseColumns._ID, "package_name", "title", "episode_title", "poster_art_uri", "intent_uri", "last_playback_position_millis", "duration_millis", "last_engagement_time_utc_millis")).apply { rows.forEach(::addRow) }
    @Test fun mapsAndFilters() {
        val entries = mapWatchNext(cursor(
            arrayOf(1L, "com.netflix.ninja", "Dark", "Ep 1", "https://x/p.jpg", "intent:#Intent;package=com.netflix.ninja;end", 600_000L, 3_000_000L, 200L),
            arrayOf(2L, "app.reelstack", "Kystvakta", null, null, "spole://watch-next/x/y", 0L, 0L, 300L),
            arrayOf(3L, "com.disney", "Andor", null, null, "intent:#Intent;package=com.disney;end", 0L, 0L, 100L)))
        val kept = filterWatchNext(entries, "app.stova")
        assertEquals(listOf("Dark", "Andor"), kept.map { it.title }); assertEquals(0.2f, kept[0].progress!!, 0.001f); assertNull(kept[1].progress)
    }
    @Test fun foreignComponentRejected() = assertNull(launchIntentFor(WatchNextEntry(1, "com.evil", "x", null, null,
        "intent:#Intent;component=com.bank/.Pay;end", null, 0)))
    @Test fun ownPackageAllowed() = assertEquals("com.netflix.ninja", launchIntentFor(WatchNextEntry(1, "com.netflix.ninja", "x", null, null,
        "intent:#Intent;package=com.netflix.ninja;end", null, 0))!!.`package`)
    @Test fun noPermissionIsUnavailable() = runTest {
        assertEquals(WatchNextResult.Unavailable(WatchNextResult.Reason.NO_PERMISSION), WatchNextReader(context.contentResolver, { false }, "app.stova").read())
    }
}
