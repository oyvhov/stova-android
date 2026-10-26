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
private val lightScrim = listOf(0f to .96f, .15f to .92f, .35f to .88f, .46f to .84f,
    .52f to .5f, .62f to .05f, .72f to 0f, 1f to 0f)
private val mediumScrim = listOf(0f to 1f, .15f to .94f, .35f to .9f, .46f to .84f,
    .52f to .65f, .62f to .18f, .76f to 0f, 1f to 0f)
private val darkScrim = listOf(0f to 1f, .45f to .9f, .62f to .74f, .72f to .45f,
    .82f to .18f, .92f to .04f, 1f to 0f)
private val positions = (lightScrim + mediumScrim + darkScrim).map { it.first }.distinct().sorted()
private val curves = listOf(listOf(0f to 0f, 1f to 0f), lightScrim, mediumScrim, darkScrim,
    listOf(0f to 1f, 1f to 1f))

internal fun backdropScrimAlpha(percent: Int, x: Float): Float {
    val scaled = percent.coerceIn(0, 100) / 25f
    val low = scaled.toInt().coerceAtMost(3)
    fun at(curve: List<Pair<Float, Float>>): Float {
        val (a, b) = curve.zipWithNext().first { x.coerceIn(0f, 1f) <= it.second.first }
        return a.second + (b.second - a.second) * ((x - a.first) / (b.first - a.first)).coerceIn(0f, 1f)
    }
    val a = at(curves[low]); val b = at(curves[low + 1])
    return a + (b - a) * (scaled - low)
}

/** Shared by Home and its settings preview. Top-bar protection stays stable. */
@Composable
fun BackdropScrim(percent: Int, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.background
    Canvas(modifier) {
        drawRect(Brush.horizontalGradient(*positions.map { it to ink.copy(alpha = backdropScrimAlpha(percent, it)) }.toTypedArray()))
        val bottomStart = .5f - ((percent - 25) / 25f).coerceIn(0f, 1f) * .05f
        val bottomAlpha = (percent / 25f).coerceIn(0f, 1f)
        drawRect(Brush.verticalGradient(listOf(ink.copy(alpha = 0f), ink.copy(alpha = bottomAlpha)),
            startY = size.height * bottomStart, endY = size.height),
            topLeft = Offset(0f, size.height * bottomStart), size = Size(size.width, size.height * (1f - bottomStart)))
        drawRect(Brush.verticalGradient(listOf(ink, ink.copy(alpha = 0f)), endY = size.height * .3f),
            size = Size(size.width, size.height * .3f))
    }
}
