package app.stova.ha.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun sparklinePoints(values: List<Double>, width: Float, height: Float): List<Offset> {
    if (values.size < 2 || values.any { !it.isFinite() }) return emptyList()
    val min = values.min(); val range = values.max() - min
    return values.mapIndexed { i, value -> Offset(width * i / values.lastIndex, if (range == 0.0) height / 2 else (height * (1 - (value-min)/range)).toFloat()) }
}
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier, current: Int? = null) {
    val text = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().heightIn(min=28.dp)) {
        val points = sparklinePoints(values, size.width, size.height)
        if (points.isNotEmpty()) {
            val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x,it.y) } }
            drawPath(path, text.copy(alpha=0.6f), style=Stroke(2.dp.toPx()))
            current?.let { points.getOrNull(it) }?.let { drawCircle(text,3.dp.toPx(),it) }
        }
    }
}
