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
        network = NetworkStatus.Wifi(level = 4, band = WifiBand.GHZ_5, ip = "192.168.10.182"),
        bluetooth = BluetoothStatus(on = true, paired = 3),
        display = DisplayStatus(3840, 2160, 60),
        storage = StorageStatus(free = 9_800_000_000, total = 16_000_000_000),
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
        compose.onNodeWithText("Wi-Fi · sterkt signal · 5 GHz · 192.168.10.182").assertIsDisplayed()
        compose.onNodeWithText("På · 3 einingar para").assertIsDisplayed()
        compose.onNodeWithText("3840 × 2160 · 60 Hz").assertIsDisplayed()
        compose.onNodeWithText("ledig av", substring = true).assertIsDisplayed()
        assertEquals("a long press is not a press", 0, settings)
    }

    @Test fun menuKeyOpensItWithFocusOnTheFirstRowAndEachRowDoesItsOwnJob() {
        render()
        settingsIcon.performSemanticsAction(SemanticsActions.RequestFocus)
        settingsIcon.performKeyInput { pressKey(Key.Menu) }
        row("Nettverk").assertIsFocused()
        val rows = listOf("Nettverk" to SystemAction.NETWORK, "Bluetooth" to SystemAction.BLUETOOTH,
            "Bilete og lyd" to SystemAction.DISPLAY, "Lagring" to SystemAction.STORAGE,
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
        render(status.copy(network = NetworkStatus.Ethernet("10.0.0.5"), bluetooth = null))
        settingsIcon.performTouchInput { longClick() }
        compose.onNodeWithText("Kabel · 10.0.0.5").assertIsDisplayed()
        row("Bluetooth").assertIsDisplayed()
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
        assertEquals(WifiBand.GHZ_24, WifiBand.of(2437))
        assertEquals(WifiBand.GHZ_5, WifiBand.of(5180))
        assertEquals(WifiBand.GHZ_6, WifiBand.of(5975))
        assertEquals(null, WifiBand.of(0))
    }
}
