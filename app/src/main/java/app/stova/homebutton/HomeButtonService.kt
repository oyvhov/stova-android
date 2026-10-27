package app.stova.homebutton

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
    private val launches = ArrayDeque<Long>()
    private var homes = HomeActivities(emptySet())
    private var observingPackages = false
    private val packageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refreshHomes() }
    }
    /** Stova's own request (not exported) from the Settings icon's menu: show Android's power menu. */
    private val powerMenu = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
    }

    private fun refreshHomes() {
        homes = HomeActivities.discover(this)
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            // Our own window acknowledges a successful return, so a second Home press
            // is not mistaken for a duplicate. No window content is requested.
            packageNames = (homes.packages + packageName).toTypedArray()
        }
    }

    private fun overrideOn(): Boolean =
        state.isOn() && !LaunchGuard.standDown(launches.toList(), LaunchGuard.lastAlive(filesDir), System.currentTimeMillis())

    override fun onServiceConnected() {
        // Some TV builds ignore the XML flag, so it is set here as well.
        refreshHomes()
        if (!observingPackages) {
            ContextCompat.registerReceiver(this, packageChanges, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            ContextCompat.registerReceiver(this, powerMenu, IntentFilter(ACTION_POWER_MENU), ContextCompat.RECEIVER_NOT_EXPORTED)
            observingPackages = true
        }
        // Load serializers and the JSON reader before the first latency-sensitive Home event.
        state.isOn()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        val decision = logic.onKey(event.keyCode, event.action, event.repeatCount, overrideOn())
        if (app.stova.BuildConfig.DEBUG) android.util.Log.d("StovaHome", "HOME action=${event.action}, repeat=${event.repeatCount}, decision=$decision")
        return when (decision) {
            KeyDecision.PASS -> false
            KeyDecision.CONSUME -> true
            KeyDecision.CONSUME_AND_LAUNCH -> { openStova(); true }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName?.toString() == packageName) logic.onStovaVisible()
            val matched = homes.contains(event.packageName, event.className)
            val window = if (matched) "${event.packageName}/${event.className}:${event.windowId}" else null
            val redirect = logic.onWindowChanged(window, overrideOn())
            if (app.stova.BuildConfig.DEBUG) android.util.Log.d("StovaHome", "Window ${event.packageName}/${event.className}, home=$matched, redirect=$redirect")
            if (redirect) openStova()
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        const val ACTION_POWER_MENU = "app.stova.action.POWER_MENU"
    }
    override fun onDestroy() {
        if (observingPackages) { unregisterReceiver(packageChanges); unregisterReceiver(powerMenu) }
        observingPackages = false
        super.onDestroy()
    }

    private fun openStova() {
        launches.addLast(System.currentTimeMillis())
        while (launches.size > LaunchGuard.THRESHOLD * 4) launches.removeFirst()
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }
}
