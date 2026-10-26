package app.stova.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

    @Test fun homeOpensAppsAndBackGoesHome() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.waitForIdle() // the Back handler is enabled by the recomposition the click causes
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertFalse(rule.activity.isFinishing)
    }

    @Test fun emptyHomeFocusesOpenApps() {
        // Found on the TV emulator: with nothing focused, the first D-pad press was swallowed.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").assertIsFocused()
    }

    // Review Important 2: Home pressed while a Stova page is open goes back to Home.
    @Test fun homeIntentReturnsToHome() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.activityRule.scenario.onActivity {
            it.onNewIntent(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME))
        }
        rule.onNodeWithTag("screen-HOME").assertExists()
    }

    @Test fun settingsOpensAppsAndBackReturnsToSettingsThenHome() {
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
        rule.onNodeWithText("Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-HOME").assertExists()
    }

    @Test fun topBarOpensSettings() {
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
    }
    @Test fun smartHomeOnlyWhenConfiguredAndBackClosesPage() {
        rule.onAllNodesWithText("Smarthus").assertCountEquals(0)
        val container=(rule.activity.application as StovaApplication).container
        runBlocking { container.household.update { it.copy(haBaseUrl="http://ha.local:8123") } }
        rule.waitForIdle()
        rule.onAllNodesWithText("Smarthus")[0].performClick()
        rule.onNodeWithTag("screen-SMART_HOME").assertExists()
        rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
    }

    // Pages fade over each other. Once the fade has settled the old page is gone and focus is on the new one.
    @Test fun pageFadeSettlesWithTheOldPageGoneAndFocusOnTheNewOne() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.onAllNodesWithText("Opne Appar").assertCountEquals(0)
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").assertIsFocused()
        // Apps opens from Settings, not the top bar; the Apps page title has gone with its page.
        rule.onAllNodesWithText("Appar").assertCountEquals(0)
    }

    // While the old page is still fading, a stray D-pad press must not land on it.
    @Test fun theLeavingPageCannotTakeFocusWhileItFades() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.onNodeWithText("Opne Appar").assertExists() // still fading out
        rule.onNodeWithText("Opne Appar").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Opne Appar").assertIsNotFocused()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Opne Appar").assertDoesNotExist()
    }

    @Test fun homeIntentClosesSmartHomeAndKeepsItAvailable() {
        val container = (rule.activity.application as StovaApplication).container
        runBlocking { container.household.update { it.copy(haBaseUrl = "http://ha.local:8123") } }
        rule.waitForIdle()
        rule.onAllNodesWithText("Smarthus")[0].performClick()
        rule.onNodeWithTag("screen-SMART_HOME").assertExists()
        rule.activityRule.scenario.onActivity {
            it.onNewIntent(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME))
        }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.onAllNodesWithText("Smarthus")[0].performClick()
        rule.onNodeWithTag("screen-SMART_HOME").assertExists()
    }
}
