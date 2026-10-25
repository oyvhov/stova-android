package app.stova.apps

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

data class IconPack(val packageName: String, val label: String, val versionCode: Long, val updatedAt: Long = 0) {
    val monochrome: Boolean get() = packageName.startsWith("com.donnnno.arcticons")
    val cacheKey: String get() = "$packageName:$versionCode:$updatedAt"
}

/** Blocking platform boundary; the repository calls it only on IO. Tests need no installed pack. */
interface IconPackSource {
    fun installed(): List<IconPack>
    fun appFilter(pack: IconPack): String?
    fun drawableCatalog(pack: IconPack): String?
    fun drawable(pack: IconPack, name: String, density: Int): Drawable?
}

@Serializable
data class IconOverride(val packageName: String, val drawable: String)

@Serializable
data class IconPackIndex(val components: Map<String, String> = emptyMap(), val drawables: List<String> = emptyList())
