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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.media.model.ServerKind
import app.stova.profiles.SeasonChoice
import app.stova.profiles.SpoleLink
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowToast

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

    // The Home service bringing Stova back after its own restart is no press: the page stays.
    @Test fun theHomeServiceComingBackMovesNothing() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Opne Appar").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Opne Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.activityRule.scenario.onActivity {
            it.onNewIntent(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
                .putExtra(app.stova.homebutton.HomeButtonService.EXTRA_RESTORE, true))
        }
        rule.onNodeWithTag("screen-APPS").assertExists()
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

    @Test fun sonosIsOptInAndBackReturnsToHome() {
        rule.onAllNodesWithText("Sonos").assertCountEquals(0)
        val container = (rule.activity.application as StovaApplication).container
        runBlocking { container.household.update { it.copy(sonos = app.stova.sonos.SonosPrefs(enabled = true)) } }
        rule.waitForIdle()
        rule.onAllNodesWithText("Sonos").assertCountEquals(0) // Still needs HA.
        runBlocking { container.household.update { it.copy(haBaseUrl = "http://ha.local:8123") } }
        rule.waitForIdle()
        rule.onAllNodesWithText("Sonos")[0].performClick()
        rule.onNodeWithTag("screen-SONOS").assertExists()
        rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
        runBlocking { container.household.update { it.copy(sonos = it.sonos.copy(enabled = false)) } }
        rule.waitForIdle()
        rule.onAllNodesWithText("Sonos").assertCountEquals(0)
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

    // Opened from the Apps page's tile, Back from Discover goes back to the Apps page; Apps opened from Settings still returns there.
    @Test fun discoverFromTheAppsPageReturnsThere() {
        val container = (rule.activity.application as StovaApplication).container
        shadowOf(rule.activity.packageManager).installPackage(PackageInfo().apply { packageName = "app.reelstack" })
        ShadowContentResolver.registerProviderInternal("app.reelstack.launcher", DiscoverSpole())
        runBlocking { container.profiles.update(container.session.active.value!!.id) { it.copy(spole = SpoleLink(ServerKind.EMBY, "eilev")) } }
        val tile = hasContentDescription("Oppdag") and hasClickAction()
        // Home's row says Spole has answered for the person.
        rule.waitUntil(10_000) { rule.onAllNodes(tile).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
        rule.onNodeWithText("Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.waitUntil(5_000) { rule.onAllNodes(tile).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(tile).performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("screen-DISCOVER").assertExists()
        rule.waitUntil(5_000) { rule.onAllNodes(hasContentDescription("Heat") and hasClickAction() and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
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

    // Opened from the rows, the system puts focus on «Opne i Spole» while Seerr's page is on its way. That is not the viewer
    // moving: when the page comes, «Be om» takes focus without a press.
    @Test fun discoverTitleFocusesRequestWhenDetailsArrive() {
        val spole = openDiscoverPage().apply { titles = true; held = CountDownLatch(1) }
        rule.onNode(hasContentDescription("Heat") and hasClickAction() and isFocused()).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Opne i Spole") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onAllNodesWithText("Be om").assertCountEquals(0)
        spole.held!!.countDown()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Be om") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
    }

    // …but a D-pad move to a related card before the page comes is the viewer's, and focus stays on the card.
    @Test fun discoverTitleKeepsFocusTheViewerMovedBeforeDetails() {
        val spole = openDiscoverPage().apply { titles = true; held = CountDownLatch(1) }
        rule.onNode(hasContentDescription("Heat") and hasClickAction() and isFocused()).performSemanticsAction(SemanticsActions.OnClick)
        val ronin = hasContentDescription("Ronin") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(ronin).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Opne i Spole") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText("Opne i Spole") and isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNode(ronin).assertIsFocused()
        spole.held!!.countDown()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Be om") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onNode(hasText("Be om") and hasClickAction()).assertIsNotFocused()
        rule.onNode(ronin).assertIsFocused()
    }

    // «Be om» opens the sheet and Back closes it without sending; «Send førespurnad» sends the film through Spole and says so.
    // The page shows the person's own request only once Spole has asked Seerr for their requests again, so «Trekk tilbake»
    // proves that came before the title's page was asked for anew. The button pressed keeps focus while Spole is asked, and
    // when it goes the first button there takes focus.
    @Test fun discoverRequestGoesThroughSpole() {
        val spole = openDiscoverPage().apply { titles = true }
        rule.onNode(hasContentDescription("Heat") and hasClickAction() and isFocused()).performSemanticsAction(SemanticsActions.OnClick)
        val ask = hasText("Be om") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(ask and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(ask).performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Legg filmen til i biblioteket ditt.").assertExists()
        rule.onNodeWithText("Send førespurnad").performKeyInput { pressKey(Key.Back) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Send førespurnad").fetchSemanticsNodes().isEmpty() }
        rule.onNode(ask).assertIsFocused()
        assertTrue(spole.calls.isEmpty())
        rule.onNode(ask).performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Send førespurnad").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Trekk tilbake") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        val (method, extras) = spole.calls.single()
        assertEquals("discover_request", method)
        assertEquals("movie", extras!!.getString("type"))
        assertEquals(949, extras.getInt("tmdb"))
        assertArrayEquals(intArrayOf(), extras.getIntArray("seasons"))
        assertEquals("Sendt til Seerr.", ShadowToast.getTextOfLatestToast())
        val refreshed = spole.asked.indexOfLast { it.path == "/discover/requests" && it.getQueryParameter("refresh") == "1" }
        assertTrue(refreshed in 0 until spole.asked.indexOfLast { it.path == "/discover/title" })
        rule.onAllNodes(ask).assertCountEquals(0)
    }

    // «Trekk tilbake» on the person's own request asks first; the sheet's «Trekk tilbake» sends the request's id through Spole,
    // and once Spole has asked Seerr the page offers «Be om» again, in focus.
    @Test fun discoverWithdrawalGoesThroughSpole() {
        val spole = openDiscoverPage().apply { titles = true }
        val request = hasContentDescription("The Gorge") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(request).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(request).performSemanticsAction(SemanticsActions.OnClick)
        val withdraw = hasText("Trekk tilbake") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(withdraw and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(withdraw).performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Trekk tilbake «The Gorge»?").assertExists()
        rule.onNode(withdraw and hasAnySibling(hasText("Behald"))).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Be om") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        val (method, extras) = spole.calls.single()
        assertEquals("discover_withdraw", method)
        assertEquals(7, extras!!.getInt("request_id"))
        assertEquals("Førespurnaden er trekt tilbake.", ShadowToast.getTextOfLatestToast())
        rule.onAllNodes(withdraw).assertCountEquals(0)
    }

    // «Søk» opens search with the field in focus. Spole is asked once typing has paused, with what was typed and page 1; text
    // typed before the pause is never asked for on its own. «Hent fleire» asks for page 2 and adds it, a result opens its title,
    // and Back from there finds the field's text and the card again without asking Spole twice.
    @Test fun discoverSearchAsksSpoleOnceTypingPauses() {
        val spole = openDiscoverPage()
        val searches = { spole.asked.filter { it.path == "/discover/search" } }
        rule.onNode(hasText("Søk") and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        val field = hasSetTextAction()
        rule.waitUntil(5_000) { rule.onAllNodes(field and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.onNode(field).performTextInput("He")
        rule.mainClock.advanceTimeBy(200)
        rule.onNode(field).performTextInput("at")
        rule.mainClock.advanceTimeBy(300)
        assertTrue(searches().isEmpty())
        rule.mainClock.advanceTimeBy(200)
        rule.mainClock.autoAdvance = true
        val heat = hasContentDescription("Heat") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(heat).fetchSemanticsNodes().isNotEmpty() }
        val first = searches().single()
        assertEquals("Heat", first.getQueryParameter("q"))
        assertEquals("1", first.getQueryParameter("page"))
        // Down from the field to the first title, along the row to «Hent fleire» and OK: page 2 comes, its first title in focus.
        rule.onNode(field).performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNode(heat).assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNode(hasContentDescription("The Heat") and hasClickAction()).assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNode(hasContentDescription("Hent fleire") and hasClickAction()).assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        val wave = hasContentDescription("Heat Wave") and hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(wave and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, searches().size)
        assertEquals("Heat", searches()[1].getQueryParameter("q"))
        assertEquals("2", searches()[1].getQueryParameter("page"))
        rule.onNode(heat).assertExists()
        rule.onAllNodes(hasContentDescription("Hent fleire")).assertCountEquals(0)
        // OK on a result opens its title; Back returns to the search as it was left.
        rule.onNode(wave).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        rule.onAllNodes(wave).assertCountEquals(0)
        rule.onNodeWithText("Heat Wave").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(5_000) { rule.onAllNodes(wave and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(field).assert(hasText("Heat"))
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertEquals(2, searches().size)
    }

    // Spole gives no answer to a query's first page: search says Seerr did not answer, rather than stand blank.
    @Test fun discoverSearchSaysSoWhenSeerrDoesNotAnswer() {
        openDiscoverPage()
        rule.onNode(hasText("Søk") and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        val field = hasSetTextAction()
        rule.waitUntil(5_000) { rule.onAllNodes(field and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(field).performTextInput("Dune")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Seerr svara ikkje.").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Ingen treff", substring = true).assertCountEquals(0)
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

    /**
     * Spole's launcher provider answering Discover with one popular title; [seerr] is what it says the person may do. With
     * [titles] it answers a title's page too, once [held] lets it: Heat still to be asked for, and The Gorge the person's own
     * request, each with Ronin as a related title. A search finds two films on its first page and one more on its second.
     * Every call is answered `ok` and kept in [calls], every question in [asked].
     * What was sent shows on a title's page only once Spole has been asked to look at the person's requests in Seerr again,
     * as Seerr gives a new request its id only then.
     */
    private class DiscoverSpole : ContentProvider() {
        @Volatile var seerr = "personal"
        @Volatile var titles = false
        @Volatile var held: CountDownLatch? = null
        val calls = CopyOnWriteArrayList<Pair<String, Bundle?>>()
        val asked = CopyOnWriteArrayList<Uri>()
        // Seerr has what Stova sent, and Spole has looked at the person's requests since.
        @Volatile private var sent = false
        @Volatile private var refreshed = false
        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? {
            asked += uri
            return when (uri.path) {
                "/discover" -> MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop", "status", "community", "genres")).apply {
                    if (seerr != "none") addRow(arrayOf<Any?>("trending", "movie", 949, "Heat", 1995, null, null, null, 5, 8.3, "Crime"))
                }
                // One request the person follows, downloading.
                "/discover/requests" -> MatrixCursor(arrayOf("type", "tmdb", "title", "poster", "backdrop", "stage", "percent", "seasons", "request_id", "is4k")).apply {
                    if (sent && uri.getQueryParameter("refresh") == "1") refreshed = true
                    if (seerr != "none") addRow(arrayOf<Any?>("movie", 950396, "The Gorge", null, null, "downloading", 42, "", 7, 0))
                }
                "/discover/title" -> if (titles) { held?.await(5, TimeUnit.SECONDS); title(uri.getQueryParameter("tmdb")) } else null
                "/discover/related" -> if (titles) MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "status")).apply {
                    addRow(arrayOf<Any?>("similar", "movie", 8195, "Ronin", 1998, 1))
                } else null
                // Two films on page 1 and a third on page 2, the last; nothing at all for «Dune», as when Spole could not ask Seerr.
                "/discover/search" -> if (uri.getQueryParameter("q") == "Dune") null else MatrixCursor(arrayOf("row", "type", "tmdb", "title", "year", "status")).apply {
                    if (uri.getQueryParameter("page") == "1") {
                        addRow(arrayOf<Any?>("search", "movie", 949, "Heat", 1995, 5))
                        addRow(arrayOf<Any?>("search", "movie", 136795, "The Heat", 2013, 1))
                    } else addRow(arrayOf<Any?>("search", "movie", 1242, "Heat Wave", 2022, 1))
                }
                else -> null
            }?.apply {
                setExtras(Bundle().apply {
                    putString("seerr", seerr)
                    if (uri.path == "/discover/search") uri.getQueryParameter("page")?.toInt()?.let { putInt("page", it); putBoolean("has_more", it == 1) }
                })
            }
        }
        override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
            calls += method to extras
            sent = true
            return Bundle().apply { putString("result", "ok") }
        }
        /** Heat to be asked for until the request has reached its page; The Gorge downloading as the person's own until taken back. */
        private fun title(tmdb: String?) = MatrixCursor(arrayOf("type", "tmdb", "title", "year", "status", "can_request", "request_id", "request_status")).apply {
            when (tmdb) {
                "949" -> addRow(if (refreshed) arrayOf<Any?>("movie", 949, "Heat", 1995, 2, 0, 8, "requested")
                    else arrayOf<Any?>("movie", 949, "Heat", 1995, 1, 1, null, null))
                "950396" -> addRow(if (refreshed) arrayOf<Any?>("movie", 950396, "The Gorge", 2025, 1, 1, null, null)
                    else arrayOf<Any?>("movie", 950396, "The Gorge", 2025, 3, 0, 7, "downloading"))
            }
        }
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
