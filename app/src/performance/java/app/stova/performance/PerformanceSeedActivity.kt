package app.stova.performance

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaColors
import app.stova.ha.cards.CardConfig
import app.stova.ha.cards.CardType
import app.stova.home.HomeRowKey
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.net.AuthResult
import app.stova.media.net.ServerInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Emulator-only fixture entry point. This class and its manifest are absent from release. */
class PerformanceSeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.view.View(this))
        check(android.os.Build.HARDWARE in setOf("ranchu", "goldfish")) { "Demo seeding requires an emulator" }
        val container = (application as StovaApplication).container
        lifecycleScope.launch {
          withContext(Dispatchers.IO) {
            val existing = container.profiles.current().profiles
            check(existing.all { it.name == "Demo" && it.pin == null }) { "Refusing to replace personal profiles" }
            val profile = existing.singleOrNull() ?: container.profiles.create("Demo", AvatarColor.OCEAN, false)
            val apps = container.appCatalog.installed()
            container.profiles.update(profile.id) {
                it.copy(apps = it.apps.copy(favorites = apps.take(5).map { app -> app.key }),
                    dashboard = listOf(CardConfig("demo-sensor", CardType.SENSOR, "sensor.demo_temperature", onHome = true)))
            }
            // No real server or credentials. A refused local connection preserves cached rows.
            val account = container.mediaAccounts.add(profile.id,
                ServerInfo(ServerKind.JELLYFIN, "demo", "Demoarkiv", "demo", "http://127.0.0.1:1"),
                AuthResult("fictional-demo-token", "demo", "Demo"))
            val artFile = File(filesDir, "demo-art.png")
            val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(StovaColors.Raised.toArgb())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = StovaColors.Muted.toArgb() }
            canvas.drawCircle(470f, 105f, 65f, paint)
            paint.color = StovaColors.Surface.toArgb()
            canvas.drawOval(-100f, 150f, 750f, 550f, paint)
            artFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val art = ItemArt(backdrop = artFile.toURI().toString(), poster = artFile.toURI().toString(), wide = artFile.toURI().toString())
            val items = (1..20).map { index ->
                MediaItem("demo$index", account.id, ItemType.MOVIE, "Demofilm $index", year = 2024,
                    runtimeMinutes = 112, overview = "Fiktivt innhald for profilgenerering og skjermbilete.",
                    ratings = Ratings(7.8, 91, "12"), progress = 0.35f, art = art)
            }
            val snapshot = MediaSnapshot(listOf(
                SnapshotRow(HomeRowKey.continueWatching(account.id), account.id, MediaRowKind.CONTINUE, items),
                SnapshotRow(HomeRowKey.latestMovies(account.id), account.id, MediaRowKind.LATEST_MOVIES, items),
                SnapshotRow(HomeRowKey.latestSeries(account.id), account.id, MediaRowKind.LATEST_SERIES, items.map { it.copy(type = ItemType.SERIES) }),
            ))
            File(filesDir, "snapshots/${profile.id}.json").apply {
                parentFile!!.mkdirs()
                writeText(StovaJson.encodeToString(MediaSnapshot.serializer(), snapshot))
            }
            container.household.update { it.copy(haBaseUrl = "http://127.0.0.1:1") }
            container.session.enter(profile.id)
          }
          startActivity(Intent(this@PerformanceSeedActivity, MainActivity::class.java))
          finish()
        }
    }
}
