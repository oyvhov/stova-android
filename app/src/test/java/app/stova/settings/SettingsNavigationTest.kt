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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.Text].first().text }
        assertEquals(listOf("Heim og utsjånad", "Smarthus og kontoar", "Profilar", "Appar", "System"), tabs)
        compose.onNodeWithText("Android").assertDoesNotExist()
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE) // real font metrics for the line check
    @Test fun eachSectionIsALargeRowWithItsSymbolAndWhatItHolds() {
        compose.setContent { StovaTheme { SettingsNavigation(SettingsSection.APPEARANCE, {}, onApps = {}) } }
        listOf("Tema, rader og bakgrunn", "Home Assistant og media", "Namn, PIN og barn", "Favorittar og ikon",
            "Heim-knappen og om Stova").forEach { compose.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed() }
        val tabs = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).fetchSemanticsNodes()
        assertEquals(5, tabs.size)
        val density = compose.onRoot().fetchSemanticsNode().layoutInfo.density
        tabs.forEach { assertTrue("${it.config[SemanticsProperties.Text]}", with(density) { it.size.height.toDp() } >= 60.dp) }
        compose.onNodeWithTag("settings-icon-APPEARANCE", useUnmergedTree = true).assertWidthIsEqualTo(StovaLayout.CategoryIconSize)
        // The longest name keeps to one line in the wider list.
        assertTrue(compose.onNodeWithText("Smarthus og kontoar", useUnmergedTree = true).getBoundsInRoot().height < 30.dp)
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
