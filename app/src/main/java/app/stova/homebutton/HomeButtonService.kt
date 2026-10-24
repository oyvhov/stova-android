package app.stova.homebutton

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import app.stova.MainActivity
import app.stova.StovaApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Sends Home to Stova. Reads no window content and stores no keys: every key but Home is passed on
 * untouched the moment it arrives.
 */
class HomeButtonService : AccessibilityService() {
    private val logic = HomeKeyLogic(SystemClock::elapsedRealtime)
    @Volatile private var overrideOn = true
    private var watch: Job? = null

    override fun onServiceConnected() {
        // Some TV builds ignore the XML flag, so it is set here as well.
        serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS }
        val container = (application as StovaApplication).container
        watch = container.appScope.launch {
            combine(container.household.data, container.crashGuard.data) { household, guard ->
                household.homeButtonOverride && !guard.tripped
            }.collect { overrideOn = it }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean =
        when (logic.onKey(event.keyCode, event.action, event.repeatCount, overrideOn)) {
            KeyDecision.PASS -> false
            KeyDecision.CONSUME -> true
            KeyDecision.CONSUME_AND_LAUNCH -> { openStova(); true }
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && logic.onWindowChanged(event.packageName, overrideOn)) {
            openStova()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        watch?.cancel()
        super.onDestroy()
    }

    private fun openStova() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }
}
