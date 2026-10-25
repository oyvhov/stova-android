package app.stova

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import app.stova.apps.*

/** Invented line art behind the real source interface, never a downloaded/installed APK. */
class DemoLineIconPack : IconPackSource {
    val apps = listOf("app.reelstack" to "Spole", "demo.jellyfin" to "Jellyfin", "demo.kodi" to "Kodi", "demo.vlc" to "VLC").map { (pkg, label) ->
        InstalledApp("$pkg/$pkg.MainActivity", pkg, "$pkg.MainActivity", label, true)
    }
    override fun installed() = listOf(IconPack(PACK, "Arcticons · demo", 1))
    override fun appFilter(pack: IconPack) = apps.drop(1).joinToString("\n") { """<item component="ComponentInfo{${it.key}}" drawable="${it.label.lowercase()}"/>""" }
    override fun drawableCatalog(pack: IconPack) = listOf("jellyfin", "kodi", "vlc", "screen", "cinema", "music").joinToString("\n") { """<item drawable="$it"/>""" }
    override fun drawable(pack: IconPack, name: String, density: Int): Drawable = object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        override fun draw(canvas: Canvas) {
            canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.scale(bounds.width()/48f, bounds.height()/48f)
            when (name) {
                "jellyfin" -> {
                    canvas.drawPath(Path().apply { moveTo(24f,6f); lineTo(42f,38f); quadTo(24f,42f,6f,38f); close() },paint)
                    canvas.drawPath(Path().apply { moveTo(24f,20f); lineTo(32f,33f); lineTo(16f,33f); close() },paint)
                }
                "kodi" -> {
                    canvas.drawPath(Path().apply { moveTo(24f,5f); lineTo(43f,24f); lineTo(24f,43f); lineTo(5f,24f); close() },paint)
                    canvas.drawPath(Path().apply { moveTo(19f,15f); lineTo(19f,33f); moveTo(31f,15f); lineTo(22f,24f); lineTo(31f,33f) },paint)
                }
                "vlc" -> {
                    canvas.drawPath(Path().apply { moveTo(21f,7f); lineTo(27f,7f); lineTo(37f,38f); lineTo(11f,38f); close(); moveTo(18f,17f); lineTo(30f,17f); moveTo(14f,29f); lineTo(34f,29f) },paint)
                    canvas.drawRoundRect(8f,38f,40f,42f,2f,2f,paint)
                }
                "music" -> { canvas.drawCircle(15f,35f,5f,paint); canvas.drawLine(20f,35f,20f,9f,paint); canvas.drawLine(20f,9f,36f,6f,paint); canvas.drawLine(36f,6f,36f,30f,paint); canvas.drawCircle(31f,30f,5f,paint) }
                else -> { canvas.drawRoundRect(5f,9f,43f,35f,3f,3f,paint); canvas.drawLine(16f,42f,32f,42f,paint); canvas.drawLine(24f,35f,24f,42f,paint) }
            }
            canvas.restore()
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
        @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
    companion object { const val PACK = "com.donnnno.arcticons.demo" }
}
