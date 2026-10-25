package app.stova.navigation

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SystemSettingsTest {
    private class Recorder(private val refuse: Set<String?>) : ContextWrapper(ApplicationProvider.getApplicationContext()) {
        val started = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            if (intent.action in refuse && intent.component == null) throw ActivityNotFoundException()
            started += intent
        }
    }

    @Test fun opensAndroidSettingsInItsOwnTask() {
        val context = Recorder(emptySet())
        assertTrue(openSystemSettings(context))
        assertEquals(Settings.ACTION_SETTINGS, context.started.single().action)
        assertTrue(context.started.single().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    private fun handleAccessibilityWith(pkg: String) = org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>().packageManager)
        .addResolveInfoForIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply { packageName = pkg; name = "$pkg.Screen" }
        })

    // The Shield answers the accessibility action with Google's stub: "Ingen apper kan gjøre dette".
    @Test fun accessibilityGoesToAndroidSettingsWhenOnlyTheStubAnswers() {
        handleAccessibilityWith("com.google.android.tv.frameworkpackagestubs")
        val context = Recorder(emptySet())
        assertEquals(false, openAccessibilitySettings(context))
        assertEquals(Settings.ACTION_SETTINGS, context.started.single().action)
    }

    @Test fun accessibilityOpensWhenARealScreenAnswers() {
        handleAccessibilityWith("com.android.tv.settings")
        val context = Recorder(emptySet())
        assertTrue(openAccessibilitySettings(context))
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, context.started.single().action)
    }

    @Test fun fallsBackToTheTvSettingsApp() {
        val context = Recorder(setOf(Settings.ACTION_SETTINGS))
        assertTrue(openSystemSettings(context))
        assertEquals("com.android.tv.settings", context.started.single().component?.packageName)
    }
}
