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
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.AppStyle
import java.io.File

data class AppArtwork(val bitmap: ImageBitmap, val packIcon: Boolean = false, val monochrome: Boolean = false)

/**
 * Banners and icons, decoded once at the size they are shown and kept in a small LRU cache.
 * `loadBanner()` picks the screen density and looks soft on a TV, so the XXHIGH variant is read
 * directly (Glassio does the same).
 */
class AppArtCache(
    private val context: Context,
    maxEntries: Int = 64,
    val iconPacks: IconPackRepository = IconPackRepository(PackageManagerIconPackSource(context), File(context.cacheDir, "icon-packs")),
) {
    private val pm = context.packageManager
    private val cache = LruCache<String, ImageBitmap>(maxEntries)

    suspend fun artwork(app: InstalledApp, prefs: AppearancePrefs, override: IconOverride? = null): AppArtwork? {
        if (override != null || prefs.iconPack != null) {
            val packs = iconPacks.installed()
            // An unavailable override falls back to the global pack, then the app's own art.
            val manual = override?.let { custom -> packs.firstOrNull { it.packageName == custom.packageName }?.let { it to custom.drawable } }
            if (manual != null) packIcon(manual.first, manual.second)?.let { return AppArtwork(it, true, manual.first.monochrome) }
            val pack = packs.firstOrNull { it.packageName == prefs.iconPack }
            if (pack != null) {
                val key = AppFilter.normalize(app.key)
                val name = iconPacks.index(pack).components[key]
                if (name != null) packIcon(pack, name)?.let { return AppArtwork(it, true, pack.monochrome) }
                if (isSpole(app)) return AppArtwork(spole(pack.monochrome, false)!!, true, pack.monochrome)
            }
        }
        val own = if (prefs.appStyle == AppStyle.ICON) icon(app) else banner(app)
        return (own ?: if (isSpole(app)) spole(false, prefs.appStyle == AppStyle.BANNER) else null)?.let { AppArtwork(it) }
    }

    suspend fun packIcon(pack: IconPack, name: String): ImageBitmap? = cached("p:${pack.cacheKey}:$name") {
        iconPacks.drawable(pack, name)?.toBitmap(PACK_PX, PACK_PX)
    }

    private suspend fun spole(outline: Boolean, banner: Boolean) = cached("spole:$outline:$banner") {
        app.stova.core.theme.spoleArtwork(context, outline, if (banner) BANNER_W else PACK_PX, if (banner) BANNER_H else PACK_PX)
    }

    private fun isSpole(app: InstalledApp) = app.packageName == "app.reelstack" || app.packageName == "app.reelstack.debug"

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

    private suspend fun cached(key: String, load: suspend () -> android.graphics.Bitmap?): ImageBitmap? {
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
        const val PACK_PX = 320
    }
}
