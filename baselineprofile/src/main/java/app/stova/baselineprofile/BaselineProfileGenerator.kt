package app.stova.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

internal const val PACKAGE = "app.stova"
private val appsLabel = Pattern.compile("Appar|Apps")
private val smartHomeLabel = Pattern.compile("Smarthus|Smart home")
internal fun seedDemo() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val output = device.executeShellCommand("am start -W -n $PACKAGE/.performance.PerformanceSeedActivity")
    check(!output.contains("Error")) { output }
    assertTrue("Demo home did not load", device.wait(Until.hasObject(By.desc("home-ready")), 5_000))
}

internal fun openTopBarAction(device: UiDevice, label: Pattern) {
    repeat(6) { device.pressDPadUp() }
    device.waitForIdle()
    // UiObject resolves the selector at click time, avoiding stale Compose accessibility nodes.
    val action = device.findObject(UiSelector().textMatches(label.pattern()).clickable(true))
    assertTrue("Top-bar action missing: $label", action.waitForExists(5_000))
    assertTrue(action.click())
    assertTrue("Action did not leave Home", device.wait(Until.gone(By.desc("home-ready")), 5_000))
}

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() {
        seedDemo()
        rule.collect(packageName = PACKAGE) {
            pressHome()
            startActivityAndWait()
            assertTrue(device.wait(Until.hasObject(By.desc("home-ready")), 5_000))
            repeat(10) { device.pressDPadRight() }
            repeat(3) { device.pressDPadDown() }
            repeat(10) { device.pressDPadRight() }
            // Reach the top bar with the remote before opening its Apps action.
            openTopBarAction(device, appsLabel)
            device.waitForIdle()
            device.pressBack()
            assertTrue(device.wait(Until.hasObject(By.desc("home-ready")), 5_000))
            if (device.hasObject(By.text(smartHomeLabel))) {
                openTopBarAction(device, smartHomeLabel)
                device.waitForIdle()
                device.pressBack()
            }
        }
    }
}

