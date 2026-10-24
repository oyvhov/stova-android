package app.stova.apps

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.R
import app.stova.TV_QUALIFIERS
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AppArtCacheTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val component = ComponentName(context.packageName, "demo.Main")
    private val app = InstalledApp(component.flattenToString(), component.packageName, component.className, "Demo", true)
    private fun activity() = shadowOf(context.packageManager).addActivityIfNotPresent(component).apply {
        applicationInfo = context.applicationInfo
    }

    @Test fun missingBannerIsRetriedWhenItBecomesAvailable() = runTest {
        val info = activity()
        info.banner = 0
        info.applicationInfo.banner = 0
        val cache = AppArtCache(context)
        assertNull(cache.banner(app))
        info.banner = R.drawable.tv_banner
        assertNotNull(cache.banner(app))
    }

    @Test fun updatedPackageReloadsBannerAndIcon() = runTest {
        activity().apply { banner = R.drawable.tv_banner; icon = R.drawable.ic_stova_foreground }
        val cache = AppArtCache(context)
        val banner = cache.banner(app)
        val icon = cache.icon(app)
        assertNotNull(banner); assertNotNull(icon)
        assertSame(banner, cache.banner(app))
        assertSame(icon, cache.icon(app))
        val updated = app.copy(lastUpdateTime = 2)
        assertNotSame(banner, cache.banner(updated))
        assertNotSame(icon, cache.icon(updated))
    }
}
