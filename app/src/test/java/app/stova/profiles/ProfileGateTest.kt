package app.stova.profiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.AppContainer
import app.stova.MainActivity
import app.stova.StovaApplication
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ProfileGateTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container: AppContainer get() = (rule.activity.application as StovaApplication).container

    /** Adult "A" with PIN 1357 and kid "K"; the picker is showing. */
    private fun seedPicker() {
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            container.profiles.create("K", AvatarColor.ROSE, true)
            container.profiles.setPin(a.id, "1357")
            container.profiles.setStartInLastProfile(false)
            container.session.leave()
        }
        rule.waitForIdle()
    }

    @Test fun firstRunCreatesAdultProfileAndEnters() {
        rule.onNodeWithText("Velkomen til Stova").assertExists()
        rule.onNodeWithText("Barneprofil").assertDoesNotExist()
        rule.onNodeWithText("Namn").performTextInput("Øyvind")
        rule.onNodeWithText("Lagre").performClick()
        // Saving writes profiles.json on the IO dispatcher, which waitForIdle does not track.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-HOME").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun pickerAsksPinForProtectedProfile() {
        seedPicker()
        rule.onNodeWithText("Kven ser på?").assertExists()
        rule.onNodeWithText("A").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }

    @Test fun kidCannotLeaveWithoutAdultPin() {
        runBlocking {
            val a = container.profiles.create("A", AvatarColor.LIME, false)
            val k = container.profiles.create("K", AvatarColor.ROSE, true)
            container.profiles.setPin(a.id, "1357")
            container.session.enter(k.id)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Byt profil").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }

    @Test @Config(fontScale = 2f) fun pickerSurvivesDoubleFontScale() {
        seedPicker()
        rule.onNodeWithText("A").assertIsDisplayed()
        rule.onNodeWithText("K").assertIsDisplayed()
    }
}
