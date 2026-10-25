package app.stova.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun titleStaysVisibleAtTheEndOfTheMenuWithLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                StovaTheme { SettingsNavigation(SettingsSection.PROFILE, {}, {}) }
            }
        }
        compose.onNodeWithText("Android-innstillingar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Innstillingar").assertIsDisplayed()
    }
}
