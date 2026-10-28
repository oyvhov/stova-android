package app.stova.navigation

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile
import app.stova.system.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SystemQuickMenuTest {
    @get:Rule val compose = createComposeRule()
    private val status = SystemStatus(
        device = "SHIELD Android TV", android = "11", uptimeMinutes = 3 * 1440 + 4 * 60,
        network = NetworkStatus.Wifi(level = 4),
        bluetooth = BluetoothStatus(on = true, paired = 3),
        powerMenu = true,
    )
    private val done = mutableListOf<SystemAction>()
    private var settings = 0

    private fun render(current: SystemStatus = status, menu: Boolean = true) {
        compose.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            StovaTheme(reduceMotion = true) {
                TopBar(Profile("p", "A", AvatarColor.LIME), { settings++ }, {},
                    systemMenu = if (menu) SystemMenu({ current }) { done += it } else null)
            }
        }
    }
    private val settingsIcon get() = compose.onNode(hasText("Innstillingar") and hasClickAction())
    private fun row(label: String) = compose.onNode(hasText(label) and hasClickAction())

    @Test fun holdingOkOnSettingsOpensASmallMenuWithTheDevicesRealStatus() {
        render()
        settingsIcon.performTouchInput { longClick() }
        compose.onNodeWithText("SHIELD Android TV").assertIsDisplayed()
        compose.onNodeWithText("Android 11 · på i 3 d 4 t").assertIsDisplayed()
        compose.onNodeWithText("På · 3 einingar para").assertIsDisplayed()
        assertEquals("a long press is not a press", 0, settings)
    }

    // «Fjerne bilete og lys, lagring, ipadresse og 5ghz og godt signal (den skal heller vise med
    // ikonet om den er tilkobla)»: the network row says it with its icon alone.
    @Test fun theNetworkIconSaysWhetherItIsConnectedAndNothingElseIsListed() {
        render()
        settingsIcon.performTouchInput { longClick() }
        compose.onNode(hasContentDescription("Wi-Fi"), useUnmergedTree = true).assertExists()
        for (gone in listOf("Bilete og lyd", "Lagring", "192.168", "GHz", "signal"))
            compose.onNodeWithText(gone, substring = true).assertDoesNotExist()
        assertEquals(listOf(1, 1, 2, 3, 3), (0..4).map(::wifiArcs))
    }

    // On the Shield: «lukker seg før eg får trykt på noko og trykker på første val». The hold that
    // opened the menu kept repeating into it, and its release chose the first row.
    @Test fun theHoldThatOpensTheMenuNeitherChoosesNorClosesIt() {
        render()
        settingsIcon.performTouchInput { longClick() }
        row("Nettverk").assertIsFocused()
        fun send(action: Int, repeat: Int) = row("Nettverk").performKeyPress(androidx.compose.ui.input.key.KeyEvent(
            android.view.KeyEvent(0L, 0L, action, android.view.KeyEvent.KEYCODE_DPAD_CENTER, repeat)))
        send(android.view.KeyEvent.ACTION_DOWN, 4)
        send(android.view.KeyEvent.ACTION_DOWN, 5)
        send(android.view.KeyEvent.ACTION_UP, 0)
        compose.onNodeWithText("SHIELD Android TV").assertIsDisplayed()
        assertEquals(emptyList<SystemAction>(), done)
        assertEquals(0, settings)
        // A fresh press inside the menu chooses as usual.
        send(android.view.KeyEvent.ACTION_DOWN, 0)
        send(android.view.KeyEvent.ACTION_UP, 0)
        assertEquals(listOf(SystemAction.NETWORK), done)
    }

    @Test fun menuKeyOpensItWithFocusOnTheFirstRowAndEachRowDoesItsOwnJob() {
        render()
        settingsIcon.performSemanticsAction(SemanticsActions.RequestFocus)
        settingsIcon.performKeyInput { pressKey(Key.Menu) }
        row("Nettverk").assertIsFocused()
        val rows = listOf("Nettverk" to SystemAction.NETWORK, "Bluetooth" to SystemAction.BLUETOOTH,
            "Start på nytt" to SystemAction.RESTART, "Alle Android-innstillingar" to SystemAction.ALL_SETTINGS)
        rows.forEach { (label, action) ->
            settingsIcon.performKeyInput { pressKey(Key.Menu) }
            row(label).performClick()
            assertEquals(action, done.last())
            // Choosing a row closes the menu.
            compose.onNodeWithText("SHIELD Android TV").assertDoesNotExist()
        }
    }

    @Test fun downMovesThroughTheRowsAndBackClosesTheMenu() {
        render()
        settingsIcon.performSemanticsAction(SemanticsActions.RequestFocus)
        settingsIcon.performKeyInput { pressKey(Key.Menu) }
        row("Nettverk").performKeyInput { pressKey(Key.DirectionDown) }
        row("Bluetooth").assertIsFocused()
        row("Bluetooth").performKeyInput { pressKey(Key.Back) }
        compose.onNodeWithText("SHIELD Android TV").assertDoesNotExist()
        settingsIcon.performClick()
        assertEquals(1, settings)
    }

    @Test fun restartSaysWhatItNeedsWhenTheHomeButtonIsOff() {
        render(status.copy(powerMenu = false))
        settingsIcon.performTouchInput { longClick() }
        compose.onNodeWithText("Slå på Heim-knappen i Stova først").assertIsDisplayed()
    }

    @Test fun cableAndUnknownBluetoothAreShownAsTheyAre() {
        render(status.copy(network = NetworkStatus.Ethernet, bluetooth = null))
        settingsIcon.performTouchInput { longClick() }
        compose.onNode(hasContentDescription("Kabel"), useUnmergedTree = true).assertExists()
        row("Bluetooth").assertIsDisplayed()
    }

    @Test fun offlineShowsTheCrossedOutIcon() {
        render(status.copy(network = NetworkStatus.Offline))
        settingsIcon.performTouchInput { longClick() }
        compose.onNode(hasContentDescription("Ikkje tilkopla"), useUnmergedTree = true).assertExists()
    }

    @Test fun withoutAMenuHoldingIsAnOrdinaryPressAsBefore() {
        render(menu = false)
        settingsIcon.performTouchInput { longClick() }
        compose.onNodeWithText("SHIELD Android TV").assertDoesNotExist()
        assertEquals(1, settings)
    }

    @Test fun uptimeAndSignalReadPlainly() {
        assertEquals(4, signalLevel(-50))
        assertEquals(2, signalLevel(-75))
        assertEquals(0, signalLevel(-95))
    }
}
