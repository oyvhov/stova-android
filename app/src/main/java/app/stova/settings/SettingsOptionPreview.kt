package app.stova.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius as CanvasRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.stova.profiles.*

internal fun hasOptionPreview(value: Any?): Boolean = value is HomeRows || value is HomeRowInset ||
    value is ArtworkSize || value is AppStyle || value is MovieStyle || value is CornerRadius ||
    value is CardSpacing || value is RowSpacing || value is ProgressStyle || value is FocusEffect || value is HeroSize

/** Literal miniature of the selected value. The neighbouring text provides its accessible name. */
@Composable
internal fun SettingsOptionPreview(value: Any?) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.onSurfaceVariant
    val card = lerp(colors.surface, ink, 0.32f)
    Canvas(Modifier.size(64.dp, 34.dp).clearAndSetSemantics {}) {
        val unit = size.width / 64f
        fun tile(x: Float, y: Float, w: Float, h: Float, radius: Float = 2f, active: Boolean = false, tone: Color = card) {
            drawRoundRect(if (active) colors.primary else tone, Offset(x * unit, y * unit), Size(w * unit, h * unit),
                CanvasRadius(radius * unit))
        }
        fun screen() {
            drawRoundRect(colors.surfaceVariant, Offset.Zero, size, CanvasRadius(4 * unit))
            drawRoundRect(card, Offset.Zero, size, CanvasRadius(4 * unit), style = Stroke(unit))
        }
        when (value) {
            is HomeRows -> {
                screen()
                val count = if (value == HomeRows.ONE) 1 else 2
                val h = if (count == 1) 12f else 7f
                repeat(count) { row -> repeat(4) { col -> tile(5f + col * 14, 29 - h - row * (h + 3), 11f, h) } }
                if (value == HomeRows.SCROLLING) repeat(4) { tile(5f + it * 14, 2f, 11f, 3f) }
            }
            is HomeRowInset -> {
                screen()
                val y = when (value) { HomeRowInset.LOW -> 21f; HomeRowInset.BALANCED -> 16f; HomeRowInset.HIGH -> 10f }
                repeat(4) { tile(5f + it * 14, y, 11f, 8f) }
            }
            is HeroSize -> {
                screen()
                if (value != HeroSize.OFF) tile(5f, 4f, if (value == HeroSize.LARGE) 36f else 24f,
                    if (value == HeroSize.LARGE) 13f else 8f)
                repeat(4) { tile(5f + it * 14, 23f, 11f, 6f) }
            }
            is ArtworkSize -> {
                val h = value.width * 32f / ArtworkSize.EXTRA_LARGE.width
                repeat(3) { tile(6f + it * 19, (34 - h) / 2, h * .5f, h) }
            }
            is CornerRadius -> tile(12f, 2f, 40f, 30f, value.dp * .3f)
            is AppStyle -> repeat(if (value == AppStyle.ICON) 3 else 2) {
                tile(3f + it * if (value == AppStyle.ICON) 21 else 31, 9f,
                    if (value == AppStyle.ICON) 16f else 27f, 16f, if (value == AppStyle.ICON) 5f else 2f)
            }
            is MovieStyle -> repeat(if (value == MovieStyle.POSTER) 3 else 2) {
                tile(5f + it * if (value == MovieStyle.POSTER) 20 else 30, if (value == MovieStyle.POSTER) 3f else 9f,
                    if (value == MovieStyle.POSTER) 14f else 25f, if (value == MovieStyle.POSTER) 28f else 16f)
            }
            is CardSpacing -> {
                val gap = value.dp * .25f
                val w = (58 - 2 * gap) / 3
                repeat(3) { tile(3 + it * (w + gap), 7f, w, 20f) }
            }
            is RowSpacing -> {
                val gap = value.top * .2f
                repeat(2) { row -> repeat(3) { col -> tile(5f + col * 19, (34 - 16 - gap) / 2 + row * (8 + gap), 15f, 8f) } }
            }
            is ProgressStyle -> {
                tile(5f, 4f, 54f, 26f)
                if (value != ProgressStyle.HIDDEN) tile(5f, 30f - value.dp, 34f, value.dp.toFloat(), 0f, true)
            }
            is FocusEffect -> {
                val focused = value.resolved()
                val sides = if (focused == FocusEffect.LIGHT_UP) lerp(card, colors.surface, .28f) else card
                tile(5f, 12f, 13f, 14f, tone = sides)
                tile(46f, 12f, 13f, 14f, tone = sides)
                val zoom = focused == FocusEffect.ZOOM || focused == FocusEffect.RING_ZOOM
                val y = if (focused == FocusEffect.LIFT || zoom) 6f else 12f
                tile(24f, y, 16f, if (zoom) 22f else 14f,
                    tone = if (focused == FocusEffect.LIGHT_UP) lerp(card, ink, .2f) else card)
                if (focused == FocusEffect.OUTLINE || focused == FocusEffect.RING_ZOOM)
                    drawRoundRect(colors.primary, Offset(22 * unit, (y - 2) * unit), Size(20 * unit, (if (zoom) 26 else 18) * unit),
                        CanvasRadius(3 * unit), style = Stroke(1.5f * unit))
            }
        }
    }
}
