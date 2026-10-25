package app.stova.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AppearanceProfileActionsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun resetRequiresConfirmationAndCopySelectsSource() {
        var resets=0; var copied:String?=null
        compose.setContent { StovaTheme { AppearanceProfileActions(listOf(Profile("a","Vaksen",AvatarColor.LIME)), { resets++ }, { copied=it }) } }
        compose.onNodeWithText("Tilbakestill utsjånad").performClick()
        assertEquals(0,resets)
        compose.onNodeWithText("Avbryt").assertIsFocused().performClick()
        assertEquals(0,resets)
        compose.onNodeWithText("Tilbakestill utsjånad").performClick()
        compose.onAllNodesWithText("Tilbakestill utsjånad").filter(hasAnyAncestor(isDialog()))[0].performClick()
        assertEquals(1,resets)
        compose.onNodeWithText("Kopier utsjånad frå profil").performClick()
        compose.onNodeWithText("Vaksen").performClick()
        assertEquals("a",copied)
    }
}
