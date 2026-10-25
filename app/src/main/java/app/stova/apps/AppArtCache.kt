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
    private val monochrome = LruCache<String, Boolean>(maxEntries)
    private val packStyles = LruCache<String, Boolean>(16)

    suspend fun artwork(app: InstalledApp, prefs: AppearancePrefs, override: IconOverride? = null): AppArtwork? {
        if (override != null || prefs.iconPack != null) {
            val packs = iconPacks.installed()
            // An unavailable override falls back to the global pack, then the app's own art.
            val manual = override?.let { custom -> packs.firstOrNull { it.packageName == custom.packageName }?.let { it to custom.drawable } }
            if (manual != null) packIcon(manual.first, manual.second)?.let { return AppArtwork(it, true, isMonochrome(manual.first, manual.second)) }
            val pack = packs.firstOrNull { it.packageName == prefs.iconPack }
            if (pack != null) {
                val key = AppFilter.normalize(app.key)
                val name = iconPacks.index(pack).components[key]
                if (name != null) packIcon(pack, name)?.let { return AppArtwork(it, true, isMonochrome(pack, name)) }
                if (isSpole(app)) {
                    if (pack.projectivy) return AppArtwork(packIcon(pack, ProjectivyIcons.SPOLE)!!, true, false)
                    val line = packStyle(pack)
                    return AppArtwork(spole(line, false)!!, true, line)
                }
            }
        }
        val own = if (prefs.appStyle == AppStyle.ICON) icon(app) else banner(app)
        return (own ?: if (isSpole(app)) spole(false, prefs.appStyle == AppStyle.BANNER) else null)?.let { AppArtwork(it) }
    }

    suspend fun packIcon(pack: IconPack, name: String): ImageBitmap? = cached("p:${pack.cacheKey}:$name") {
        val bitmap = if (pack.projectivy && name == ProjectivyIcons.SPOLE) app.stova.core.theme.projectivySpoleArtwork(context)
            else iconPacks.drawable(pack, name)?.let { drawable ->
                val width = drawable.intrinsicWidth.coerceAtLeast(1)
                val height = drawable.intrinsicHeight.coerceAtLeast(1)
                val scale = PACK_PX.toFloat() / maxOf(width, height)
                drawable.toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
            }
        bitmap?.also {
            monochrome.put("${pack.cacheKey}:$name", pack.monochrome || bitmapIsMonochrome(it))
        }
    }

    fun isMonochrome(pack: IconPack, name: String): Boolean = pack.monochrome || monochrome.get("${pack.cacheKey}:$name") == true

    private suspend fun packStyle(pack: IconPack): Boolean {
        if (pack.monochrome) return true
        packStyles.get(pack.cacheKey)?.let { return it }
        val sample = iconPacks.index(pack).drawables.take(3)
        val line = sample.isNotEmpty() && sample.all { name -> packIcon(pack, name) != null && isMonochrome(pack, name) }
        packStyles.put(pack.cacheKey, line)
        return line
    }

    /** Classify once during IO decoding. Transparency and antialiasing do not change ink colour. */
    private fun bitmapIsMonochrome(bitmap: android.graphics.Bitmap): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var ink: Int? = null
        for (pixel in pixels) {
            if (android.graphics.Color.alpha(pixel) < 64) continue
            val reference = ink
            if (reference == null) ink = pixel else {
                if (kotlin.math.abs(android.graphics.Color.red(pixel) - android.graphics.Color.red(reference)) > 12 ||
                    kotlin.math.abs(android.graphics.Color.green(pixel) - android.graphics.Color.green(reference)) > 12 ||
                    kotlin.math.abs(android.graphics.Color.blue(pixel) - android.graphics.Color.blue(reference)) > 12) return false
            }
        }
        return ink != null
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
