package app.stova.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.StovaColors
import app.stova.profiles.season
import kotlin.math.sin
import kotlin.random.Random

/** Where one app's web sits: [corner] 0..3 clockwise from top left, [reach] a share of the tile's short side. */
internal data class TileWeb(val corner: Int, val reach: Float, val spider: Boolean)
/** Each app its own web, the same every time: chosen from the app's key, not at random. */
internal fun tileWeb(seed: Int): TileWeb {
    val random = Random(seed)
    return TileWeb(random.nextInt(4), .28f + random.nextFloat() * .28f, random.nextInt(3) == 0)
}

/**
 * Max dresses each app tile's own box: snow on its top edge with a sprig of holly at Jul, a web in
 * one of its corners at Halloween. [seed] (the app's key) places them differently on every app.
 * Drawn inside the tile's clip, above the icon, and never moving.
 */
@Composable
fun Modifier.seasonalTile(seed: Int): Modifier {
    val p = LocalAppearance.current
    val touch = tileTouch(p.season(), p.seasonalDecor) ?: return this
    return drawWithContent {
        drawContent()
        when (touch) {
            SeasonTouch.SNOW_CAPS -> {
                val random = Random(seed)
                snowCap(random)
                val left = random.nextBoolean()
                holly(Offset(if (left) size.height * .16f else size.width - size.height * .36f, size.height * .1f), size.height * .2f)
            }
            SeasonTouch.TILE_WEBS -> web(tileWeb(seed))
            else -> Unit
        }
    }
}

private fun DrawScope.web(web: TileWeb) {
    val right = web.corner == 1 || web.corner == 2
    val bottom = web.corner >= 2
    val corner = Offset(if (right) size.width else 0f, if (bottom) size.height else 0f)
    val reach = minOf(size.width, size.height) * web.reach
    cobweb(corner, if (right) -1f else 1f, if (bottom) -1f else 1f, reach = reach, alpha = .5f)
    if (web.spider && !bottom) {
        // A small spider let down on its thread from the middle of the web.
        val x = corner.x + (if (right) -1f else 1f) * reach * .45f
        val body = Offset(x, reach * 1.25f)
        val tint = StovaColors.Spider.copy(alpha = .75f)
        drawLine(tint.copy(alpha = .4f), Offset(x, reach * .45f), body, density * .7f)
        drawOval(tint, body - Offset(2.5f * density, 3.5f * density), Size(5 * density, 7 * density))
        for (side in listOf(-1f, 1f)) repeat(3) { leg ->
            val from = body + Offset(side * 2 * density, (leg - 1f) * 1.5f * density)
            drawLine(tint, from, from + Offset(side * 4 * density, (leg - 1f) * 2.5f * density + 1.5f * density), density * .8f)
        }
    }
}

/** A band of snow along the top with a soft, uneven lower edge and two drips, placed by [random]. */
private fun DrawScope.snowCap(random: Random) {
    val depth = size.height * .09f
    val phase = random.nextFloat() * 6f
    val cap = Path().apply {
        moveTo(0f, 0f); lineTo(size.width, 0f)
        var x = size.width
        while (x >= 0f) {
            val dip = depth * (1f + .35f * sin(x / size.width * 17f + phase) + .2f * sin(x / size.width * 41f + 1f))
            lineTo(x, dip); x -= size.width / 48f
        }
        lineTo(0f, 0f); close()
    }
    val snow = StovaColors.Snow.copy(alpha = .92f)
    drawPath(cap, snow)
    repeat(2) {
        val x = size.width * (.3f + random.nextFloat() * .6f)
        val length = depth * (.4f + random.nextFloat() * .5f)
        drawCircle(snow, depth * .32f, Offset(x, depth + length))
        drawRect(snow, Offset(x - depth * .32f, depth * .9f), Size(depth * .64f, length))
    }
}

/** Two leaves and three red berries. */
private fun DrawScope.holly(center: Offset, size: Float) {
    for (angle in listOf(-35f, 35f)) rotate(angle, center) {
        val leaf = Path().apply {
            moveTo(center.x, center.y)
            quadraticTo(center.x + size * .55f, center.y - size * .32f, center.x + size, center.y)
            quadraticTo(center.x + size * .55f, center.y + size * .32f, center.x, center.y)
            close()
        }
        drawPath(leaf, StovaColors.Fir)
    }
    listOf(Offset(-.12f, .04f), Offset(.1f, .02f), Offset(0f, .2f)).forEach { (dx, dy) ->
        drawCircle(StovaColors.Bulbs[0], size * .16f, center + Offset(dx * size, dy * size))
    }
}
