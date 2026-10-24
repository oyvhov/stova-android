package app.stova.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class PackageManagerAppCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun register(pkg: String, vararg categories: String) {
        val pm = shadowOf(context.packageManager)
        val component = ComponentName(pkg, "$pkg.Main")
        pm.addActivityIfNotPresent(component)
        categories.forEach { category ->
            pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(category) })
        }
    }

    @Before fun apps() {
        register("tv.app", Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        register("phone.app", Intent.CATEGORY_LAUNCHER)
        register("com.google.android.tvlauncher", Intent.CATEGORY_LEANBACK_LAUNCHER)
    }

    @Test fun scanIncludesArtworkRevision() = runTest {
        val info = context.packageManager.getPackageInfo("tv.app", 0)
        info.lastUpdateTime = 1234L
        shadowOf(context.packageManager).installPackage(info)
        register("tv.app", Intent.CATEGORY_LEANBACK_LAUNCHER)
        val apps = PackageManagerAppCatalog(context, emptySet()).installed()
        assertEquals(1234L, apps.first { it.packageName == "tv.app" }.lastUpdateTime)
    }

    @Test fun listsTvFirstDedupesAndExcludesStock() = runTest {
        val apps = PackageManagerAppCatalog(context, setOf(context.packageName, "com.google.android.tvlauncher")).installed()
        assertEquals(setOf("tv.app", "phone.app"), apps.map { it.packageName }.toSet())
        assertTrue(apps.first { it.packageName == "tv.app" }.tv)
        assertFalse(apps.first { it.packageName == "phone.app" }.tv)
        assertEquals(1, apps.count { it.packageName == "tv.app" })
    }
}
