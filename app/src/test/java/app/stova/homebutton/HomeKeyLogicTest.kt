package app.stova.homebutton

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test

class HomeKeyLogicTest {
    private var now = 10_000L
    private val l = HomeKeyLogic { now }
    private val H = KeyEvent.KEYCODE_HOME
    private val D = KeyEvent.ACTION_DOWN
    private val U = KeyEvent.ACTION_UP
    private val stock = "stock/Main:1"
    private val projectivy = "projectivy/Main:2"

    @Test fun otherKeysPass() = assertEquals(KeyDecision.PASS, l.onKey(KeyEvent.KEYCODE_DPAD_UP,D,0,true))
    @Test fun homePassesWhenOverrideOff() = assertEquals(KeyDecision.PASS,l.onKey(H,D,0,false))
    @Test fun everyDistinctHomePressCanReturnToTheHomePage() {
        repeat(3) {
            assertEquals(KeyDecision.CONSUME,l.onKey(H,D,0,true))
            assertEquals(KeyDecision.CONSUME_AND_LAUNCH,l.onKey(H,U,0,true))
            now += 50
        }
    }
    @Test fun partialHomeGestureMustNotSuppressVisibleLauncherRecovery() {
        l.onKey(H,D,0,true); now += 50
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun competingLauncherAfterAHomeKeyLaunchMustStillRecover() {
        l.onKey(H,D,0,true);l.onKey(H,U,0,true);now += 50
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun stockThenProjectivyAreTwoSeparateRecoveries() {
        assertTrue(l.onWindowChanged(stock,true));now += 50
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun secondPressAfterStovaWasVisibleIsNeverSwallowed() {
        assertTrue(l.onWindowChanged(projectivy,true));now += 50
        l.onStovaVisible()
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun duplicateEventsFromTheSameWindowAreCoalesced() {
        assertTrue(l.onWindowChanged(projectivy,true));now += 50
        assertFalse(l.onWindowChanged(projectivy,true))
    }
    @Test fun aNewEventAfterAFailedStartCanRetry() {
        assertTrue(l.onWindowChanged(projectivy,true));now += 1_001
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun aNewPhysicalGestureClearsThePreviousWindow() {
        assertTrue(l.onWindowChanged(projectivy,true));now += 50
        l.onKey(H,D,0,true)
        assertTrue(l.onWindowChanged(projectivy,true))
    }
    @Test fun nonHomeWindowsAndDisabledOverrideNeverRedirect() {
        assertFalse(l.onWindowChanged(null,true))
        assertFalse(l.onWindowChanged(projectivy,false))
    }
}
