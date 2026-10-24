package app.stova.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Stova draws its own icons, like Spole: a 24-unit grid, 2-unit round strokes, one visual
 * language. The stroke colour is a placeholder; `Icon` tints it.
 */
object StovaIcons {
    val Home: ImageVector by lazy {
        icon("home") {
            moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f); lineTo(20f, 20f); lineTo(14.5f, 20f)
            lineTo(14.5f, 14f); lineTo(9.5f, 14f); lineTo(9.5f, 20f); lineTo(4f, 20f); close()
        }
    }

    val Apps: ImageVector by lazy {
        icon("apps") {
            square(4f, 4f); square(14f, 4f); square(4f, 14f); square(14f, 14f)
        }
    }

    val Settings: ImageVector by lazy {
        icon("settings") {
            moveTo(4f, 6f); lineTo(20f, 6f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 18f); lineTo(20f, 18f)
            moveTo(9f, 4f); lineTo(9f, 8f)
            moveTo(15f, 10f); lineTo(15f, 14f)
            moveTo(7f, 16f); lineTo(7f, 20f)
        }
    }

    val Lock: ImageVector by lazy {
        icon("lock") {
            moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(8.5f, 11f); lineTo(8.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.5f, y1 = 8f)
            lineTo(15.5f, 11f)
        }
    }

    val Plus: ImageVector by lazy {
        icon("plus") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }
}

private fun PathBuilder.square(x: Float, y: Float, side: Float = 6f) {
    moveTo(x, y); lineTo(x + side, y); lineTo(x + side, y + side); lineTo(x, y + side); close()
}

private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()
