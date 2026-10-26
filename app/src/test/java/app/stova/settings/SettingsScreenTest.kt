package app.stova.settings

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.AppContainer
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container: AppContainer get() = (rule.activity.application as StovaApplication).container

    private fun profile(name: String) = runBlocking { container.profiles.file.first().profiles.first { it.name == name } }

    private fun openSettings() {
        rule.waitForIdle()
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.waitForIdle()
    }

    private fun tab(label: String) = rule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    /** The way back from an Appearance category: it carries the section name, like the tab. */
    private fun breadcrumb() = rule.onNode(hasText("Heim og utsjånad") and hasClickAction() and
        !SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    @Test fun enteringSettingsOpensHomeAndAppearanceDirectly() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Heim og utsjånad").assertIsFocused()
        rule.onNode(hasText("Tema") and hasClickAction()).assertExists()
        rule.onNodeWithText("Oversikt").assertDoesNotExist()
        rule.onNodeWithText("Set PIN").assertDoesNotExist()
    }

    @Test fun rowsOnHomeLiveInsideHomeAndAppearance() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        rule.onNode(hasText("Rader på Heim") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Bakgrunnsrotasjon").assertIsFocused()
        back()
        rule.onNode(hasText("Rader på Heim") and hasClickAction()).assertIsFocused()
    }

    @Test fun libraryDialogReturnsToItsControlAndSavesExplicitEmptySelection() {
        runBlocking {container.session.enter(container.profiles.create("A",AvatarColor.LIME,false).id)}
        openSettings()
        rule.onNode(hasText("Heim og utsjånad") and SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Tab)).performClick()
        rule.onNode(hasText("Bakgrunn") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Bibliotek til bakgrunnen").performScrollTo().performClick()
        rule.onNodeWithText("Avbryt").performClick()
        rule.onNodeWithText("Bibliotek til bakgrunnen").assertIsFocused()
        rule.onNodeWithText("Bibliotek til bakgrunnen").performKeyInput {pressKey(Key.DirectionCenter)}
        rule.onNodeWithText("Fjern alle val").performClick()
        rule.onNodeWithText("Bruk utvalet").performClick()
        rule.onNodeWithText("Bibliotek til bakgrunnen").assertIsFocused()
        rule.waitUntil(5_000) {profile("A").appearance.backgroundFilter.libraries==emptySet<app.stova.profiles.BackgroundLibraryRef>()}
    }

    @Test fun leftFromAScrolledSettingReturnsToItsOwnSection() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        val appearance=hasText("Heim og utsjånad") and SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Tab)
        rule.onNode(appearance).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.onNode(hasText("Ikon") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Ikonpakke").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.onNodeWithText("Ikonpakke").performKeyInput { keyDown(Key.DirectionLeft);keyUp(Key.DirectionLeft) }
        rule.onNode(appearance).assertIsFocused()
        rule.onNodeWithText("Ikonpakke").assertExists()
    }

    @Test fun rightFromTheSectionEntersItsFirstControl() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        val appearance=hasText("Heim og utsjånad") and SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Tab)
        rule.onNode(appearance).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.onNode(appearance).performKeyInput { keyDown(Key.DirectionRight);keyUp(Key.DirectionRight) }
        rule.onNode(hasText("Tema") and hasClickAction()).assertIsFocused()
    }

    @Test fun reduceMotionPersists() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Fokus") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Reduser rørsle").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("A").appearance.reduceMotion }
    }

    @Test fun sliderLastValuePersistsWhenLeavingImmediately() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Ikon") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Bakgrunn på appkort").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(43f) }
        // Android's back dispatcher owns page navigation; raw Compose keys only reach the category.
        // Category, then the section menu, then out of Settings: one step each.
        repeat(3) {
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.waitUntil(5_000) { profile("A").appearance.appTileBackgroundOpacity == 43 }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Ikon") and hasClickAction()).performScrollTo().performClick()
        rule.onNode(hasText("Bakgrunn på appkort") and hasText("43 %")).performScrollTo().assertIsDisplayed()
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test fun backFromContentGoesToTheSectionMenuBeforeLeavingSettings() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Profilar").performClick()
        val homeTab = tab("Profilar")
        homeTab.assertIsNotFocused()
        back()
        homeTab.assertIsFocused()
        rule.onNodeWithTag("screen-SETTINGS").assertExists()
        back()
        rule.onNodeWithTag("screen-HOME").assertExists()
    }

    @Test fun sectionsDoNotJumpToEachOther() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        rule.onNode(hasText("Rader på Heim") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Utsjånad på framsida").assertDoesNotExist()
    }

    @Test fun appearanceStartsWithCategoriesAndOpensOnlyTheChosenControls() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Tema") and hasClickAction()).assertIsFocused()
        rule.onNodeWithText("Kortstorleik").assertDoesNotExist()
        rule.onNode(hasText("Fokus") and hasClickAction()).performClick()
        rule.onNodeWithText("Fokuseffekt").assertIsFocused()
        rule.onNodeWithText("Klokkeformat").assertDoesNotExist()
        rule.onNodeWithText("Kortstorleik").assertDoesNotExist()
        breadcrumb().performClick()
        rule.onNode(hasText("Fokus") and hasClickAction()).assertIsFocused()
    }

    @Test fun appearanceCategoryCanBeOpenedAndBackedOutWithTheRemote() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Tema") and hasClickAction()).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNode(hasText("Tema") and hasClickAction()).assertIsFocused()
        rule.onRoot().performKeyInput { pressKey(Key.Back) }
        rule.onNode(hasText("Tema") and hasClickAction()).assertIsFocused()
        rule.onNode(hasText("Toppfelt") and hasClickAction()).assertIsDisplayed()
        rule.onNodeWithText("Klokkeformat").assertDoesNotExist()
    }

    @Test fun appearanceChoicesPersistOnlyForTheActiveProfile() {
        runBlocking {
            container.profiles.create("B", AvatarColor.OCEAN, false)
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
        }
        openSettings()
        tab("Heim og utsjånad").performClick()
        rule.onNode(hasText("Kort og rader") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Kortstorleik").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("A").appearance.cardSize == app.stova.profiles.ArtworkSize.MEDIUM }
        assertEquals(app.stova.profiles.ArtworkSize.COMPACT, profile("B").appearance.cardSize)
    }

    @Test fun adultReachesAndroidSettingsFromStovaSettings() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("System").performClick()
        rule.onNodeWithText("Android").performScrollTo().performClick()
        val started = org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity
        assertEquals(android.provider.Settings.ACTION_SETTINGS, started?.action)
    }

    // On the Shield the sections only changed on OK; on a TV the page follows focus.
    @Test fun sectionFollowsFocus() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        rule.onNodeWithText("Set PIN").assertDoesNotExist()
        tab("Profilar").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.waitForIdle()
        rule.onNodeWithText("Set PIN").assertExists()
    }

    // Switching a section leaves a single pane in the focus tree and keeps focus on its tab.
    @Test fun sectionSwitchKeepsFocusOnTheTabWithoutLeavingOldControls() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Profilar").performClick()
        rule.onNodeWithText("Set PIN").assertExists()
        val themeCard = hasText("Tema") and hasClickAction()
        rule.onNode(themeCard).assertDoesNotExist()
        val tab = hasText("Heim og utsjånad") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        rule.mainClock.autoAdvance = false
        rule.onNode(tab).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Set PIN").assertDoesNotExist()
        rule.onNode(themeCard).assertExists()
        rule.onNode(tab).assertIsFocused()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Set PIN").assertDoesNotExist()
        rule.onNode(themeCard).assertExists()
        rule.onNode(tab).assertIsFocused()
    }

    @Test fun adultCanEditKidAppearanceWithoutChangingAdult() {
        runBlocking {
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
            container.profiles.create("K", AvatarColor.ROSE, true)
        }
        openSettings()
        tab("Profilar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("K").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("K").performScrollTo().performClick()
        rule.onNodeWithText("Utsjånad").performScrollTo().performClick()
        rule.onNode(hasText("Kort og rader") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithText("Kortstorleik").performScrollTo().performClick()
        rule.waitUntil(5_000) { profile("K").appearance.cardSize == app.stova.profiles.ArtworkSize.MEDIUM }
        assertEquals(app.stova.profiles.ArtworkSize.COMPACT, profile("A").appearance.cardSize)
        breadcrumb().performClick()
        rule.onNode(hasText("Rader på Heim") and hasClickAction()).performScrollTo().performClick()
        rule.onAllNodesWithText("Kortformat for rada")[0].performScrollTo().performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) { profile("K").home.rowFormats[app.stova.home.HomeRowKey.APPS]?.format == app.stova.profiles.RowFormat.LARGE_WIDE }
        assertEquals(emptyMap<app.stova.home.HomeRowKey, app.stova.profiles.RowStyle>(), profile("A").home.rowFormats)
    }

    @Test fun kidSeesOnlySwitchProfile() {
        runBlocking {
            container.profiles.create("A", AvatarColor.LIME, false)
            container.session.enter(container.profiles.create("K", AvatarColor.ROSE, true).id)
        }
        openSettings()
        rule.onNodeWithText("Heim og utsjånad").assertDoesNotExist()
        assertEquals(1, rule.onAllNodesWithText("Byt profil").fetchSemanticsNodes().size) // the one button
    }

    @Test fun allowAppForKid() {
        val pm = shadowOf(rule.activity.packageManager)
        val component = ComponentName("demo.banan", "demo.banan.Main")
        pm.addActivityIfNotPresent(component).nonLocalizedLabel = "Banan"
        pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) })
        runBlocking {
            container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id)
            container.profiles.create("K", AvatarColor.ROSE, true)
        }
        openSettings()
        tab("Profilar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("K").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("K").performScrollTo().performClick()
        rule.onNodeWithText("Tillatne appar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Banan").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Banan").performClick()
        rule.waitUntil(5_000) { "demo.banan/demo.banan.Main" in profile("K").apps.kidAllowed }
    }

    // Review Important 6: a new PIN must be typed twice before it is saved.
    @Test fun newPinNeedsConfirmation() {
        runBlocking { container.session.enter(container.profiles.create("A", AvatarColor.LIME, false).id) }
        openSettings()
        tab("Profilar").performClick()
        rule.onNodeWithText("Set PIN").performClick()
        "2468".forEach { rule.onNodeWithText(it.toString()).performClick() }
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Stadfest ny PIN").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(null, profile("A").pin)
        "2468".forEach { rule.onNodeWithText(it.toString()).performClick() }
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(5_000) { profile("A").pin != null }
    }

    @Test fun changingPinRequiresCurrentPin() {
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            container.profiles.setPin(a.id, "1357")
            container.session.enter(a.id)
        }
        openSettings()
        tab("Profilar").performClick()
        rule.onNodeWithText("Endre PIN").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }
}
