package app.stova.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaAppNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun enterProfile() {
        val container = (rule.activity.application as StovaApplication).container
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            container.session.enter(a.id)
        }
        rule.waitForIdle()
    }

    @Test fun homeHasNoSidebarAndFillsTheScreen() {
        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertCountEquals(0)
        rule.onNodeWithTag("screen-HOME").assertWidthIsEqualTo(960.dp)
    }

    @Test fun topBarOpensAppsAndBackGoesHome() {
        rule.onAllNodesWithText("Appar")[0].performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.waitForIdle() // the Back handler is enabled by the recomposition the click causes
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertFalse(rule.activity.isFinishing)
    }

    @Test fun emptyHomeFocusesOpenApps() {
        // Found on the TV emulator: with nothing focused, the first D-pad press was swallowed.
        rule.onNodeWithText("Opne Appar").assertIsFocused()
    }

    @Test fun topBarOpensSettings() {
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
    }
}
