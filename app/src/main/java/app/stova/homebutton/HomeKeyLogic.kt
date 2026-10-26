package app.stova.homebutton

import android.view.KeyEvent

enum class KeyDecision { PASS, CONSUME, CONSUME_AND_LAUNCH }

/** Each real Home gesture can return to Stova. Only duplicate events from the same external
 * HOME window are coalesced; a competing launcher must never inherit another launcher's veto. */
class HomeKeyLogic(private val clock: () -> Long) {
    private var lastWindow: String? = null
    private var windowLaunchAt = Long.MIN_VALUE / 2

    fun onKey(keyCode: Int, action: Int, repeatCount: Int, overrideOn: Boolean): KeyDecision {
        if (keyCode != KeyEvent.KEYCODE_HOME || !overrideOn) return KeyDecision.PASS
        return when (action) {
            KeyEvent.ACTION_DOWN -> {
                if (repeatCount == 0) lastWindow = null
                KeyDecision.CONSUME
            }
            KeyEvent.ACTION_UP -> KeyDecision.CONSUME_AND_LAUNCH
            else -> KeyDecision.CONSUME
        }
    }

    /** Acknowledgement, not a timer: the next external HOME window is a new recovery. */
    fun onStovaVisible() { lastWindow = null }

    fun onWindowChanged(homeWindow: String?, overrideOn: Boolean): Boolean {
        if (!overrideOn || homeWindow == null) return false
        val now = clock()
        if (homeWindow == lastWindow && now - windowLaunchAt < LAUNCH_DEBOUNCE_MS) return false
        lastWindow = homeWindow
        windowLaunchAt = now
        return true
    }

    companion object { const val LAUNCH_DEBOUNCE_MS = 1_000L }
}
