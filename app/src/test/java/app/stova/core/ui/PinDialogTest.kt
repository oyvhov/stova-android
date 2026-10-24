package app.stova.core.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.PinCheck
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class PinDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun acceptedPinCallsOnAccepted() {
        var accepted = false
        compose.setContent {
            StovaTheme { PinDialog("PIN", { if (it == "1357") PinCheck.Accepted else PinCheck.Rejected(0) }, { accepted = true }, {}) }
        }
        "1357".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("OK").performClick(); compose.waitForIdle()
        assertTrue(accepted)
    }

    @Test fun lockoutShowsWaitAndDisablesOk() {
        compose.setContent { StovaTheme { PinDialog("PIN", { PinCheck.LockedOut(15_000) }, {}, {}) } }
        "0000".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Prøv igjen om 15 s").assertExists()
        compose.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test fun wrongPinShowsMessage() {
        compose.setContent { StovaTheme { PinDialog("PIN", { PinCheck.Rejected(0) }, {}, {}) } }
        "1111".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Feil PIN").assertExists()
    }
}
