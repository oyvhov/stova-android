package app.stova.navigation

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import app.stova.media.model.ServerKind
import app.stova.profiles.SeasonChoice
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaAppNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun enterProfile() {
        val container = (rule.activity.application as StovaApplication).container
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            // No season, whatever today's date: the seasonal scene's clock asks for frames of its own, so
            // with the calendar on these tests saw different frames at Halloween and Jul than the rest of the year.
            container.profiles.update(a.id) { it.copy(appearance = SeasonChoice.NONE.applyTo(it.appearance)) }
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
        rule.onNodeWithText("Opne Appar").performClick()
        // Frame by frame until the switch has begun: which frame a click's recomposition lands on is
        // Compose's business. The fade lasts many frames more, so the old page is still there below.
        repeat(5) { if (rule.onAllNodesWithTag("screen-APPS").fetchSemanticsNodes().isEmpty()) rule.mainClock.advanceTimeByFrame() }
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.onNodeWithText("Opne Appar").assertExists() // still fading out
        rule.onNodeWithText("Opne Appar").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Opne Appar").assertIsNotFocused()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Opne Appar").assertDoesNotExist()
    }

    // The Oppdag tile at the end of Home's row opens the page; Back walks home to the tile that opened it.
    @Test fun discoverTileOpensThePageAndBackReturnsHome() {
        val container = (rule.activity.application as StovaApplication).container
        // A Spole that answers Discover for the profile's person, as SpoleDiscoverTest's does.
        shadowOf(rule.activity.packageManager).installPackage(PackageInfo().apply { packageName = "app.reelstack" })
        ShadowContentResolver.registerProviderInternal("app.reelstack.launcher", DiscoverSpole())
        runBlocking { container.profiles.update(container.session.active.value!!.id) { it.copy(spole = SpoleLink(ServerKind.EMBY, "eilev")) } }
        val tile = hasContentDescription("Oppdag") and hasClickAction()
        rule.waitUntil(10_000) { rule.onAllNodes(tile).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription("Heat") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNode(tile).assertIsFocused().performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("screen-DISCOVER").assertExists()
        // The page opens on its first card; OK on it opens the title's step, and Back takes that step away again.
        val card = hasContentDescription("Heat") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(card and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(card and isFocused()).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle() // the Back handlers are enabled by the recomposition the click causes
        rule.onAllNodes(card).assertCountEquals(0)
        rule.onNodeWithText("Heat").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(5_000) { rule.onAllNodes(card and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("screen-DISCOVER").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.waitUntil(5_000) { rule.onAllNodes(tile and isFocused()).fetchSemanticsNodes().isNotEmpty() }
    }

    // A request Spole follows for the person stands on the page; OK opens its title with what the card showed.
    @Test fun discoverRequestOpensItsTitle() {
        openDiscoverPage()
        val request = hasContentDescription("The Gorge") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(request).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Lastar ned · 42 %").assertExists()
        rule.onNode(request).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onAllNodes(request).assertCountEquals(0)
        rule.onNodeWithText("The Gorge").assertExists()
        rule.onNodeWithTag("screen-DISCOVER").assertExists()
    }

    // Spole says Seerr is not there for the person any more: the page has nothing to show, so it goes home by itself.
    @Test fun discoverLeavesForHomeWhenSeerrIsGone() {
        val spole = openDiscoverPage()
        spole.seerr = "none"
        val container = (rule.activity.application as StovaApplication).container
        runBlocking { container.discover.refresh(container.session.active.value!!.id, SpoleLink(ServerKind.EMBY, "eilev"), force = true) }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-HOME").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onAllNodesWithTag("screen-DISCOVER").assertCountEquals(0)
    }

    // The profile names no Spole person any more and has no account to ask as: the page goes home by itself.
    @Test fun discoverLeavesForHomeWithoutASpolePerson() {
        openDiscoverPage()
        val container = (rule.activity.application as StovaApplication).container
        runBlocking { container.profiles.update(container.session.active.value!!.id) { it.copy(spole = null) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-HOME").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onAllNodesWithTag("screen-DISCOVER").assertCountEquals(0)
    }

    /** The Oppdag page opened from Home's tile, with Spole answering for the profile's person and the page's first card in focus. */
    private fun openDiscoverPage(): DiscoverSpole {
        val container = (rule.activity.application as StovaApplication).container
        val spole = DiscoverSpole()
        shadowOf(rule.activity.packageManager).installPackage(PackageInfo().apply { packageName = "app.reelstack" })
        ShadowContentResolver.registerProviderInternal("app.reelstack.launcher", spole)
        runBlocking { container.profiles.update(container.session.active.value!!.id) { it.copy(spole = SpoleLink(ServerKind.EMBY, "eilev")) } }
        val tile = hasContentDescription("Oppdag") and hasClickAction()
        rule.waitUntil(10_000) { rule.onAllNodes(tile).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(tile).performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("screen-DISCOVER").assertExists()
        rule.waitUntil(5_000) { rule.onAllNodes(hasContentDescription("Heat") and hasClickAction() and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        return spole
    }

    /** Spole's launcher provider answering Discover with one popular title; [seerr] is what it says the person may do. */
    private class DiscoverSpole : ContentProvider() {
        @Volatile var seerr = "personal"
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? =
            when (uri.path) {
                "/discover" -> MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop", "status", "community", "genres")).apply {
                    if (seerr != "none") addRow(arrayOf<Any?>("trending", "movie", 949, "Heat", 1995, null, null, null, 5, 8.3, "Crime"))
                }
                // One request the person follows, downloading.
                "/discover/requests" -> MatrixCursor(arrayOf("type", "tmdb", "title", "poster", "backdrop", "stage", "percent", "seasons", "request_id", "is4k")).apply {
                    if (seerr != "none") addRow(arrayOf<Any?>("movie", 950396, "The Gorge", null, null, "downloading", 42, "", 7, 0))
                }
                else -> null
            }?.apply { setExtras(Bundle().apply { putString("seerr", seerr) }) }
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
