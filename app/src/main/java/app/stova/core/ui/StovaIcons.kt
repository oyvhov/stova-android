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
    val Check by lazy { icon("check") { moveTo(4f, 12f); lineTo(9f, 17f); lineTo(20f, 6f) } }
    val Hide by lazy { icon("hide") {
        moveTo(3f, 12f); curveTo(8f, 3f, 16f, 3f, 21f, 12f); curveTo(16f, 21f, 8f, 21f, 3f, 12f); close()
        moveTo(3f, 21f); lineTo(21f, 3f)
    } }
    val Palette by lazy { icon("palette") {
        moveTo(12f, 3f); curveTo(6f, 3f, 3f, 7f, 3f, 12f); curveTo(3f, 18f, 7f, 21f, 12f, 21f)
        curveTo(16f, 21f, 15f, 17f, 17f, 16f); curveTo(19f, 15f, 21f, 16f, 21f, 12f); curveTo(21f, 7f, 17f, 3f, 12f, 3f); close()
        moveTo(7f, 10f); lineTo(7.1f, 10f); moveTo(11f, 7f); lineTo(11.1f, 7f); moveTo(16f, 8f); lineTo(16.1f, 8f)
    } }
    val Rows by lazy { icon("rows") {
        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 10f); lineTo(3f, 10f); close()
        moveTo(3f, 14f); lineTo(21f, 14f); lineTo(21f, 19f); lineTo(3f, 19f); close()
    } }
    val Image by lazy { icon("image") {
        moveTo(4f, 3f); lineTo(20f, 3f); lineTo(20f, 21f); lineTo(4f, 21f); close()
        moveTo(4f, 16f); lineTo(9f, 11f); lineTo(13f, 15f); lineTo(16f, 12f); lineTo(20f, 16f)
        moveTo(15f, 7f); lineTo(15.1f, 7f)
    } }
    val Focus by lazy { icon("focus") {
        moveTo(8f, 3f); lineTo(3f, 3f); lineTo(3f, 8f); moveTo(16f, 3f); lineTo(21f, 3f); lineTo(21f, 8f)
        moveTo(3f, 16f); lineTo(3f, 21f); lineTo(8f, 21f); moveTo(21f, 16f); lineTo(21f, 21f); lineTo(16f, 21f)
        moveTo(15f, 12f); arcTo(3f, 3f, 0f, true, true, 9f, 12f); arcTo(3f, 3f, 0f, true, true, 15f, 12f)
    } }
    val Clock by lazy { icon("clock") {
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f); arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        moveTo(12f, 7f); lineTo(12f, 12f); lineTo(16f, 14f)
    } }
    val User by lazy { icon("user") {
        moveTo(16f, 7f); arcTo(4f, 4f, 0f, true, true, 8f, 7f); arcTo(4f, 4f, 0f, true, true, 16f, 7f)
        moveTo(4f, 21f); curveTo(4f, 11f, 20f, 11f, 20f, 21f)
    } }
    val Link by lazy { icon("link") {
        moveTo(9f, 14f); lineTo(15f, 8f); moveTo(7f, 12f); lineTo(5f, 14f); curveTo(0f, 19f, 5f, 24f, 10f, 19f); lineTo(12f, 17f)
        moveTo(12f, 7f); lineTo(14f, 5f); curveTo(19f, 0f, 24f, 5f, 19f, 10f); lineTo(17f, 12f)
    } }
    val Info by lazy { icon("info") {
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f); arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        moveTo(12f, 11f); lineTo(12f, 17f); moveTo(12f, 7f); lineTo(12.1f, 7f)
    } }
    val ChevronRight by lazy { icon("chevron-right") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) } }
    val ChevronLeft by lazy { icon("chevron-left") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) } }
    val Edit by lazy { icon("edit") {
        moveTo(4f, 16f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(8f, 20f); lineTo(3f, 21f); close()
        moveTo(13f, 7f); lineTo(17f, 11f)
    } }
    val Star by lazy { icon("star") {
        moveTo(12f, 3f); lineTo(15f, 9f); lineTo(21f, 10f); lineTo(16.5f, 14.5f); lineTo(18f, 21f)
        lineTo(12f, 17.5f); lineTo(6f, 21f); lineTo(7.5f, 14.5f); lineTo(3f, 10f); lineTo(9f, 9f); close()
    } }
    val Pumpkin by lazy { icon("pumpkin") {
        moveTo(12f, 7f); quadTo(11.8f, 4.6f, 12.8f, 3.2f)
        moveTo(12.4f, 5.2f); quadTo(15.2f, 2.9f, 17.4f, 4.9f); quadTo(14.8f, 6.5f, 12.4f, 5.2f)
        moveTo(12f, 7f); curveTo(17f, 6f, 21f, 8.5f, 21f, 13.5f)
        curveTo(21f, 18.5f, 17f, 21f, 12f, 21f); curveTo(7f, 21f, 3f, 18.5f, 3f, 13.5f)
        curveTo(3f, 8.5f, 7f, 6f, 12f, 7f); close()
        moveTo(8.8f, 7.4f); quadTo(6.6f, 14f, 8.8f, 20.6f)
        moveTo(15.2f, 7.4f); quadTo(17.4f, 14f, 15.2f, 20.6f)
    } }
    val Snowflake by lazy { icon("snowflake") {
        repeat(6) { arm ->
            val angle = Math.toRadians(arm * 60.0 - 90.0)
            val dx = kotlin.math.cos(angle).toFloat(); val dy = kotlin.math.sin(angle).toFloat()
            moveTo(12f, 12f); lineTo(12f + dx * 9f, 12f + dy * 9f)
            for (side in listOf(-1f, 1f)) {
                val turn = angle + side * Math.toRadians(40.0)
                moveTo(12f + dx * 5.6f, 12f + dy * 5.6f)
                lineTo(12f + dx * 5.6f + kotlin.math.cos(turn).toFloat() * 3f, 12f + dy * 5.6f + kotlin.math.sin(turn).toFloat() * 3f)
            }
        }
    } }
    val Light by lazy { icon("light") {
        moveTo(8f, 16f); curveTo(8f, 13f, 5f, 12f, 5f, 8f); curveTo(5f, 0f, 19f, 0f, 19f, 8f); curveTo(19f, 12f, 16f, 13f, 16f, 16f); close()
        moveTo(9f, 20f); lineTo(15f, 20f); moveTo(10f, 23f); lineTo(14f, 23f)
    } }
    val Toggle by lazy { icon("toggle") {
        moveTo(12f, 3f); lineTo(12f, 12f); moveTo(6f, 6f); curveTo(-2f, 17f, 12f, 28f, 20f, 17f); curveTo(23f, 13f, 21f, 8f, 18f, 6f)
    } }
    val Climate by lazy { icon("climate") {
        moveTo(9f, 15f); lineTo(9f, 5f); curveTo(9f, 1f, 15f, 1f, 15f, 5f); lineTo(15f, 15f); curveTo(23f, 24f, 1f, 24f, 9f, 15f)
        moveTo(12f, 10f); lineTo(12f, 19f)
    } }
    val Sensor by lazy { icon("sensor") {
        moveTo(3f, 17f); lineTo(7f, 10f); lineTo(12f, 14f); lineTo(17f, 5f); lineTo(21f, 8f)
        moveTo(3f, 21f); lineTo(21f, 21f)
    } }
    val Sun by lazy { icon("sun") {
        moveTo(16f, 12f); arcTo(4f, 4f, 0f, true, true, 8f, 12f); arcTo(4f, 4f, 0f, true, true, 16f, 12f)
        moveTo(12f, 2f); lineTo(12f, 4f); moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(4f, 12f); moveTo(20f, 12f); lineTo(22f, 12f)
        moveTo(5f, 5f); lineTo(6f, 6f); moveTo(18f, 18f); lineTo(19f, 19f)
        moveTo(5f, 19f); lineTo(6f, 18f); moveTo(18f, 6f); lineTo(19f, 5f)
    } }
    val Moon by lazy { icon("moon") {
        moveTo(19f, 16f); curveTo(5f, 20f, 4f, 6f, 9f, 3f); curveTo(-7f, 10f, 9f, 31f, 19f, 16f)
    } }
    val Cloud by lazy { icon("cloud") {
        moveTo(6f, 18f); curveTo(-2f, 18f, 0f, 8f, 7f, 10f); curveTo(7f, 1f, 21f, 3f, 20f, 11f); curveTo(26f, 14f, 22f, 18f, 18f, 18f); close()
    } }
    val Rain by lazy { icon("rain") {
        moveTo(4f, 14f); curveTo(-1f, 9f, 6f, 5f, 9f, 8f); curveTo(11f, 0f, 23f, 5f, 20f, 11f); lineTo(22f, 14f)
        moveTo(7f, 16f); lineTo(5f, 21f); moveTo(13f, 16f); lineTo(11f, 21f); moveTo(19f, 16f); lineTo(17f, 21f)
    } }
    val Snow by lazy { icon("snow") {
        moveTo(12f, 2f); lineTo(12f, 22f); moveTo(3f, 7f); lineTo(21f, 17f); moveTo(3f, 17f); lineTo(21f, 7f)
        moveTo(8f, 4f); lineTo(12f, 7f); lineTo(16f, 4f); moveTo(8f, 20f); lineTo(12f, 17f); lineTo(16f, 20f)
    } }
    val Wind by lazy { icon("wind") {
        moveTo(2f, 8f); lineTo(17f, 8f); curveTo(23f, 8f, 22f, 1f, 17f, 3f)
        moveTo(2f, 12f); lineTo(14f, 12f); moveTo(2f, 16f); lineTo(18f, 16f); curveTo(24f, 16f, 21f, 23f, 17f, 21f)
    } }
    val Lightning by lazy { icon("lightning") { moveTo(14f, 2f); lineTo(4f, 14f); lineTo(11f, 14f); lineTo(10f, 22f); lineTo(21f, 9f); lineTo(14f, 9f); close() } }
    val Fog by lazy { icon("fog") { moveTo(3f, 6f); lineTo(21f, 6f); moveTo(5f, 12f); lineTo(19f, 12f); moveTo(3f, 18f); lineTo(21f, 18f) } }
    val Play by lazy { icon("play") { moveTo(8f, 4f); lineTo(20f, 12f); lineTo(8f, 20f); close() } }
    val Pause by lazy { icon("pause") { moveTo(8f, 4f); lineTo(8f, 20f); moveTo(16f, 4f); lineTo(16f, 20f) } }
    val Camera by lazy { icon("camera") { moveTo(3f, 7f); lineTo(16f, 7f); lineTo(16f, 17f); lineTo(3f, 17f); close(); moveTo(16f, 10f); lineTo(22f, 6f); lineTo(22f, 18f); lineTo(16f, 14f) } }
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

    /** Android's settings: eight teeth round an axle, so it never reads as Stova's own sliders. */
    val Gear: ImageVector by lazy {
        icon("gear") {
            for (i in 0 until 8) {
                val centre = i * 45.0
                listOf(-14.0 to 7.4f, -9.0 to 9.8f, 9.0 to 9.8f, 14.0 to 7.4f).forEachIndexed { k, (offset, radius) ->
                    val angle = Math.toRadians(centre + offset)
                    val x = 12f + radius * kotlin.math.cos(angle).toFloat(); val y = 12f + radius * kotlin.math.sin(angle).toFloat()
                    if (i == 0 && k == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            close()
            moveTo(15f, 12f); arcTo(3f, 3f, 0f, true, true, 9f, 12f); arcTo(3f, 3f, 0f, true, true, 15f, 12f)
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
