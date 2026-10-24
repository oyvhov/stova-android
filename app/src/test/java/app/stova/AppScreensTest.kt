package app.stova

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
}
