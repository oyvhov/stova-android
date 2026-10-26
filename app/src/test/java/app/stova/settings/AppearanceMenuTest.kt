package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.assumeScreensEnabled
import app.stova.saveScreen
import app.stova.core.theme.StovaTheme
import app.stova.profiles.AppearancePrefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun render(fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                StovaTheme(reduceMotion = true) {
                    var section by remember { mutableStateOf(SettingsSection.APPEARANCE) }
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsLayout(section, { section = it }) { shown, _ ->
                            if (shown == SettingsSection.APPEARANCE) AppearanceMenu { category, firstControl ->
                                // The real screen puts HomeRowSettings here; it needs a container.
                                if (category == AppearanceCategory.ROWS) app.stova.core.ui.StovaButton("Rows control", {}, firstControl)
                                else AppearanceSettingsContent(AppearancePrefs(), category = category, firstControl = firstControl, includePreview = false, onChange = {})
                            } else app.stova.core.ui.StovaButton("Other section", {})
                        }
                    }
                }
            }
        }
        compose.onNode(hasText("Heim og utsjånad") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
            androidx.compose.ui.semantics.Role.Tab)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
    }
    @Test fun overviewFitsAllNineCategories() {
        render()
        listOf("Tema", "Heim og hero", "Kort og rader", "Bakgrunn", "Fokus", "Toppfelt", "Ikon", "Profil", "Rader på Heim").forEach {
            compose.onNode(hasText(it) and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.Button)).assertIsDisplayed()
        }
        compose.onNode(hasText("Ikon") and hasClickAction()).assertIsDisplayed()
    }

    @Test fun categoryHasOneTitleAndTheSameSymbolAsItsOverviewTile() {
        render()
        compose.onNodeWithTag("appearance-icon-FOCUS", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Fokus") and hasClickAction()).performClick()
        compose.onAllNodesWithText("Fokus").assertCountEquals(1)
        compose.onNodeWithTag("appearance-header-icon-FOCUS", useUnmergedTree = true).assertIsDisplayed()
    }
    @Test fun largeTextCanReachLastCategoryAndReturnWithoutChangingValues() {
        render(2f)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNode(hasText("Tema") and hasClickAction()).assertIsFocused()
        compose.onNode(hasText("Ikon") and hasClickAction()).performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNode(hasText("Ikon") and hasClickAction()).assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        if (System.getProperty("stova.screens") == "true") compose.saveScreen("menu-large-icons")
        compose.onNodeWithText("Ikonpakke").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Back) }
        compose.onNode(hasText("Ikon") and hasClickAction()).assertIsFocused().assertIsDisplayed()
    }
    @Test fun menuScreens() {
        assumeScreensEnabled()
        render()
        compose.saveScreen("menu-overview")
        compose.onNode(hasText("Fokus") and hasClickAction()).performClick()
        compose.saveScreen("menu-focus")
    }

    @Test fun allCategoriesEnterTheirOwnFirstControlAndBackReturnsToTheirTile() {
        render()
        val first = listOf("Tema", "Synlege rader", "Kortstorleik", "Bakgrunnskjelde", "Fokuseffekt", "Klokkeformat", "Ikonpakke", "Tilbakestill utsjånad", "Rows control")
        AppearanceCategory.entries.forEachIndexed { index, category ->
            compose.onNodeWithTag("appearance-icon-${category.name}", useUnmergedTree = true)
                .onParent().performClick()
            compose.onNode(hasText(first[index]) and hasClickAction()).assertIsFocused()
            compose.onRoot().performKeyInput { pressKey(Key.Back) }
            compose.onNodeWithTag("appearance-icon-${category.name}", useUnmergedTree = true)
                .onParent().assertIsFocused()
        }
    }

    @Test fun homeLayoutScreen() {
        assumeScreensEnabled()
        render()
        compose.onNode(hasText("Heim og hero") and hasClickAction()).performClick()
        compose.onNodeWithText("Synlege rader").assertIsFocused()
        compose.saveScreen("menu-home-layout")
    }
}
