package app.stova.homebutton

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The Home service lives in its own process and reads the override state from disk. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeOverrideStateTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun state() = HomeOverrideState(tmp.root)

    @Test fun onByDefault() = assertTrue(state().isOn())

    @Test fun offWhenUserTurnedItOff() {
        tmp.root.resolve("household.json").writeText("""{"homeButtonOverride":false}""")
        assertFalse(state().isOn())
    }

    @Test fun offWhenCrashGuardTripped() {
        tmp.root.resolve("crash_guard.json").writeText("""{"times":[1,2,3],"tripped":true}""")
        assertFalse(state().isOn())
    }

    @Test fun corruptFilesMeanDefaults() {
        tmp.root.resolve("household.json").writeText("{not json")
        assertTrue(state().isOn())
    }

    @Test fun readsFreshEachTime() {
        val s = state(); assertTrue(s.isOn())
        tmp.root.resolve("household.json").writeText("""{"homeButtonOverride":false}""")
        assertFalse(s.isOn())
    }

    @Test fun serviceRunsInItsOwnProcess() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getServiceInfo(ComponentName(context, HomeButtonService::class.java), 0)
        assertTrue(info.processName, info.processName.endsWith(":homebutton"))
    }
}
