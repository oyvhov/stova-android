package app.stova.core.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.min

/**
 * The colour a picture leans towards: the average of its more colourful, mid-bright pixels, so a
 * mostly grey frame with a red coat still reads red. Null when the picture is close to grey; no
 * tint is better than an invented one. [pixels] are ARGB, as from `Bitmap.getPixels`.
 */
fun artTone(pixels: IntArray): Color? {
    var r = 0.0; var g = 0.0; var b = 0.0; var weight = 0.0
    for (p in pixels) {
        val pr = (p shr 16 and 255) / 255f; val pg = (p shr 8 and 255) / 255f; val pb = (p and 255) / 255f
        val hi = max(pr, max(pg, pb)); val lo = min(pr, min(pg, pb))
        if (hi <= 0f) continue
        val saturation = (hi - lo) / hi
        // Near-black and blown-out pixels say little about the picture's colour.
        val w = saturation * saturation * if (hi in 0.15f..0.95f) 1f else 0.2f
        r += pr * w; g += pg * w; b += pb * w; weight += w
    }
    if (pixels.isEmpty() || weight / pixels.size < MIN_COLOUR) return null
    return Color((r / weight).toFloat(), (g / weight).toFloat(), (b / weight).toFloat())
}

/** Hue in degrees, saturation and lightness in 0..1 (HSL). */
fun Color.hsl(): Triple<Float, Float, Float> {
    val hi = max(red, max(green, blue)); val lo = min(red, min(green, blue))
    val lightness = (hi + lo) / 2f
    if (hi == lo) return Triple(0f, 0f, lightness)
    val d = hi - lo
    val saturation = d / (1f - kotlin.math.abs(2f * lightness - 1f))
    val hue = when (hi) {
        red -> ((green - blue) / d).mod(6f)
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return Triple(hue, saturation.coerceIn(0f, 1f), lightness)
}

/**
 * The picture's tone as the app shell animates it. It is read only while drawing, so a new picture
 * redraws the scrim, focus and tiles without recomposing them. Each use keeps its own lightness,
 * so contrast and the profile's theme hold; only the hue leans, and only a little.
 */
@Stable
class ArtToneState(private val color: State<Color>, private val strength: State<Float>) {
    /** The dark ink of the scrim behind the hero text. */
    fun ink(base: Color): Color = toward(base, base.hsl().third, INK_SATURATION, INK_MIX)
    /** The light a focused control gets. */
    fun light(base: Color): Color = toward(base, base.hsl().third, LIGHT_SATURATION, LIGHT_MIX)
    /** The raised surface behind an app tile. */
    fun surface(base: Color): Color = toward(base, base.hsl().third, SURFACE_SATURATION, SURFACE_MIX)

    private fun toward(base: Color, lightness: Float, saturation: Float, mix: Float): Color {
        val s = strength.value
        if (s <= 0.001f) return base
        val (hue, own, _) = color.value.hsl()
        return lerp(base, Color.hsl(hue, min(own, saturation), lightness, base.alpha), mix * s)
    }
}

/** Provided only where the backdrop is on screen; elsewhere null and nothing is tinted. */
val LocalArtTone = staticCompositionLocalOf<ArtToneState?> { null }

/** Below this average colourfulness a picture counts as grey. */
private const val MIN_COLOUR = 0.01
private const val INK_SATURATION = 0.35f
private const val INK_MIX = 0.6f
private const val LIGHT_SATURATION = 0.45f
private const val LIGHT_MIX = 0.35f
private const val SURFACE_SATURATION = 0.3f
private const val SURFACE_MIX = 0.5f
