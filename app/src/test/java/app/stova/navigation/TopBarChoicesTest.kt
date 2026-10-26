package app.stova.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopBarChoicesTest {
    @get:Rule val compose = createComposeRule()
    @Test fun switchesHideOptionalChromeButKeepSettingsReachable() {
        var p by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance=p) {
            TopBar(Profile("p","Demo",AvatarColor.LIME),{},{},onSmartHome={},leading={ Text("HA-test") })
        } }
        compose.onNodeWithText("HA-test").assertExists()
        compose.onNodeWithText("Smarthus").assertExists()
        compose.onNodeWithText("Byt profil").assertExists()
        compose.runOnIdle { p=p.copy(showHaChips=false,showSmartHomeButton=false,showProfile=false,clock=ClockMode.HIDDEN) }
        compose.onNodeWithText("HA-test").assertDoesNotExist()
        compose.onNodeWithText("Smarthus").assertDoesNotExist()
        compose.onNodeWithText("Byt profil").assertDoesNotExist()
        compose.onNodeWithText("Innstillingar").assertExists()
    }
    @Test fun dateCanBeShownWhenClockIsHiddenAndLargeClockGrows() {
        var p by mutableStateOf(AppearancePrefs(clock=ClockMode.HIDDEN,showDate=true))
        compose.setContent { StovaTheme(appearance=p) { app.stova.core.ui.Clock(clock={0L}) } }
        compose.onNodeWithTag("top-date",useUnmergedTree=true).assertExists()
        compose.onNodeWithTag("top-clock",useUnmergedTree=true).assertDoesNotExist()
        compose.runOnIdle { p=p.copy(clock=ClockMode.H24) }
        val height=compose.onNodeWithTag("top-clock",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.height
        compose.runOnIdle { p=p.copy(clockSize=ClockSize.LARGE) }
        org.junit.Assert.assertTrue(compose.onNodeWithTag("top-clock",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.height>height)
    }
}
