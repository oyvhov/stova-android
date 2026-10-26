package app.stova.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
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
                StovaTheme { SettingsNavigation(SettingsSection.PROFILE, {}) }
            }
        }
        compose.onNodeWithText("System").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Innstillingar").assertIsDisplayed()
    }

    @Test fun everySectionHasItsOwnVisualSymbol() {
        compose.setContent { StovaTheme { SettingsNavigation(SettingsSection.APPEARANCE, {}) } }
        settingsOrder.forEach { section ->
            compose.onNodeWithTag("settings-icon-${section.name}", useUnmergedTree = true).assertExists()
        }
    }

    @Test fun fourSectionsAndAppsWithoutHeadingsOrAnAndroidTab() {
        compose.setContent { StovaTheme { SettingsNavigation(SettingsSection.APPEARANCE, {}, onApps = {}) } }
        val tabs = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.Text].joinToString() }
        assertEquals(listOf("Heim og utsjånad", "Smarthus og kontoar", "Profilar", "Appar", "System"), tabs)
        compose.onNodeWithText("Android").assertDoesNotExist()
    }

    @Test fun appsSitsBetweenProfilesAndSystemInDpadOrder() {
        compose.setContent { StovaTheme { SettingsNavigation(SettingsSection.APPEARANCE, {}, onApps = {}) } }
        fun tab(label: String) = compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        tab("Profilar").performSemanticsAction(SemanticsActions.RequestFocus)
        tab("Profilar").performKeyInput { pressKey(Key.DirectionDown) }
        tab("Appar").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        tab("System").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        tab("System").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        tab("Appar").assertIsFocused()
    }
}
