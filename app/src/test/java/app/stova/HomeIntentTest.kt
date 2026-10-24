package app.stova

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeIntentTest {
    @Test fun mainActivityAnswersHomeIntent() {
        val pm = ApplicationProvider.getApplicationContext<Context>().packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val names = pm.queryIntentActivities(home, 0).map { it.activityInfo.name }
        assertTrue(names.contains("app.stova.MainActivity"))
    }

    @Test fun mainActivityIsListedOnTvLauncher() {
        val pm = ApplicationProvider.getApplicationContext<Context>().packageManager
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        assertTrue(pm.queryIntentActivities(tv, 0).any { it.activityInfo.name == "app.stova.MainActivity" })
    }
}
