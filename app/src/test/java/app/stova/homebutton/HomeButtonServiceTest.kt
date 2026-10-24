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

