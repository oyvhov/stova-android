package app.stova.homebutton

import android.view.KeyEvent

/** The built-in launchers that may come to the front when Home is pressed. Never disabled by Stova. */
object StockLaunchers {
    val PACKAGES = setOf("com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx")
}

enum class KeyDecision { PASS, CONSUME, CONSUME_AND_LAUNCH }

/**
 * When the Home key belongs to Stova (Glassio's approach). The key filter is the main path: the
 * press is consumed and Stova opens on release, so the stock launcher never flashes. A stock
 * launcher window appearing without a recent key press (Home from the phone remote) is the fallback.
 */
class HomeKeyLogic(private val clock: () -> Long) {
    private var homeDownAt = NEVER
    private var lastLaunch = NEVER

    fun onKey(keyCode: Int, action: Int, repeatCount: Int, overrideOn: Boolean): KeyDecision {
        if (keyCode != KeyEvent.KEYCODE_HOME || !overrideOn) return KeyDecision.PASS
        return when (action) {
            KeyEvent.ACTION_DOWN -> {
                if (repeatCount == 0) homeDownAt = clock()
                KeyDecision.CONSUME
            }
            KeyEvent.ACTION_UP -> if (launchAllowed()) KeyDecision.CONSUME_AND_LAUNCH else KeyDecision.CONSUME
            else -> KeyDecision.CONSUME
        }
    }

    fun onWindowChanged(packageName: CharSequence?, overrideOn: Boolean): Boolean {
        if (!overrideOn || packageName?.toString() !in StockLaunchers.PACKAGES) return false
        if (clock() - homeDownAt < RECENT_KEY_WINDOW_MS) return false
        return launchAllowed()
    }

    private fun launchAllowed(): Boolean {
        val now = clock()
        if (now - lastLaunch < LAUNCH_DEBOUNCE_MS) return false
        lastLaunch = now
        return true
    }

    companion object {
        const val RECENT_KEY_WINDOW_MS = 1_500L
        const val LAUNCH_DEBOUNCE_MS = 1_000L
        private const val NEVER = Long.MIN_VALUE / 2
    }
}
