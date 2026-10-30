package app.stova.kids

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import app.stova.core.theme.KidsColors as K
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The picture-book world behind the children's Home: a sky that follows the hour, hills that
 * follow the season and the things a child expects to find there. The world itself is one layer
 * that is drawn again only when the season or the hour changes; snow, leaves, petals and
 * fireflies drift above it only while [lively], and fade away when nobody is pressing anything.
 */
@Composable
fun KidsWorld(season: KidsSeason, part: DayPart, lively: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            sky(part)
            lights(part, season)
            if (part != DayPart.NIGHT) clouds(part)
            if (season == KidsSeason.HALLOWEEN && part != DayPart.DAY) bats()
            hills(season, part)
            props(season, part)
        }
        val drifting = lively && !LocalReduceMotion.current
        val shown by animateFloatAsState(if (drifting) 1f else 0f, tween(if (drifting) 900 else 2_400), label = "drift")
        if (shown > 0f) Particles(particleKind(season, part), shown, drifting)
    }
}

private enum class Drift { SNOW, LEAVES, PETALS, FIREFLIES }
private fun particleKind(season: KidsSeason, part: DayPart): Drift = when (season) {
    KidsSeason.WINTER, KidsSeason.CHRISTMAS -> Drift.SNOW
    KidsSeason.AUTUMN, KidsSeason.HALLOWEEN -> Drift.LEAVES
    KidsSeason.SUMMER -> if (part == DayPart.EVENING || part == DayPart.NIGHT) Drift.FIREFLIES else Drift.PETALS
    else -> Drift.PETALS
}

@Composable
private fun Particles(kind: Drift, shown: Float, moving: Boolean) {
    val seeds = remember(kind) { List(if (kind == Drift.SNOW) 26 else 16) { Seed(Random(it * 31 + kind.ordinal)) } }
    val time = if (moving) {
        rememberInfiniteTransition(label = "drift").animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "t")
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0.3f) }
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        val unit = size.minDimension / 540f
        for (s in seeds) {
            val fall = (s.y + t * s.speed) % 1f
            val sway = sin((t * s.speed * 6 + s.phase) * 2 * PI).toFloat() * s.sway * unit
            val at = Offset(s.x * size.width + sway, fall * size.height * 1.05f - 0.03f * size.height)
            when (kind) {
                Drift.SNOW -> drawCircle(K.Cloud.copy(alpha = 0.85f * shown), (1.6f + 2.6f * s.size) * unit, at)
                Drift.LEAVES -> {
                    val r = (4f + 3f * s.size) * unit
                    val leaf = Path().apply { moveTo(at.x, at.y); quadraticTo(at.x + r, at.y - r, at.x + 2 * r, at.y); quadraticTo(at.x + r, at.y + r, at.x, at.y); close() }
                    drawPath(leaf, (if (s.size > 0.5f) K.Scarf else K.Pumpkin).copy(alpha = 0.9f * shown))
                }
                Drift.PETALS -> drawOval((if (s.size > 0.5f) K.PetalPink else K.Petal).copy(alpha = 0.85f * shown),
                    at, Size((5f + 3f * s.size) * unit, (3f + 2f * s.size) * unit))
                Drift.FIREFLIES -> {
                    val hover = Offset(at.x, (s.y * 0.5f + 0.35f) * size.height + sway)
                    val blink = (sin((t * 40 + s.phase) * 2 * PI).toFloat() + 1f) / 2f
                    drawCircle(K.Firefly.copy(alpha = 0.25f * blink * shown), 8f * unit, hover)
                    drawCircle(K.Firefly.copy(alpha = (0.4f + 0.6f * blink) * shown), 2.4f * unit, hover)
                }
            }
        }
    }
}

private class Seed(r: Random) {
    val x = r.nextFloat(); val y = r.nextFloat(); val size = r.nextFloat()
    val speed = 1f + r.nextFloat() * 2f; val sway = 6f + r.nextFloat() * 14f; val phase = r.nextFloat()
}

// The world, drawn once.

private fun DrawScope.sky(part: DayPart) {
    val (top, low) = when (part) {
        DayPart.MORNING -> K.MorningTop to K.MorningLow
        DayPart.DAY -> K.DayTop to K.DayLow
        DayPart.EVENING -> K.EveningTop to K.EveningLow
        DayPart.NIGHT -> K.NightTop to K.NightLow
    }
    drawRect(Brush.verticalGradient(listOf(top, low), 0f, size.height * 0.5f))
}

private fun DrawScope.lights(part: DayPart, season: KidsSeason) {
    val u = size.height / 540f
    when (part) {
        DayPart.NIGHT -> {
            val stars = Random(7)
            repeat(46) {
                val at = Offset(stars.nextFloat() * size.width, stars.nextFloat() * size.height * 0.36f)
                val r = (0.7f + stars.nextFloat() * 1.6f) * u
                drawCircle(K.Star.copy(alpha = 0.5f + stars.nextFloat() * 0.5f), r, at)
                if (it % 9 == 0) drawPath(sparkle(at, r * 4f), K.Star.copy(alpha = 0.8f))
            }
            val moon = Offset(size.width * 0.86f, size.height * 0.12f)
            val r = (if (season == KidsSeason.HALLOWEEN) 34f else 26f) * u
            drawCircle(K.Moon.copy(alpha = 0.18f), r * 1.8f, moon)
            drawCircle(K.Moon, r, moon)
            drawCircle(K.MoonShade, r * 0.22f, moon + Offset(-r * 0.3f, -r * 0.2f))
            drawCircle(K.MoonShade, r * 0.14f, moon + Offset(r * 0.35f, r * 0.25f))
            drawCircle(K.MoonShade, r * 0.1f, moon + Offset(-r * 0.1f, r * 0.45f))
        }
        else -> {
            val (at, r, tint) = when (part) {
                DayPart.MORNING -> Triple(Offset(size.width * 0.14f, size.height * 0.24f), 24f * u, K.SunGlow)
                DayPart.EVENING -> Triple(Offset(size.width * 0.82f, size.height * 0.30f), 36f * u, lerp(K.Sun, K.EveningLow, 0.45f))
                else -> Triple(Offset(size.width * 0.87f, size.height * 0.11f), 28f * u, K.Sun)
            }
            drawCircle(K.SunGlow.copy(alpha = 0.22f), r * 2.4f, at)
            drawCircle(K.SunGlow.copy(alpha = 0.35f), r * 1.6f, at)
            if (part == DayPart.DAY) for (i in 0 until 10) {
                val a = i * 2 * PI / 10
                val from = at + Offset((r * 1.35f * kotlin.math.cos(a)).toFloat(), (r * 1.35f * sin(a)).toFloat())
                val to = at + Offset((r * 1.75f * kotlin.math.cos(a)).toFloat(), (r * 1.75f * sin(a)).toFloat())
                drawLine(K.Sun, from, to, 4f * u, StrokeCap.Round)
            }
            drawCircle(tint, r, at)
        }
    }
}

private fun DrawScope.clouds(part: DayPart) {
    val u = size.height / 540f
    val tint = if (part == DayPart.EVENING) lerp(K.Cloud, K.EveningLow, 0.35f) else K.Cloud
    for ((x, y, s) in listOf(Triple(0.30f, 0.10f, 1f), Triple(0.58f, 0.19f, 0.75f), Triple(0.08f, 0.08f, 0.6f))) {
        val at = Offset(size.width * x, size.height * y)
        val c = tint.copy(alpha = 0.9f)
        drawRoundRect(c, at + Offset(-44f * s * u, 0f), Size(96f * s * u, 22f * s * u), CornerRadius(11f * s * u))
        drawCircle(c, 20f * s * u, at + Offset(-18f * s * u, 2f * s * u))
        drawCircle(c, 26f * s * u, at + Offset(8f * s * u, -6f * s * u))
        drawCircle(c, 17f * s * u, at + Offset(32f * s * u, 6f * s * u))
    }
}

private fun DrawScope.bats() {
    val u = size.height / 540f
    for ((x, y, s) in listOf(Triple(0.62f, 0.10f, 1f), Triple(0.70f, 0.17f, 0.7f), Triple(0.55f, 0.20f, 0.6f))) {
        val at = Offset(size.width * x, size.height * y); val w = 18f * s * u
        val bat = Path().apply {
            moveTo(at.x, at.y)
            quadraticTo(at.x - w * 0.5f, at.y - w * 0.6f, at.x - w, at.y - w * 0.2f)
            quadraticTo(at.x - w * 0.7f, at.y + w * 0.05f, at.x - w * 0.5f, at.y + w * 0.25f)
            quadraticTo(at.x - w * 0.25f, at.y + w * 0.05f, at.x, at.y + w * 0.3f)
            quadraticTo(at.x + w * 0.25f, at.y + w * 0.05f, at.x + w * 0.5f, at.y + w * 0.25f)
            quadraticTo(at.x + w * 0.7f, at.y + w * 0.05f, at.x + w, at.y - w * 0.2f)
            quadraticTo(at.x + w * 0.5f, at.y - w * 0.6f, at.x, at.y)
            close()
        }
        drawPath(bat, K.Shadow.copy(alpha = 0.85f))
    }
}

private fun hillColours(season: KidsSeason): List<Color> = when (season) {
    KidsSeason.WINTER, KidsSeason.CHRISTMAS -> K.SnowHills
    KidsSeason.AUTUMN -> K.AutumnHills
    KidsSeason.HALLOWEEN -> K.DuskHills
    KidsSeason.SUMMER -> K.SummerHills
    else -> K.SpringHills
}

private fun veil(part: DayPart) = when (part) { DayPart.NIGHT -> 0.55f; DayPart.EVENING -> 0.22f; DayPart.MORNING -> 0.05f; DayPart.DAY -> 0f }

/** The height of hill [layer] at [x]: gentle waves, each layer its own. */
private fun DrawScope.hillAt(layer: Int, x: Float): Float {
    val base = floatArrayOf(0.30f, 0.42f, 0.58f)[layer]
    val amp = floatArrayOf(0.045f, 0.04f, 0.035f)[layer]
    val wave = floatArrayOf(1.4f, 1.9f, 2.6f)[layer]
    val shift = floatArrayOf(0.2f, 1.3f, 2.1f)[layer]
    val f = x / size.width
    return size.height * (base - amp * sin((f * wave + shift) * PI).toFloat() - amp * 0.4f * sin((f * wave * 2.3f + shift) * PI).toFloat())
}

private fun DrawScope.hills(season: KidsSeason, part: DayPart) {
    val colours = hillColours(season)
    for (layer in 0..2) {
        val hill = Path().apply {
            moveTo(0f, size.height); lineTo(0f, hillAt(layer, 0f))
            var x = 0f
            while (x <= size.width) { lineTo(x, hillAt(layer, x)); x += 16f }
            lineTo(size.width, hillAt(layer, size.width)); lineTo(size.width, size.height); close()
        }
        drawPath(hill, lerp(colours[layer], K.NightVeil, veil(part)))
        // A thin light along the ridge gives each hill its edge, like a picture book.
        val ridge = Path().apply {
            moveTo(0f, hillAt(layer, 0f)); var x = 0f
            while (x <= size.width) { lineTo(x, hillAt(layer, x)); x += 16f }
        }
        drawPath(ridge, K.Ridge.copy(alpha = if (part == DayPart.NIGHT) 0.16f else 0.22f), style = Stroke(2.5f * size.height / 540f))
    }
}

private fun DrawScope.props(season: KidsSeason, part: DayPart) {
    val u = size.height / 540f
    val dark = veil(part)
    fun on(layer: Int, fx: Float) = Offset(size.width * fx, hillAt(layer, size.width * fx))
    when (season) {
        KidsSeason.WINTER, KidsSeason.CHRISTMAS -> {
            val lit = season == KidsSeason.CHRISTMAS
            for ((fx, s) in listOf(0.05f to 1.1f, 0.12f to 0.8f, 0.38f to 0.9f, 0.70f to 1f, 0.94f to 1.2f)) fir(on(0, fx), s * u, dark, lit)
            snowman(on(1, 0.965f), u, dark)
        }
        KidsSeason.HALLOWEEN -> {
            bareTree(on(0, 0.08f), u * 1.2f)
            for ((fx, s) in listOf(0.03f to 1.2f, 0.25f to 0.8f, 0.52f to 0.9f, 0.78f to 1f, 0.96f to 1.1f)) pumpkin(on(0, fx), s * u, part != DayPart.DAY)
        }
        KidsSeason.AUTUMN -> for ((fx, s) in listOf(0.04f to 1.1f, 0.30f to 0.8f, 0.62f to 1f, 0.92f to 1.2f)) roundTree(on(0, fx), s * u, lerp(K.Scarf, K.NightVeil, dark), dark)
        else -> {
            val crown = if (season == KidsSeason.SUMMER) K.Leaf else lerp(K.Leaf, K.Shell, 0.25f)
            for ((fx, s) in listOf(0.04f to 1.1f, 0.33f to 0.8f, 0.66f to 1f, 0.93f to 1.2f)) {
                roundTree(on(0, fx), s * u, lerp(crown, K.NightVeil, dark), dark)
                if (season != KidsSeason.SUMMER) for (i in 0..4) drawCircle(lerp(K.PetalPink, K.NightVeil, dark), 3f * s * u,
                    on(0, fx) + Offset((i - 2) * 9f * s * u, (-44f - (i % 2) * 12f) * s * u))
            }
            val flowers = Random(3)
            repeat(30) {
                val fx = flowers.nextFloat()
                val at = on(1, fx) + Offset(0f, (6f + flowers.nextFloat() * 30f) * u)
                drawCircle(lerp(if (it % 3 == 0) K.Petal else K.PetalPink, K.NightVeil, dark), (2.5f + flowers.nextFloat() * 2f) * u, at)
            }
            if (season == KidsSeason.EASTER) for ((i, fx) in listOf(0.02f, 0.18f, 0.48f, 0.83f, 0.97f).withIndex()) egg(on(0, fx), u, K.Egg[i % K.Egg.size], dark)
            if (season == KidsSeason.MAY_17) for (fx in listOf(0.10f, 0.50f, 0.88f)) flagPole(on(0, fx), u)
        }
    }
}

private fun DrawScope.fir(foot: Offset, s: Float, dark: Float, lit: Boolean) {
    val green = lerp(StovaColors.Fir, K.NightVeil, dark * 0.6f)
    drawRect(lerp(K.Trunk, K.NightVeil, dark), Offset(foot.x - 4f * s, foot.y - 12f * s), Size(8f * s, 14f * s))
    for (i in 0..2) {
        val w = (40f - i * 9f) * s; val top = foot.y - (24f + i * 18f) * s - 14f * s
        val tier = Path().apply { moveTo(foot.x, top); lineTo(foot.x + w / 2, top + 26f * s); lineTo(foot.x - w / 2, top + 26f * s); close() }
        drawPath(tier, green)
        drawLine(K.Cloud.copy(alpha = 0.85f), Offset(foot.x - w / 2.4f, top + 24f * s), Offset(foot.x + w / 2.4f, top + 24f * s), 3f * s, StrokeCap.Round)
    }
    if (lit) {
        for ((i, c) in StovaColors.Bulbs.withIndex()) drawCircle(c, 2.4f * s, Offset(foot.x + (i - 2) * 6f * s, foot.y - (22f + (i % 3) * 14f) * s))
        drawPath(sparkle(Offset(foot.x, foot.y - 78f * s), 7f * s), K.Star)
    }
}

private fun DrawScope.snowman(foot: Offset, u: Float, dark: Float) {
    val snow = lerp(K.Cloud, K.NightVeil, dark * 0.4f)
    drawCircle(snow, 16f * u, foot + Offset(0f, -12f * u)); drawCircle(snow, 11f * u, foot + Offset(0f, -34f * u))
    drawCircle(K.Shadow, 1.6f * u, foot + Offset(-4f * u, -36f * u)); drawCircle(K.Shadow, 1.6f * u, foot + Offset(4f * u, -36f * u))
    drawLine(K.Pumpkin, foot + Offset(0f, -33f * u), foot + Offset(9f * u, -31f * u), 3f * u, StrokeCap.Round)
    drawRoundRect(K.Scarf, foot + Offset(-11f * u, -27f * u), Size(22f * u, 5f * u), CornerRadius(2.5f * u))
}

private fun DrawScope.pumpkin(foot: Offset, s: Float, glowing: Boolean) {
    val c = foot + Offset(0f, -11f * s)
    if (glowing) drawCircle(K.Firefly.copy(alpha = 0.18f), 26f * s, c)
    drawOval(K.PumpkinDeep, c + Offset(-17f * s, -12f * s), Size(34f * s, 24f * s))
    drawOval(K.Pumpkin, c + Offset(-11f * s, -12f * s), Size(22f * s, 24f * s))
    drawRoundRect(K.Stem, c + Offset(-2f * s, -18f * s), Size(4f * s, 8f * s), CornerRadius(2f * s))
    if (glowing) {
        val face = K.Firefly
        drawPath(Path().apply { moveTo(c.x - 9f * s, c.y - 2f * s); lineTo(c.x - 5f * s, c.y - 7f * s); lineTo(c.x - 2f * s, c.y - 2f * s); close() }, face)
        drawPath(Path().apply { moveTo(c.x + 9f * s, c.y - 2f * s); lineTo(c.x + 5f * s, c.y - 7f * s); lineTo(c.x + 2f * s, c.y - 2f * s); close() }, face)
        drawArc(face, 10f, 160f, true, c + Offset(-8f * s, -2f * s), Size(16f * s, 10f * s))
    }
}

private fun DrawScope.bareTree(foot: Offset, s: Float) {
    val wood = K.Shadow.copy(alpha = 0.85f)
    drawLine(wood, foot, foot + Offset(0f, -56f * s), 7f * s, StrokeCap.Round)
    drawLine(wood, foot + Offset(0f, -30f * s), foot + Offset(-20f * s, -50f * s), 4f * s, StrokeCap.Round)
    drawLine(wood, foot + Offset(0f, -40f * s), foot + Offset(18f * s, -62f * s), 4f * s, StrokeCap.Round)
    drawLine(wood, foot + Offset(-12f * s, -42f * s), foot + Offset(-24f * s, -40f * s), 2.5f * s, StrokeCap.Round)
}

private fun DrawScope.roundTree(foot: Offset, s: Float, crown: Color, dark: Float) {
    drawRoundRect(lerp(K.Trunk, K.NightVeil, dark), Offset(foot.x - 4f * s, foot.y - 26f * s), Size(8f * s, 28f * s), CornerRadius(3f * s))
    drawCircle(crown, 20f * s, foot + Offset(0f, -42f * s))
    drawCircle(crown, 15f * s, foot + Offset(-14f * s, -32f * s))
    drawCircle(crown, 15f * s, foot + Offset(14f * s, -32f * s))
    drawCircle(K.Cloud.copy(alpha = 0.12f), 9f * s, foot + Offset(-6f * s, -50f * s))
}

private fun DrawScope.egg(foot: Offset, u: Float, colour: Color, dark: Float) {
    val c = lerp(colour, K.NightVeil, dark)
    drawOval(c, foot + Offset(-8f * u, -22f * u), Size(16f * u, 21f * u))
    val zig = Path().apply {
        moveTo(foot.x - 8f * u, foot.y - 12f * u)
        for (i in 1..4) lineTo(foot.x - 8f * u + i * 4f * u, foot.y - if (i % 2 == 0) 12f * u else 15f * u)
    }
    drawPath(zig, K.Cloud.copy(alpha = 0.8f), style = Stroke(1.6f * u))
}

private fun DrawScope.flagPole(foot: Offset, u: Float) {
    drawLine(K.Shell, foot, foot + Offset(0f, -64f * u), 2.5f * u, StrokeCap.Round)
    val at = foot + Offset(1f, -64f * u); val w = 26f * u; val h = 19f * u
    drawRect(K.FlagRed, at, Size(w, h))
    drawRect(K.FlagWhite, at + Offset(w * 0.27f, 0f), Size(w * 0.18f, h)); drawRect(K.FlagWhite, at + Offset(0f, h * 0.38f), Size(w, h * 0.25f))
    drawRect(K.FlagBlue, at + Offset(w * 0.31f, 0f), Size(w * 0.09f, h)); drawRect(K.FlagBlue, at + Offset(0f, h * 0.44f), Size(w, h * 0.12f))
}
