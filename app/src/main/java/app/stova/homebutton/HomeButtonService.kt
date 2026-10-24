package app.stova.homebutton

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import app.stova.MainActivity

/**
 * Sends Home to Stova. Runs in its own process (`:homebutton`) so a crash in the launcher UI does
 * not kill it: Android stops binding an accessibility service that has crashed until the user
 * toggles it. Reads no window content and stores no keys: every key but Home is passed on
 * untouched the moment it arrives.
 */
class HomeButtonService : AccessibilityService() {
    private val logic = HomeKeyLogic(SystemClock::elapsedRealtime)
    private val state by lazy { HomeOverrideState(filesDir) }

    override fun onServiceConnected() {
        // Some TV builds ignore the XML flag, so it is set here as well.
        serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        return when (logic.onKey(event.keyCode, event.action, event.repeatCount, state.isOn())) {
            KeyDecision.PASS -> false
            KeyDecision.CONSUME -> true
            KeyDecision.CONSUME_AND_LAUNCH -> { openStova(); true }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && logic.onWindowChanged(event.packageName, state.isOn())) {
            openStova()
        }
    }

    override fun onInterrupt() = Unit

    private fun openStova() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }
}
