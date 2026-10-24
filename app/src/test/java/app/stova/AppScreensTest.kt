package app.stova

import android.content.ComponentName
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import app.stova.home.*
import app.stova.media.*
import app.stova.media.model.*
import app.stova.background.BackdropLayer
import app.stova.core.theme.StovaTheme
import app.stova.navigation.TopBar
import app.stova.profiles.Profile
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.robolectric.Shadows.shadowOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The real app, rendered for review. Run with -Pstova.screens=true. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container get() = (rule.activity.application as StovaApplication).container

    @Before fun enabled() = assumeScreensEnabled()

    @Test fun firstRun() {
        rule.saveScreen("02-first-run")
    }

    @Test fun picker() {
        runBlocking {
            val a = container.profiles.create("Demo", AvatarColor.LIME, false)
            container.profiles.create("Liten", AvatarColor.ROSE, true)
            container.profiles.create("Vaksen", AvatarColor.OCEAN, false)
            container.profiles.setPin(a.id, "1357")
            container.profiles.setStartInLastProfile(false)
            container.session.leave()
        }
        rule.saveScreen("03-picker")
    }

    @Test fun shell() {
        runBlocking { container.session.enter(container.profiles.create("Demo", AvatarColor.LIME, false).id) }
        rule.saveScreen("04-shell")
    }

    @Test fun settings() {
        runBlocking {
            val a = container.profiles.create("Demo", AvatarColor.LIME, false)
            container.profiles.create("Liten", AvatarColor.ROSE, true)
            container.profiles.setPin(a.id, "1357")
            container.session.enter(a.id)
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Liten").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("07-settings")
        rule.onNodeWithText("Home-knappen").performClick()
        rule.waitForIdle()
        rule.saveScreen("08-home-button")
    }

    private fun installFakeApps(): List<String> {
        val pm = shadowOf(rule.activity.packageManager)
        return listOf("Spole", "Jellyfin", "Tunet", "Kodi", "VLC", "Innstillingar").map { label ->
            val pkg = "demo.${label.lowercase()}"
            val component = ComponentName(pkg, "$pkg.Main")
            pm.addActivityIfNotPresent(component).nonLocalizedLabel = label
            pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) })
            "$pkg/$pkg.Main"
        }
    }

    @Test fun homeWithFavorites() {
        val keys = installFakeApps()
        runBlocking {
            val id = container.profiles.create("Demo", AvatarColor.LIME, false).id
            keys.take(4).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Favorittappar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("05-home")
    }

    @Test fun appsPage() {
        val keys = installFakeApps()
        runBlocking {
            val id = container.profiles.create("Demo", AvatarColor.LIME, false).id
            keys.take(2).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Appar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alle appar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("06-apps")
    }

    /** Local geometric art, painted deterministically; these screens never contact a server. */
    private fun demoArt(name: String, tint: Int): String {
        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(tint)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.rgb(175, 180, 155)
        canvas.drawCircle(900f, 150f, 85f, paint)
        paint.color = android.graphics.Color.rgb(46, 67, 62)
        canvas.drawPath(android.graphics.Path().apply {
            moveTo(0f, 500f); lineTo(350f, 160f); lineTo(700f, 510f)
            lineTo(1050f, 290f); lineTo(1280f, 490f); lineTo(1280f, 720f); lineTo(0f, 720f); close()
        }, paint)
        paint.color = android.graphics.Color.rgb(27, 44, 43)
        canvas.drawRect(0f, 570f, 1280f, 720f, paint)
        val file = File(rule.activity.cacheDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    }

    private fun renderMediaHome(poster: Boolean = false, fontScale: Float = 1f) {
        val backdrop = demoArt("demo-landscape", android.graphics.Color.rgb(79, 95, 88))
        val second = demoArt("demo-evening", android.graphics.Color.rgb(68, 77, 95))
        val account = MediaAccount("demo-account", "demo-profile", ServerKind.JELLYFIN, "demo-server", "Demoarkiv", "https://demo.invalid", "demo-user", "Demo")
        val item = MediaItem("demo-film", account.id, ItemType.MOVIE, "Fjordlys", year = 2024, runtimeMinutes = 112,
            overview = "Ei ferd gjennom stille fjordar og opne landskap. Vegen vidare byr på nye møte og ukjende stader.",
            ratings = Ratings(7.84, 91, "12"), progress = 0.405f, art = ItemArt(backdrop = backdrop, poster = backdrop, wide = backdrop))
        val items = listOf(item, item.copy(id = "demo-film-2", title = "Vegen vidare", art = ItemArt(backdrop = second, poster = second, wide = second)), item.copy(id = "demo-film-3", title = "Nordavind"))
        val kind = if (poster) MediaRowKind.LATEST_MOVIES else MediaRowKind.CONTINUE
        val key = if (poster) HomeRowKey.latestMovies(account.id) else HomeRowKey.continueWatching(account.id)
        val rows = listOf(HomeRowUi.Media(key, account, kind, items, false),
            HomeRowUi.Media(HomeRowKey.latestSeries(account.id), account, MediaRowKind.LATEST_SERIES,
                listOf(item.copy(id = "demo-episode", type = ItemType.EPISODE, seriesName = "Fjordlys", season = 1, episode = 2)), false))
        rule.runOnUiThread {
            rule.activity.setContent {
                StovaTheme(reduceMotion = true) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                        Box(Modifier.fillMaxSize()) {
                            BackdropLayer(backdrop)
                            HomeContent(rows, FocusedTarget.Media(item, account), key, container.appArt, container.appCatalog,
                                {}, {}, {}, {}, topBar = { TopBar(Profile("demo-profile", "Demo", AvatarColor.LIME), {}, {}, {}) })
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("7,8").assertIsDisplayed()
        rule.onNodeWithText(app.stova.media.ui.criticText(91, java.util.Locale.forLanguageTag("nn"))).assertExists()
        // Decode the same sized requests into Coil's memory cache before taking the window image.
        runBlocking {
            val loader = coil3.SingletonImageLoader.get(rule.activity)
            for (url in listOf(backdrop, second)) for ((w, h) in listOf(1920 to 1080, 496 to 279, 264 to 396)) {
                loader.execute(coil3.request.ImageRequest.Builder(rule.activity).data(url).size(w, h).build())
            }
        }
        rule.waitForIdle()
    }

    @Test fun homeWithHeroScoreAndMediaRows() {
        renderMediaHome()
        rule.saveScreen("09-home-media-hero")
    }

    @Test fun homeWithMoviePosters() {
        renderMediaHome(poster = true)
        rule.saveScreen("10-home-media-posters")
    }

    @Test fun mediaHeroAtDoubleFontSize() {
        renderMediaHome(fontScale = 2f)
        rule.onAllNodesWithText("Demoarkiv")[0].assertIsDisplayed()
        rule.saveScreen("11-home-media-font-2")
    }
}
