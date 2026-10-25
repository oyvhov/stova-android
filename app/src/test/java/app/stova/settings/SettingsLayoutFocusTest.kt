package app.stova.settings

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.StovaButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsLayoutFocusTest {
    @get:Rule val compose = createComposeRule()

    private fun render(fontScale: Float = 1f) {
        compose.setContent {
            var selected by remember { mutableStateOf(SettingsSection.PROFILE) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                StovaTheme(reduceMotion = true) {
                    SettingsLayout(selected, { selected = it }, {}) { section, _ ->
                        if (section == SettingsSection.ABOUT) Text("Version")
                        else repeat(12) { index -> StovaButton("${section.name} $index", {}) }
                    }
                }
            }
        }
    }

    private fun tab(label: String) = compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    @Test fun largeFontSidebarMovesInOrderAndStopsAtBothEnds() {
        render(fontScale = 2f)
        tab("Profil").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        tab("Profil").assertIsFocused()
        val labels = listOf("Profil", "Kontoar", "Heim", "Smarthus", "Home-knappen", "Utsjånad", "Om", "Android-innstillingar")
        labels.zipWithNext().forEach { (from, to) ->
            tab(from).performKeyInput { pressKey(Key.DirectionDown) }
            tab(to).assertIsFocused()
        }
        tab(labels.last()).performKeyInput { pressKey(Key.DirectionDown) }
        tab(labels.last()).assertIsFocused()
        labels.asReversed().zipWithNext().forEach { (from, to) ->
            tab(from).performKeyInput { pressKey(Key.DirectionUp) }
            tab(to).assertIsFocused()
        }
    }

    @Test fun fastSectionChangeThenRightEntersTheNewPane() {
        render()
        tab("Profil").performKeyInput {
            repeat(3) { pressKey(Key.DirectionDown) }
            pressKey(Key.DirectionRight)
        }
        compose.onNodeWithText("SMART_HOME 0").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        tab("Smarthus").assertIsFocused()
    }

    @Test fun confirmingASectionEntersItsFirstControl() {
        render()
        tab("Heim").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        tab("Heim").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("HOME 0").assertIsFocused()
    }

    @Test fun linksBetweenPanesEnterTheDestinationWithoutFallingBackToProfile() {
        compose.setContent {
            var selected by remember { mutableStateOf(SettingsSection.PROFILE) }
            StovaTheme(reduceMotion = true) {
                SettingsLayout(selected, { selected = it }, {}) { section, open ->
                    when (section) {
                        SettingsSection.HOME -> StovaButton("Home appearance", { open(SettingsSection.APPEARANCE) })
                        SettingsSection.APPEARANCE -> StovaButton("Edit rows", { open(SettingsSection.HOME) })
                        else -> StovaButton("Profile control", {})
                    }
                }
            }
        }
        tab("Heim").performClick()
        repeat(3) {
            compose.onNodeWithText("Home appearance").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionCenter) }
            compose.onNodeWithText("Edit rows").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionCenter) }
        }
        compose.onNodeWithText("Home appearance").assertIsFocused()
    }

    @Test fun clickingACrossLinkAlsoKeepsTheDestinationSelected() {
        compose.setContent {
            var selected by remember { mutableStateOf(SettingsSection.HOME) }
            StovaTheme(reduceMotion = true) {
                SettingsLayout(selected, { selected = it }, {}) { section, open ->
                    if (section == SettingsSection.HOME) StovaButton("Home appearance", { open(SettingsSection.APPEARANCE) })
                    else StovaButton("${section.name} control", {})
                }
            }
        }
        compose.onNodeWithText("Home appearance").performClick()
        tab("Utsjånad").assertIsSelected()
        compose.onNodeWithText("APPEARANCE control").assertIsFocused()
    }

    @Test fun scrolledContentReturnsToItsSelectedTabAndVerticalEdgesStayInside() {
        render()
        tab("Profil").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("PROFILE 0").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithText("PROFILE 0").assertIsFocused()
        repeat(11) { compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) } }
        compose.onNodeWithText("PROFILE 11").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("PROFILE 11").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        tab("Profil").assertIsFocused()
    }

    @Test fun rightIntoAnInformationalPaneKeepsFocusOnItsTab() {
        render()
        tab("Om").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        tab("Om").performKeyInput { pressKey(Key.DirectionRight) }
        tab("Om").assertIsFocused()
    }

    @Test fun leftUsesThePreviousInlineControlBeforeReturningToTheSidebar() {
        compose.setContent {
            var selected by remember { mutableStateOf(SettingsSection.PROFILE) }
            StovaTheme(reduceMotion = true) {
                SettingsLayout(selected, { selected = it }, {}) { _, _ ->
                    Row {
                        StovaButton("Earlier", {})
                        StovaButton("Later", {})
                    }
                }
            }
        }
        tab("Profil").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("Earlier").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("Later").assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithText("Earlier").assertIsFocused().performKeyInput { pressKey(Key.DirectionLeft) }
        tab("Profil").assertIsFocused()
    }

    private fun assertPendingEntryIsCancelledBy(cancel: Key) {
        render()
        tab("Profil").assertIsFocused()
        compose.mainClock.autoAdvance = false
        tab("Profil").performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
            keyDown(cancel)
            keyUp(cancel)
        }
        compose.mainClock.advanceTimeBy(250)
        tab("Profil").assertIsFocused()
        compose.mainClock.autoAdvance = true
    }

    @Test fun leftCancelsPendingPaneEntryAtTheSidebarBoundary() =
        assertPendingEntryIsCancelledBy(Key.DirectionLeft)

    @Test fun upCancelsPendingPaneEntryAtTheSidebarBoundary() =
        assertPendingEntryIsCancelledBy(Key.DirectionUp)
}
