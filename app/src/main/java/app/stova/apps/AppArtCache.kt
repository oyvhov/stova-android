package app.stova.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Banners and icons, decoded once at the size they are shown and kept in a small LRU cache.
 * `loadBanner()` picks the screen density and looks soft on a TV, so the XXHIGH variant is read
 * directly (Glassio does the same).
 */
class AppArtCache(context: Context, maxEntries: Int = 64) {
    private val pm = context.packageManager
    private val cache = LruCache<String, ImageBitmap>(maxEntries)

    suspend fun banner(app: InstalledApp): ImageBitmap? = cached("b:${app.key}:${app.lastUpdateTime}") {
        val info = pm.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
        val id = if (info.banner != 0) info.banner else info.applicationInfo.banner
        if (id == 0) null else drawable(info.applicationInfo.packageName, id)?.toBitmap(BANNER_W, BANNER_H)
    }

    suspend fun icon(app: InstalledApp): ImageBitmap? = cached("i:${app.key}:${app.lastUpdateTime}") {
        val info = pm.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
        val id = info.iconResource
        val drawable = (if (id != 0) drawable(info.applicationInfo.packageName, id) else null) ?: info.loadIcon(pm)
        drawable?.toBitmap(ICON_PX, ICON_PX)
    }

    private suspend fun cached(key: String, load: () -> android.graphics.Bitmap?): ImageBitmap? {
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bitmap = try {
                load()?.asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: Resources.NotFoundException) {
                null
            }
            if (bitmap != null) cache.put(key, bitmap)
            bitmap
        }
    }

    private fun drawable(packageName: String, id: Int): Drawable? =
        pm.getResourcesForApplication(packageName).getDrawableForDensity(id, DisplayMetrics.DENSITY_XXHIGH, null)

    private companion object {
        // 248 × 139.5 dp at the Shield's 320 dpi; icons at 64 dp.
        const val BANNER_W = 496
        const val BANNER_H = 279
        const val ICON_PX = 128
    }
}
