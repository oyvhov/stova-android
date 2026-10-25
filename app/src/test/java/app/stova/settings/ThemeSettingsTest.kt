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
import app.stova.core.theme.*
import app.stova.profiles.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class ThemeSettingsTest {
    @get:Rule val compose=createComposeRule()
    @Test fun allMoodsAccentsSeasonsAndDecorationsAreDpadChoicesWithPreview() {
        var p by mutableStateOf(AppearancePrefs(seasonChoice=SeasonChoice.NONE))
        compose.setContent { StovaTheme(appearance=p) { Column(Modifier.verticalScroll(rememberScrollState())) { AppearanceSettingsContent(p) { p=it } } } }
        fun step(label: String,key: Key=Key.DirectionRight) {
            val row=compose.onNode(hasText(label) and hasClickAction()).performScrollTo()
            row.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            row.performKeyInput { pressKey(key) }
        }
        // The mood row holds the plain moods only; Jul and Halloween live in the season row.
        for(mood in listOf(VisualTheme.MIDNIGHT,VisualTheme.CINEMA,VisualTheme.PLUM,VisualTheme.FOREST)) {
            step("Tema"); assertEquals(mood,p.visualTheme)
        }
        assertEquals(SeasonChoice.NONE,p.seasonChoice)
        compose.runOnIdle { p=p.copy(accent=Accent.LIME) }
        for(accent in Accent.entries.drop(1)+Accent.LIME) {
            step("Aksentfarge"); assertEquals(accent,p.accent)
        }
        step("Sesong"); assertEquals(SeasonChoice.CALENDAR,p.seasonChoice)
        step("Sesong"); assertEquals(SeasonChoice.CHRISTMAS,p.seasonChoice)
        assertEquals(VisualTheme.NOEL,p.visualTheme)
        step("Sesong"); assertEquals(SeasonChoice.HALLOWEEN,p.seasonChoice)
        step("Sesong"); assertEquals(SeasonChoice.NONE,p.seasonChoice)
        assertEquals(VisualTheme.FOREST,p.visualTheme)
        step("Sesongpynt"); assertEquals(SeasonalDecor.OFF,p.seasonalDecor)
        step("Sesongpynt",Key.DirectionLeft); assertEquals(SeasonalDecor.OFF,p.seasonalDecor)
        step("Sesongpynt",Key.Enter); assertEquals(SeasonalDecor.CALM,p.seasonalDecor)
        compose.onNodeWithText("Førehandsvising").assertExists()
    }
}
