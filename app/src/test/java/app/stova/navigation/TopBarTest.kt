package app.stova.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class TopBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun focusingLabelsKeepsAllButtonBoundsStable() {
        compose.setContent { StovaTheme {
            TopBar(Profile("a", "A", AvatarColor.LIME), {}, {}, {})
        } }
        val labels = listOf("Appar", "Innstillingar", "Byt profil")
        val before = labels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        compose.onNodeWithText("Appar").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Appar").assertIsFocused()
        compose.onNodeWithText("Appar").performKeyInput {
            keyDown(Key.DirectionRight); keyUp(Key.DirectionRight)
        }
        compose.onNodeWithText("Innstillingar").assertIsFocused()
        val after = labels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        assertEquals(before, after)
    }
}
