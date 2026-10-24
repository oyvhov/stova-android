package app.stova.homebutton

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeKeyLogicTest {
    private var now = 10_000L
    private val l = HomeKeyLogic { now }
    private val H = KeyEvent.KEYCODE_HOME
    private val D = KeyEvent.ACTION_DOWN
    private val U = KeyEvent.ACTION_UP

    @Test fun otherKeysPass() = assertEquals(KeyDecision.PASS, l.onKey(KeyEvent.KEYCODE_DPAD_UP, D, 0, true))

    @Test fun homePassesWhenOverrideOff() = assertEquals(KeyDecision.PASS, l.onKey(H, D, 0, false))

    @Test fun homeDownConsumedUpLaunches() {
        assertEquals(KeyDecision.CONSUME, l.onKey(H, D, 0, true))
        assertEquals(KeyDecision.CONSUME_AND_LAUNCH, l.onKey(H, U, 0, true))
    }

    @Test fun windowFallbackSkippedRightAfterKey() {
        l.onKey(H, D, 0, true); now += 1_499
        assertFalse(l.onWindowChanged("com.google.android.tvlauncher", true))
    }

    @Test fun windowFallbackLaunchesForStockOnly() {
        assertTrue(l.onWindowChanged("com.google.android.tvlauncher", true)); now += 1_001
        assertFalse(l.onWindowChanged("com.netflix.ninja", true))
    }

    @Test fun launchesAreDebounced() {
        assertTrue(l.onWindowChanged("com.google.android.apps.tv.launcherx", true)); now += 999
        assertFalse(l.onWindowChanged("com.google.android.apps.tv.launcherx", true))
    }

    @Test fun windowFallbackOffWhenOverrideOff() = assertFalse(l.onWindowChanged("com.google.android.tvlauncher", false))
}
