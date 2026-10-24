package app.stova

import android.content.ComponentName
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
            val a = container.profiles.create("Øyvind", AvatarColor.LIME, false)
            container.profiles.create("Maya", AvatarColor.ROSE, true)
            container.profiles.create("Jonas", AvatarColor.OCEAN, false)
            container.profiles.setPin(a.id, "1357")
            container.profiles.setStartInLastProfile(false)
            container.session.leave()
        }
        rule.saveScreen("03-picker")
    }

    @Test fun shell() {
        runBlocking { container.session.enter(container.profiles.create("Øyvind", AvatarColor.LIME, false).id) }
        rule.saveScreen("04-shell")
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
            val id = container.profiles.create("Øyvind", AvatarColor.LIME, false).id
            keys.take(4).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Favorittappar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("05-home")
    }

    @Test fun appsPage() {
        val keys = installFakeApps()
        runBlocking {
            val id = container.profiles.create("Øyvind", AvatarColor.LIME, false).id
            keys.take(2).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Appar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alle appar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("06-apps")
    }
}
