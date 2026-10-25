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

    @Test fun everyChoiceChangesItsPreferenceWithDpadAndOk() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) {
            Column(Modifier.verticalScroll(rememberScrollState())) { AppearanceSettingsContent(prefs) { prefs = it } }
        } }
        val choices = listOf("Herostorleik", "Omtale i heroen", "Sjangrar i heroen", "Spotlight-tid", "Radavstand",
            "Kortstorleik", "Titlar under kort", "Appstil", "Nyleg lagt til · Filmar", "Hjørne", "Bakgrunnsdemping",
            "Aksentfarge", "Fokuseffekt", "Klokkeformat")
        choices.forEach { label ->
            val row = compose.onNodeWithText(label).performScrollTo()
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.assertIsFocused()
            val before = prefs
            row.performKeyInput { pressKey(Key.DirectionRight) }
            assertNotEquals("Right changes $label", before, prefs)
            row.performKeyInput { pressKey(Key.DirectionLeft) }
            assertEquals("Left reverses $label", before, prefs)
            row.performKeyInput { pressKey(Key.Enter) }
            assertNotEquals("OK changes $label", before, prefs)
        }
        assertEquals(CardSize.LARGE, prefs.cardSize)
        assertTrue(prefs.showTitles)
        assertEquals(AppStyle.ICON, prefs.appStyle)
        assertEquals(MovieStyle.WIDE, prefs.movieStyle)
        assertEquals(HeroSize.LARGE, prefs.heroSize)
        assertTrue(prefs.showOverview)
        assertFalse(prefs.showGenres)
        assertEquals(SpotlightTime.SLOW, prefs.spotlightTime)
        assertEquals(BackdropDimming.DARK, prefs.backdropDimming)
        assertEquals(Corners.SHARP, prefs.corners)
        assertEquals(Accent.OCEAN, prefs.accent)
        assertEquals(FocusEffect.ZOOM, prefs.focusEffect)
        assertEquals(ClockMode.H12, prefs.clock)
        assertEquals(RowSpacing.AIRY, prefs.rowSpacing)
    }
}
