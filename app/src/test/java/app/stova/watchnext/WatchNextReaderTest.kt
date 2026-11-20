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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class WatchNextReaderTest {
 private val context:android.content.Context=androidx.test.core.app.ApplicationProvider.getApplicationContext()
    @Test fun missingPermissionRegistersNoObserverAndGrantStartsObservation() = runTest {
        var allowed = false
        val resolver = context.contentResolver
        val shadow = org.robolectric.Shadows.shadowOf(resolver)
        val uri = android.media.tv.TvContract.WatchNextPrograms.CONTENT_URI
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(android.media.tv.TvContract.AUTHORITY, provider())
        val reader = WatchNextReader(resolver, { allowed }, "app.stova")
        val emitted = kotlinx.coroutines.channels.Channel<WatchNextResult>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val job = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { reader.observe().collect { emitted.send(it) } }
        // Wait for the initial IO read before changing permission. A status value alone can
        // otherwise come from that old read, before the new observer has been registered.
        assertEquals(WatchNextResult.Unavailable(WatchNextResult.Reason.NO_PERMISSION), emitted.receive())
        assertTrue(shadow.getContentObservers(uri).isEmpty())
        allowed = true
        reader.refresh()
        assertTrue(emitted.receive() is WatchNextResult.Available)
        assertEquals(1, shadow.getContentObservers(uri).size)
        job.cancel()
    }
    private fun cursor(vararg rows: Array<Any?>) = MatrixCursor(arrayOf(BaseColumns._ID, "package_name", "title", "episode_title", "poster_art_uri", "intent_uri", "last_playback_position_millis", "duration_millis", "last_engagement_time_utc_millis", "content_rating")).apply { rows.forEach(::addRow) }
    @Test fun mapsAndFilters() {
        val entries = mapWatchNext(cursor(
            arrayOf(1L, "com.netflix.ninja", "Dark", "Ep 1", "https://x/p.jpg", "intent:#Intent;package=com.netflix.ninja;end", 600_000L, 3_000_000L, 200L, "com.android.tv/US_TV/US_TV_MA"),
            arrayOf(2L, "app.reelstack", "Kystvakta", null, null, "spole://watch-next/x/y", 0L, 0L, 300L, null),
            arrayOf(3L, "com.disney", "Andor", null, null, "intent:#Intent;package=com.disney;end", 0L, 0L, 100L, null)))
        val kept = filterWatchNext(entries, "app.stova")
        assertEquals(listOf("Dark", "Andor"), kept.map { it.title }); assertEquals(0.2f, kept[0].progress!!, 0.001f); assertNull(kept[1].progress)
        assertEquals("com.android.tv/US_TV/US_TV_MA", kept[0].contentRating); assertNull(kept[1].contentRating)
        assertEquals(600_000L, kept[0].positionMs); assertEquals(3_000_000L, kept[0].durationMs)
        assertNull(kept[1].positionMs); assertNull(kept[1].durationMs)
    }
    @Test fun contentRatingsBecomeAges() {
        assertEquals(13, watchNextAge("com.android.tv/US_MV/US_MV_PG13"))
        assertEquals(18, watchNextAge("com.android.tv/US_TV/US_TV_MA/US_TV_V"))
        assertEquals(7, watchNextAge("com.android.tv/DK_TV/DK_TV_7"))
        assertEquals(0, watchNextAge("com.android.tv/DE_TV/DE_TV_ALL"))
        assertEquals(18, watchNextAge("com.android.tv/DK_TV/DK_TV_7, com.android.tv/US_MV/US_MV_R"))
        listOf(null, "", "garbage", "com.android.tv/IN_TV/IN_TV_A", "com.android.tv/DK_TV/DK_TV_7,com.android.tv/IN_TV/IN_TV_A")
            .forEach { assertNull(it, watchNextAge(it)) }
    }
    @Test fun anAgeLimitKeepsOnlyTitlesRatedWithinIt() {
        fun entry(id: Long, rating: String?) = WatchNextEntry(id, "demo.player", "T$id", null, null, "intent:#Intent;package=demo.player;end", null, id, rating)
        val entries = listOf(entry(1, "com.android.tv/DE_TV/DE_TV_6"), entry(2, "com.android.tv/US_MV/US_MV_R"), entry(3, null))
        assertEquals(listOf(1L), withinAge(entries, 6).map { it.id })
        assertEquals(listOf(1L, 2L), withinAge(entries, 18).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), withinAge(entries, null).map { it.id })
    }
    // A 15-rated film from a grown-up's app lay on a six-year-old's Home: the profile's limit applies to the row.
    @Test fun theRowFollowsTheProfilesAgeLimit() = runTest {
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(android.media.tv.TvContract.AUTHORITY, provider(rows = listOf(
            arrayOf(1L, "demo.player", "Bluey", null, null, "intent:#Intent;package=demo.player;end", 0L, 0L, 300L, "com.android.tv/DK_TV/DK_TV_ALL"),
            arrayOf(2L, "demo.player", "Heat", null, null, "intent:#Intent;package=demo.player;end", 0L, 0L, 200L, "com.android.tv/DK_TV/DK_TV_15"),
            arrayOf(3L, "demo.player", "Unrated", null, null, "intent:#Intent;package=demo.player;end", 0L, 0L, 100L, null))))
        val source = WatchNextRowSource(WatchNextReader(context.contentResolver, { true }, "app.stova"))
        fun profile(maxAge: Int?) = app.stova.profiles.Profile("p", "Demo", app.stova.core.theme.AvatarColor.ROSE,
            appearance = app.stova.profiles.AppearancePrefs(backgroundFilter = app.stova.profiles.BackgroundFilter(maxAge = maxAge)))
        suspend fun titles(maxAge: Int?) = source.rows(profile(maxAge)).first()
            .filterIsInstance<app.stova.home.HomeRowUi.WatchNext>().flatMap { it.entries }.map { it.title }
        assertEquals(listOf("Bluey"), titles(6))
        assertEquals(listOf("Bluey", "Heat"), titles(15))
        assertEquals(listOf("Bluey", "Heat", "Unrated"), titles(null))
    }
    @Test fun theSameTitleIsShownOnceFromTheAppThatSawItLast() {
        fun entry(id: Long, pkg: String, title: String, at: Long, subtitle: String? = null) =
            WatchNextEntry(id, pkg, title, subtitle, null, "intent:#Intent;package=$pkg;end", null, at)
        val kept = filterWatchNext(listOf(entry(1, "org.jellyfin.androidtv", "One Battle After Another", 100),
            entry(2, "org.jellyfin.androidtv", "One Battle After Another", 300), entry(3, "org.jellyfin.androidtv", "one battle after another ", 200),
            entry(4, "com.netflix.ninja", "One Battle After Another", 50, "  "),
            entry(5, "no.nrk.tv", "Emil", 40, "Episode 1"), entry(6, "no.nrk.tv", "Emil", 30, "Episode 2")), "app.stova")
        assertEquals(listOf(2L, 5L, 6L), kept.map { it.id })
    }
    @Test fun foreignComponentRejected() = assertNull(launchIntentFor(WatchNextEntry(1, "com.evil", "x", null, null,
        "intent:#Intent;component=com.bank/.Pay;end", null, 0)))
    @Test fun ownPackageAllowed() = assertEquals("com.netflix.ninja", launchIntentFor(WatchNextEntry(1, "com.netflix.ninja", "x", null, null,
        "intent:#Intent;package=com.netflix.ninja;end", null, 0))!!.`package`)
    private fun provider(blocked: Boolean = false, rows: List<Array<Any?>> = listOf(
        arrayOf(1L, "demo.player", "Demo", null, null, "intent:#Intent;package=demo.player;end", 10L, 100L, 200L, null))): android.content.ContentProvider = object : android.content.ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: android.net.Uri): String? = null
        override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? = null
        override fun delete(uri: android.net.Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: android.net.Uri, values: android.content.ContentValues?, selection: String?, args: Array<out String>?) = 0
        override fun query(uri: android.net.Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): android.database.Cursor {
            assertNull(selection); assertNull(args)
            if (blocked) throw SecurityException("blocked")
            return cursor(*rows.toTypedArray())
        }
    }
    @Test fun providerQueryHasNoSelection() = runTest {
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(android.media.tv.TvContract.AUTHORITY, provider())
        val result = WatchNextReader(context.contentResolver, { true }, "app.stova").read() as WatchNextResult.Available
        assertEquals("Demo", result.entries.single().title)
    }
    @Test fun blockedProviderIsUnavailable() = runTest {
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(android.media.tv.TvContract.AUTHORITY, provider(blocked = true))
        assertEquals(WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED), WatchNextReader(context.contentResolver, { true }, "app.stova").read())
    }
    @Test fun deniedPermissionIsBlocked() = runTest {
        val reader = WatchNextReader(context.contentResolver, { false }, "app.stova")
        reader.permissionDenied()
        assertEquals(WatchNextResult.Unavailable(WatchNextResult.Reason.BLOCKED), reader.read())
    }
    @Test fun kidNeverReadsProvider() = runTest {
        var permissionReads = 0
        val reader = WatchNextReader(context.contentResolver, { permissionReads++; true }, "app.stova")
        val kid = app.stova.profiles.Profile("kid", "Demo", app.stova.core.theme.AvatarColor.ROSE, kid = true)
        assertTrue(WatchNextRowSource(reader).rows(kid).first().isEmpty())
        assertEquals(0, permissionReads)
    }
    @Test fun noPermissionIsUnavailable() = runTest {
        assertEquals(WatchNextResult.Unavailable(WatchNextResult.Reason.NO_PERMISSION), WatchNextReader(context.contentResolver, { false }, "app.stova").read())
    }
}
