package app.stova.homebutton

import android.accessibilityservice.AccessibilityServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS, shadows = [ConnectedServiceInfo::class])
class HomeButtonServiceTest {
    @org.junit.Before fun ownReceiverPermission() {
        val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions("${app.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    }

    @Test fun installedHomeActivityIsRedirectedButItsSettingsAreNot() {
        val controller = Robolectric.buildService(HomeButtonService::class.java).create()
        val service = controller.get()
        val pm = org.robolectric.Shadows.shadowOf(service.packageManager)
        val home = android.content.ComponentName("com.spocky.projengmenu", "com.spocky.projengmenu.ui.home.MainActivity")
        pm.addActivityIfNotPresent(home)
        pm.addIntentFilterForActivity(home, android.content.IntentFilter(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        })
        service.serviceInfo = AccessibilityServiceInfo()
        ReflectionHelpers.callInstanceMethod<Unit>(service, "onServiceConnected")
        fun window(activity: String) = android.view.accessibility.AccessibilityEvent.obtain(
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = home.packageName; className = activity
        }
        service.onAccessibilityEvent(window("com.spocky.projengmenu.ui.settings.SettingsActivity"))
        assertNull(org.robolectric.shadow.api.Shadow.extract<ConnectedServiceInfo>(service).nextStartedActivity)
        service.onAccessibilityEvent(window(home.className))
        assertEquals(app.stova.MainActivity::class.java.name,
            org.robolectric.shadow.api.Shadow.extract<ConnectedServiceInfo>(service).nextStartedActivity?.component?.className)
        assertTrue(home.packageName in service.serviceInfo.packageNames)
        controller.destroy()
    }

    @Test fun connectionWarmsOverrideReaderBeforeFirstHomeKey() {
        val controller = Robolectric.buildService(HomeButtonService::class.java).create()
        val service = controller.get()
        service.serviceInfo = AccessibilityServiceInfo()
        val reader = ReflectionHelpers.getField<Lazy<*>>(service, "state\$delegate")
        assertFalse(reader.isInitialized())
        ReflectionHelpers.callInstanceMethod<Unit>(service, "onServiceConnected")
        assertTrue(reader.isInitialized())
        assertTrue(service.serviceInfo.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS != 0)
        controller.destroy()
    }
}

/** Supplies the service info Android provides only after the system binds accessibility. */
@org.robolectric.annotation.Implements(android.accessibilityservice.AccessibilityService::class)
class ConnectedServiceInfo : org.robolectric.shadows.ShadowService() {
    private var info = AccessibilityServiceInfo()
    @org.robolectric.annotation.Implementation fun getServiceInfo(): AccessibilityServiceInfo = info
    @org.robolectric.annotation.Implementation fun setServiceInfo(value: AccessibilityServiceInfo) { info = value }
}
