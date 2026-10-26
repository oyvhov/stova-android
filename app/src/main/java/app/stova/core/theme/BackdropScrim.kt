package app.stova.core.theme

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush

// The original light/medium/dark curves at 25/50/75. Interpolation preserves every
// original stop, while allowing continuous adjustment down to clear and up to ink.
// The text side keeps its protection up to .46; the old mid stops at .52 made a hard edge and are gone.
private val lightScrim = listOf(0f to .96f, .15f to .92f, .35f to .88f, .46f to .84f,
    .62f to .05f, .76f to 0f, 1f to 0f)
private val mediumScrim = listOf(0f to 1f, .15f to .94f, .35f to .9f, .47f to .85f,
    .62f to .18f, .82f to 0f, 1f to 0f)
private val darkScrim = listOf(0f to 1f, .45f to .9f, .62f to .74f, .72f to .45f,
    .82f to .18f, .92f to .04f, 1f to 0f)
private val curves = listOf(listOf(0f to 0f, 1f to 0f), lightScrim, mediumScrim, darkScrim,
    listOf(0f to 1f, 1f to 1f))

/**
 * A monotone cubic through each curve's stops (Fritsch–Carlson): it passes every original anchor,
 * never overshoots, and bends smoothly, so the fade has no visible knee where straight lines met.
 */
private fun smoothAt(curve: List<Pair<Float, Float>>, x: Float): Float {
    val px = x.coerceIn(0f, 1f)
    val n = curve.size
    val d = FloatArray(n - 1) { (curve[it + 1].second - curve[it].second) / (curve[it + 1].first - curve[it].first) }
    val m = FloatArray(n) { i -> when (i) { 0 -> d[0]; n - 1 -> d[n - 2]; else -> if (d[i - 1] * d[i] <= 0f) 0f else (d[i - 1] + d[i]) / 2 } }
    for (i in 0 until n - 1) {
        if (d[i] == 0f) { m[i] = 0f; m[i + 1] = 0f; continue }
        val a = m[i] / d[i]; val b = m[i + 1] / d[i]; val s = a * a + b * b
        if (s > 9f) { val k = 3f / kotlin.math.sqrt(s); m[i] = k * a * d[i]; m[i + 1] = k * b * d[i] }
    }
    val i = (0 until n - 1).first { px <= curve[it + 1].first }
    val (x0, y0) = curve[i]; val (x1, y1) = curve[i + 1]
    val h = x1 - x0; val t = (px - x0) / h
    val t2 = t * t; val t3 = t2 * t
    return (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * h * m[i] + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * h * m[i + 1]
}

internal fun backdropScrimAlpha(percent: Int, x: Float): Float {
    val scaled = percent.coerceIn(0, 100) / 25f
    val low = scaled.toInt().coerceAtMost(3)
    val a = smoothAt(curves[low], x); val b = smoothAt(curves[low + 1], x)
    return (a + (b - a) * (scaled - low)).coerceIn(0f, 1f)
}

private const val SCRIM_STOPS = 48
private fun eased(t: Float) = t * t * (3 - 2 * t)

/** Shared by Home and its settings preview. Top-bar protection stays stable. */
@Composable
fun BackdropScrim(percent: Int, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    val tone = LocalArtTone.current
    Canvas(modifier) {
        val ink = tone?.ink(base) ?: base
        // Many stops sampled from the smooth curve, so the GPU's straight segments stay invisible.
        drawRect(Brush.horizontalGradient(*(0..SCRIM_STOPS).map { val x = it / SCRIM_STOPS.toFloat()
            x to ink.copy(alpha = backdropScrimAlpha(percent, x)) }.toTypedArray()))
        val bottomStart = .5f - ((percent - 25) / 25f).coerceIn(0f, 1f) * .05f
        val bottomAlpha = (percent / 25f).coerceIn(0f, 1f)
        drawRect(Brush.verticalGradient(*(0..16).map { val t = it / 16f; t to ink.copy(alpha = bottomAlpha * eased(t)) }.toTypedArray(),
            startY = size.height * bottomStart, endY = size.height),
            topLeft = Offset(0f, size.height * bottomStart), size = Size(size.width, size.height * (1f - bottomStart)))
        drawRect(Brush.verticalGradient(*(0..16).map { val t = it / 16f; t to ink.copy(alpha = 1f - eased(t)) }.toTypedArray(), endY = size.height * .3f),
            size = Size(size.width, size.height * .3f))
    }
}
