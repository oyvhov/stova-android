package app.stova.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import app.stova.profiles.ProfileEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS,fontScale=2f)
class ProfileDialogLayoutTest {
    @get:Rule val compose=createComposeRule()
    @Test fun largeTextCanReachSaveAndCancelWithoutLosingTheForm() {
        var saved=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            EditorDialog({}) {ProfileEditor(Profile("p","Øyvind",AvatarColor.LIME),false,{_,_,_->saved++},{})}
        }}
        compose.onNodeWithText("Endre profil").assertIsDisplayed()
        compose.onNodeWithContentDescription("Lime").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithContentDescription("Lime").performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Lagre").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithText("Lagre").performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Avbryt").assertIsFocused().assertIsDisplayed()
        assertEquals(0,saved)
    }
}
