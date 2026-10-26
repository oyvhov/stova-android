package app.stova.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class TopBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hiddenClockRemovesOnlyTheClockSlot() {
        val clock = SemanticsMatcher("clock text") { node ->
            node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text) &&
                node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].any { Regex("[0-9]+:[0-9]+.*").matches(it.text) }
        }
        val mode = androidx.compose.runtime.mutableStateOf(app.stova.profiles.ClockMode.HIDDEN)
        compose.setContent { StovaTheme(appearance = app.stova.profiles.AppearancePrefs(clock = mode.value)) {
            TopBar(Profile("a", "A", AvatarColor.LIME), {}, {}, {})
        } }
        compose.onNode(clock).assertDoesNotExist()
        compose.onNodeWithText("Appar").assertIsDisplayed()
        compose.runOnIdle { mode.value = app.stova.profiles.ClockMode.H12 }
        compose.onNode(clock).assertIsDisplayed()
    }

    @Test fun adultHomeHasOneSettingsEntryAndNoSeparateAndroidIcon() {
        compose.setContent { StovaTheme {
            TopBar(Profile("a", "A", AvatarColor.LIME), {}, {}, {})
        } }
        compose.onNodeWithText("Android-innstillingar").assertDoesNotExist()
        compose.onNodeWithText("Innstillingar").assertIsDisplayed()
    }

    @Test fun noAndroidSettingsButtonWithoutAnAction() {
        compose.setContent { StovaTheme { TopBar(Profile("k", "K", AvatarColor.ROSE, kid = true), {}, {}, {}) } }
        compose.onNodeWithText("Android-innstillingar").assertDoesNotExist()
    }

    @Test fun focusingLabelsKeepsNeighbourButtonsInPlace() {
        compose.setContent { StovaTheme {
            TopBar(Profile("a", "A", AvatarColor.LIME), {}, {}, {})
        } }
        val labels = listOf("Appar", "Innstillingar", "Byt profil")
        val before = labels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        compose.onNodeWithText("Appar").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Appar").assertIsFocused()
        compose.onNodeWithText("Appar").performKeyInput {
            keyDown(Key.DirectionRight); keyUp(Key.DirectionRight)
        }
        compose.onNodeWithText("Innstillingar").assertIsFocused()
        val after = labels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        assertEquals(before[0], after[0])
        assertEquals(before[2], after[2])
        assertEquals(before[1].center.x, after[1].center.x, 0.1f)
        org.junit.Assert.assertTrue(after[1].width in before[1].width..before[1].width*1.02f)
        org.junit.Assert.assertTrue(kotlin.math.abs(after[1].center.y-before[1].center.y) <= 6f)
    }
}
