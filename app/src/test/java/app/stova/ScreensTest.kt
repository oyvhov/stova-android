package app.stova

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.PinPanel
import app.stova.profiles.PinCheck
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders screens for review. Run: scripts/gradle.ps1 testDebugUnitTest --tests app.stova.ScreensTest -Pstova.screens=true */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun enabled() = assumeScreensEnabled()

    @Test fun pin() {
        compose.setContent {
            StovaTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    PinPanel("Skriv PIN for Øyvind", { PinCheck.Accepted }, {})
                }
            }
        }
        compose.saveScreen("01-pin")
    }
}
