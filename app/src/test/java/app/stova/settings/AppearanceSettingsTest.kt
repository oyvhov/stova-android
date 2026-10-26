package app.stova.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AppearanceSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun appBackgroundHasARealPercentageRange() {
        compose.setContent { StovaTheme { IconPackSettingsContent(AppearancePrefs(), emptyList()) {} } }
        compose.onNodeWithText("Bakgrunn på appkort")
            .assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                androidx.compose.ui.semantics.ProgressBarRangeInfo(100f, 0f..100f, 99)))
    }

    @Test fun backgroundOffersLibraryAndAgeControls() {
        compose.setContent { StovaTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppearanceSettingsContent(AppearancePrefs(), category = AppearanceCategory.BACKGROUND, includePreview = false) {}
            }
        } }
        compose.onNodeWithText("Bibliotek til bakgrunnen").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Høgaste aldersgrense").performScrollTo().assertIsDisplayed()
    }

    @Test fun lowerAgeCanBeChosenWithoutTemporarilyAllowingAdultArt() {
        var prefs by mutableStateOf(AppearancePrefs())
        val changes=mutableListOf<Int?>()
        compose.setContent {StovaTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            AppearanceSettingsContent(prefs,category=AppearanceCategory.BACKGROUND,includePreview=false) {
                prefs=it;changes+=it.backgroundFilter.maxAge
            }
        }}}
        compose.onNodeWithText("Høgaste aldersgrense").performScrollTo().performClick()
        compose.onNodeWithText("Til og med 6 år").performScrollTo().performClick()
        assertEquals(listOf(6),changes)
        compose.onNodeWithText("Høgaste aldersgrense").assertIsFocused()
    }

    @Test fun globalAndRowSizesOfferTheSameSixChoicesWhileHeadingsKeepThree() {
        var prefs by mutableStateOf(AppearancePrefs())
        var style by mutableStateOf(RowStyle())
        var rowMode by mutableStateOf(false)
        compose.setContent { StovaTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (rowMode) app.stova.home.RowStyleChoices(style, RowFormat.WIDE, prefs.cardSize, globalTitles = prefs.showTitles) { style = it }
                else AppearanceSettingsContent(prefs, category = AppearanceCategory.CARDS, includePreview = false) { prefs = it }
            }
        } }
        val sizes = listOf("Kompakt" to 224, "Middels" to 248, "Stor" to 296,
            "Ekstra stor" to 344, "Ekstra liten" to 152, "Liten" to 200)
        for (row in listOf(false, true)) {
            compose.runOnIdle { rowMode = row }
            val label = if (row) "Storleik for rada" else "Kortstorleik"
            sizes.forEach { (name, width) ->
                compose.onNode(hasText(label) and hasText(name)).performScrollTo().assertIsDisplayed()
                assertEquals(width, if (row) (style.size ?: prefs.cardSize).width else prefs.cardSize.width)
                compose.onNodeWithText(label).performClick()
            }
        }
        val heading = "Storleik på overskrifta"
        for (name in listOf("Middels", "Stor", "Liten", "Middels")) {
            compose.onNode(hasText(heading) and hasText(name)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(heading).performClick()
        }
    }

    @Test fun projectivyStartsTransparentAndCardBackgroundCanBeEnabledAgain() {
        var prefs by mutableStateOf(AppearancePrefs(appTileBackgroundOpacity = 70))
        compose.setContent { StovaTheme(appearance=prefs) {
            IconPackSettingsContent(prefs,listOf(app.stova.apps.IconPack(app.stova.apps.ProjectivyIcons.PACKAGE,"Projectivy Icon Pack",119))) { prefs=it }
        } }
        compose.onNodeWithText("Ikonpakke").performClick()
        assertEquals(app.stova.apps.ProjectivyIcons.PACKAGE,prefs.iconPack)
        assertFalse(prefs.appTileBackground)
        compose.onNodeWithText("Bakgrunn på appkort").performSemanticsAction(SemanticsActions.SetProgress) { it(43f) }
        assertTrue(prefs.appTileBackground)
        assertEquals(43, prefs.appBackgroundPercent)
    }

    @Test fun numericChoicesUseSlidersAndChangeOnlyTheirOwnValues() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            AppearanceSettingsContent(prefs, includePreview = false) { prefs = it }
        } } }
        listOf("Hjørne" to 17, "Kortavstand" to 11, "Bakgrunnsdemping" to 63, "Spotlight-tid" to 23).forEach { (label, value) ->
            compose.onNodeWithText(label).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(value.toFloat()) }
        }
        assertEquals(AppearancePrefs(cornerRadiusDp = 17, cardSpacingDp = 11, backdropDimmingPercent = 63, spotlightSeconds = 23), prefs)
    }

    @Test fun homePresentationAndPositionAreVisibleAndChangeOnlyTheirOwnPreference() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppearanceSettingsContent(prefs, category = AppearanceCategory.HERO, includePreview = false) { prefs = it }
            }
        } }
        compose.onNodeWithText("Synlege rader").performClick()
        assertEquals(HomeRows.TWO, prefs.homeRows)
        compose.onNodeWithText("Plassering av radene").performScrollTo().performClick()
        assertEquals(HomeRowInset.BALANCED, prefs.homeRowInset)
        assertEquals(ArtworkSize.COMPACT, prefs.cardSize)
        assertEquals(HeroSize.COMPACT, prefs.heroSize)
    }

    @Test fun everyChoiceChangesItsPreferenceWithDpadAndOk() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) {
            Column(Modifier.verticalScroll(rememberScrollState())) { AppearanceSettingsContent(prefs) { prefs = it } }
        } }
        val choices = listOf("Herostorleik", "Logo eller tittel", "Omtale i heroen", "Sjangrar i heroen", "Radavstand",
            "Vis årstal i heroen", "Vis aldersgrense i heroen", "Vis vurderingar i heroen", "Vis ny sesong i heroen",
            "Kortstorleik", "Titlar under kort", "Appstil", "Nyleg lagt til · Filmar",
            "Aksentfarge", "Fokuseffekt", "Klokkeformat", "Framdriftsstrek", "Bakgrunnskjelde",
            "Titlar til bakgrunnen", "Roleg bakgrunnsrørsle", "Vis dato", "Vis HA-brikker", "Vis julenedteljing",
            "Vis Smarthus-knapp", "Vis profilbilete", "Klokkestorleik")
        choices.forEach { label ->
            val row = compose.onNodeWithText(label).performScrollTo()
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.assertIsFocused()
            val before = prefs
            row.performKeyInput { pressKey(Key.DirectionRight) }
            assertNotEquals("Right changes $label", before, prefs)
            val stepped = prefs
            // Left leaves the row for the section list; it never changes a value (review finding 3).
            row.performKeyInput { pressKey(Key.DirectionLeft) }
            assertEquals("Left keeps $label", stepped, prefs)
            compose.runOnIdle { prefs = before }
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.performKeyInput { pressKey(Key.Enter) }
            assertEquals("OK steps $label like Right", stepped, prefs)
        }
        assertEquals(ArtworkSize.MEDIUM, prefs.cardSize)
        assertFalse(prefs.showTitles)
        assertEquals(AppStyle.ICON, prefs.appStyle)
        assertEquals(MovieStyle.WIDE, prefs.movieStyle)
        assertEquals(HeroSize.LARGE, prefs.heroSize)
        assertTrue(prefs.showOverview)
        assertFalse(prefs.showGenres)
        assertEquals(Accent.OCEAN, prefs.accent)
        assertEquals(FocusEffect.ZOOM, prefs.focusEffect)
        assertEquals(ClockMode.H12, prefs.clock)
        assertEquals(RowSpacing.AIRY, prefs.rowSpacing)
    }

    @Test fun pageHasEightExplainedGroupsAndPreviewChangesWithHero() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) {
            Column(Modifier.verticalScroll(rememberScrollState())) { AppearanceSettingsContent(prefs) { prefs = it } }
        } }
        listOf("Tema", "Heim og hero", "Kort og rader", "Bakgrunn", "Fokus", "Toppfelt", "Ikon", "Profil").forEach {
            compose.onAllNodesWithText(it)[0].performScrollTo().assertExists()
        }
        compose.onNodeWithTag("appearance-preview").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("preview-hero",useUnmergedTree=true).assertExists()
        compose.runOnIdle { prefs = prefs.copy(heroSize = HeroSize.OFF) }
        compose.onNodeWithTag("preview-hero",useUnmergedTree=true).assertDoesNotExist()
    }
}
