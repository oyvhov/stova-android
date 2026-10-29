package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.StovaButton
import app.stova.profiles.AppearancePrefs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class AuditSettingsTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test fun addingMediaHasItsOwnTitle() {
        compose.setContent {StovaTheme(reduceMotion=true) {
            AccountsContent(emptyList(),{}) {StovaButton("Vel tenar",{})}
        }}
        compose.onNodeWithText("Legg til konto").performClick()
        compose.onNodeWithText("Legg til mediekonto").assertIsDisplayed()
    }
    @Test fun advancedSetupIsOptionalAndBackClosesOnlyItsPanel() {
        compose.setContent {StovaTheme(reduceMotion=true) {app.stova.homebutton.AdvancedHomeButtonSettings("app.stova.debug")}}
        compose.onNodeWithText("Med adb (valfritt)").assertDoesNotExist()
        compose.onNodeWithText("Avansert oppsett").performClick()
        compose.onNodeWithText("Med adb (valfritt)").assertIsDisplayed()
        compose.onNodeWithText("Lukk").assertIsFocused().performKeyInput {pressKey(Key.Back)}
        compose.onNodeWithText("Med adb (valfritt)").assertDoesNotExist()
    }
    @Test fun iconPreviewInTheHeaderStaysVisibleWhenControlsScroll() {
        compose.setContent {StovaTheme(reduceMotion=true) {
            SettingsLayout(SettingsSection.APPEARANCE,{}) {_,_->
                AppearanceMenu(preview={Box(Modifier.size(304.dp,100.dp).testTag("icons-preview"))}) {category,first->
                    AppearanceSettingsContent(AppearancePrefs(),category=category,firstControl=first,onChange={})
                }
            }
        }}
        compose.onNode(hasText("Ikon") and hasClickAction()).performClick()
        compose.onNodeWithText("Ikonpakke").assertIsFocused()
        val clipped=compose.onNodeWithTag("icons-preview").getBoundsInRoot()
        val full=compose.onNodeWithTag("icons-preview").getUnclippedBoundsInRoot()
        assertEquals(full.top.value,clipped.top.value,1f)
        assertEquals(full.bottom.value,clipped.bottom.value,1f)
        compose.onNodeWithText("Bakgrunn på appkort").performScrollTo()
        compose.onNodeWithTag("icons-preview").assertIsDisplayed()
    }
}
