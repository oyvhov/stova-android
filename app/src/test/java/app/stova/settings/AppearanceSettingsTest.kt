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

    @Test fun projectivyStartsTransparentAndCardBackgroundCanBeEnabledAgain() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance=prefs) {
            IconPackSettingsContent(prefs,listOf(app.stova.apps.IconPack(app.stova.apps.ProjectivyIcons.PACKAGE,"Projectivy Icon Pack",119))) { prefs=it }
        } }
        compose.onNodeWithText("Ikonpakke").performClick()
        assertEquals(app.stova.apps.ProjectivyIcons.PACKAGE,prefs.iconPack)
        assertFalse(prefs.appTileBackground)
        compose.onNodeWithText("Bakgrunn på appkort").performClick()
        assertTrue(prefs.appTileBackground)
    }

    @Test fun everyChoiceChangesItsPreferenceWithDpadAndOk() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) {
            Column(Modifier.verticalScroll(rememberScrollState())) { AppearanceSettingsContent(prefs) { prefs = it } }
        } }
        val choices = listOf("Herostorleik", "Logo eller tittel", "Omtale i heroen", "Sjangrar i heroen", "Spotlight-tid", "Radavstand",
            "Vis årstal i heroen", "Vis aldersgrense i heroen", "Vis vurderingar i heroen", "Vis ny sesong i heroen",
            "Kortstorleik", "Titlar under kort", "Appstil", "Nyleg lagt til · Filmar", "Hjørne", "Bakgrunnsdemping",
            "Aksentfarge", "Fokuseffekt", "Klokkeformat", "Kortavstand", "Framdriftsstrek", "Bakgrunnskjelde",
            "Spotlight-kjelde", "Roleg bakgrunnsrørsle", "Vis dato", "Vis HA-brikker", "Vis sesongnedteljing", "Vis Appar-knapp",
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
        assertEquals(ArtworkSize.LARGE, prefs.cardSize)
        assertTrue(prefs.showTitles)
        assertEquals(AppStyle.ICON, prefs.appStyle)
        assertEquals(MovieStyle.WIDE, prefs.movieStyle)
        assertEquals(HeroSize.LARGE, prefs.heroSize)
        assertTrue(prefs.showOverview)
        assertFalse(prefs.showGenres)
        assertEquals(SpotlightTime.SLOW, prefs.spotlightTime)
        assertEquals(BackdropDimming.DARK, prefs.backdropDimming)
        assertEquals(CornerRadius.EXTRA_ROUND, prefs.cornerRadius)
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
