package app.stova.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.stova.core.theme.*
import app.stova.profiles.*
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Activity-level reporting sees keys even inside a dialog and never consumes them. */
object SeasonalInput {
    val generation = MutableStateFlow(0L)
    fun report() { generation.value += 1 }
}

internal fun seasonalMotionEnabled(season: Season, decor: SeasonalDecor, reduced: Boolean, started: Boolean) =
    season != Season.NONE && decor == SeasonalDecor.FULL && !reduced && started

/** After this long without a key, the particles fade out and the clock rests until the next key. */
internal const val SEASON_REST_SECONDS = 120f
internal const val SEASON_FADE_SECONDS = 3f
internal const val SPIDER_RETRACT_SECONDS = .8f

/** 0..1: fades in after waking, fades out once Home has been left alone for the rest time. */
internal fun seasonalPresence(idleSeconds: Float, awakeSeconds: Float) =
    min((awakeSeconds / SEASON_FADE_SECONDS).coerceIn(0f, 1f), 1f - ((idleSeconds - SEASON_REST_SECONDS) / SEASON_FADE_SECONDS).coerceIn(0f, 1f))

/** A key winds a hanging spider back up instead of removing it. */
internal fun spiderShown(idleSeconds: Float, retractFrom: Float, sinceKey: Float) =
    max(spiderDrop(idleSeconds), retractFrom * (1f - sinceKey / SPIDER_RETRACT_SECONDS).coerceIn(0f, 1f))

@Stable
class SeasonalClock internal constructor() {
    val time = mutableFloatStateOf(0f)
    var idleStart by mutableFloatStateOf(0f)
    var wokeAt by mutableFloatStateOf(0f)
    var retractFrom by mutableFloatStateOf(0f)
    val idle get() = time.floatValue - idleStart
    val presence get() = seasonalPresence(idle, time.floatValue - wokeAt)
    val spider get() = spiderShown(idle, retractFrom, idle)
    val resting get() = idle >= SEASON_REST_SECONDS + SEASON_FADE_SECONDS
}

/**
 * A draw-phase clock: at most one update per 50 ms, no frame work while hidden or reduced,
 * and none while resting. Keys never restart the loop, so holding a direction keeps the motion smooth.
 */
@Composable
fun rememberSeasonalClock(): SeasonalClock {
    val p = LocalAppearance.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val enabled = seasonalMotionEnabled(p.season(), p.seasonalDecor, LocalReduceMotion.current, lifecycle.isAtLeast(Lifecycle.State.STARTED))
    val clock = remember { SeasonalClock() }
    val input by SeasonalInput.generation.collectAsState()
    LaunchedEffect(input) {
        val now = clock.time.floatValue
        if (clock.resting) clock.wokeAt = now
        clock.retractFrom = clock.spider
        clock.idleStart = now
    }
    LaunchedEffect(enabled, p.season()) {
        if (!enabled) return@LaunchedEffect
        clock.idleStart = clock.time.floatValue
        var previous = 0L
        while (true) {
            if (clock.resting) {
                snapshotFlow { clock.resting }.first { !it }
                previous = 0L
            }
            withFrameMillis { frame ->
                if (previous != 0L) clock.time.floatValue += ((frame - previous) / 1000f).coerceAtMost(.1f)
                previous = frame
            }
            delay(50)
        }
    }
    return clock
}

@Composable
internal fun rememberSeasonalTime(): State<Float> = rememberSeasonalClock().time

/** What Max puts on Home for a season: many different things, not only more snow. Calm and Off show none. */
internal enum class SeasonTouch {
    SNOW, LIGHTS, BAUBLES, STARS, SHOOTING_STAR, SLEIGH, DRIFT, TREES, SNOW_CAPS,
    EMBERS, BATS, COBWEBS, SPIDER, MIST, LANTERNS, GHOST, WITCH, TILE_WEBS,
}
internal fun seasonTouches(season: Season): Set<SeasonTouch> = when (season) {
    Season.CHRISTMAS -> setOf(SeasonTouch.SNOW, SeasonTouch.LIGHTS, SeasonTouch.BAUBLES, SeasonTouch.STARS,
        SeasonTouch.SHOOTING_STAR, SeasonTouch.SLEIGH, SeasonTouch.DRIFT, SeasonTouch.TREES, SeasonTouch.SNOW_CAPS)
    Season.HALLOWEEN -> setOf(SeasonTouch.EMBERS, SeasonTouch.BATS, SeasonTouch.COBWEBS, SeasonTouch.SPIDER,
        SeasonTouch.MIST, SeasonTouch.LANTERNS, SeasonTouch.GHOST, SeasonTouch.WITCH, SeasonTouch.TILE_WEBS)
    Season.NONE -> emptySet()
}
/** The dressing on an app tile's own box; it never moves, so it stays under reduced motion too. */
internal fun tileTouch(season: Season, decor: SeasonalDecor): SeasonTouch? =
    if (decor != SeasonalDecor.FULL) null else when (season) {
        Season.CHRISTMAS -> SeasonTouch.SNOW_CAPS
        Season.HALLOWEEN -> SeasonTouch.TILE_WEBS
        Season.NONE -> null
    }

internal data class SeasonParticle(val x: Float, val start: Float, val radius: Float, val speed: Float, val phase: Float)
/**
 * Spread over the whole screen. At Jul: far specks, middle flakes and a few large near crystals,
 * so the snow has depth without blur. Larger flakes fall faster.
 */
internal fun seasonParticles(season: Season): List<SeasonParticle> {
    val random = Random(season.ordinal * 7919)
    fun layer(count: Int, smallest: Float, largest: Float) = List(count) {
        val radius = smallest + random.nextFloat() * (largest - smallest)
        SeasonParticle(random.nextFloat(), random.nextFloat(), radius, .012f + radius * .009f, random.nextFloat() * 2f * PI.toFloat())
    }
    return if (season == Season.CHRISTMAS) layer(84, .5f, .95f) + layer(60, 1.2f, 2.4f) + layer(18, 4f, 6f)
    else layer(90, .8f, 3f)
}

/** Bats crossing the upper screen on long, slow loops at Halloween. */
internal data class SeasonBat(val start: Float, val y: Float, val size: Float, val speed: Float, val phase: Float)
internal fun seasonBats(season: Season): List<SeasonBat> {
    if (season != Season.HALLOWEEN) return emptyList()
    val random = Random(31)
    return List(7) { SeasonBat(random.nextFloat(), .08f + random.nextFloat() * .34f, 7f + random.nextFloat() * 9f,
        .014f + random.nextFloat() * .014f, random.nextFloat() * 2f * PI.toFloat()) }
}
internal fun spiderDrop(idleSeconds: Float) = ((idleSeconds - 45f) / 5f).coerceIn(0f, 1f)

/** One bulb on the garland: [x] across the screen (0..1), [y] in dp from the top, [colour] an index into the bulbs. */
internal data class LightBulb(val x: Float, val y: Float, val colour: Int, val phase: Float)
private const val GARLAND_SWAGS = 5
private const val GARLAND_TOP = 3f
private const val GARLAND_SAG = 13f
/** Height of the wire in dp at [x] (0..1): five swags dipping from 3 to 16 dp, above the top bar's controls at 28 dp. */
internal fun garlandY(x: Float): Float { val t = (x * GARLAND_SWAGS) % 1f; return GARLAND_TOP + GARLAND_SAG * 4f * t * (1f - t) }
/** A bulb about every 38 dp across the full width, colours in turn. */
internal fun christmasLights(widthDp: Float): List<LightBulb> {
    val count = (widthDp / 38f).roundToInt().coerceAtLeast(GARLAND_SWAGS)
    return List(count + 1) { i -> val x = i / count.toFloat(); LightBulb(x, garlandY(x), i % StovaColors.Bulbs.size, i * 1.7f) }
}

internal data class SeasonStar(val x: Float, val y: Float, val size: Float, val speed: Float, val phase: Float)
/** Small four-point stars in the upper sky, each twinkling at its own pace. */
internal fun seasonStars(): List<SeasonStar> {
    val random = Random(1224)
    return List(16) { SeasonStar(random.nextFloat(), .1f + random.nextFloat() * .32f, 2.5f + random.nextFloat() * 3.5f,
        .6f + random.nextFloat() * .9f, random.nextFloat() * 2f * PI.toFloat()) }
}

/** 0..1 through a visitor's crossing, or null while it is away: it comes every [every] seconds and stays [stay]. */
private fun visit(seconds: Float, every: Float, stay: Float, offset: Float = 0f): Float? {
    val t = (seconds + offset) % every
    return if (t < stay) t / stay else null
}

/**
 * One Canvas with no input or accessibility nodes, covering the whole screen. Home draws it twice
 * on one [clock]: behind its content (sky, far snow, embers) and, with [front], over it, where the
 * edges, the near snow and the visitors stay see-through so the cards remain readable.
 */
@Composable
fun SeasonalScene(modifier: Modifier = Modifier, front: Boolean = false, clock: SeasonalClock = rememberSeasonalClock()) {
    val p = LocalAppearance.current
    val season = p.season()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!seasonalMotionEnabled(season, p.seasonalDecor, LocalReduceMotion.current, lifecycle.isAtLeast(Lifecycle.State.STARTED))) return
    val particles = remember(season) { seasonParticles(season) }
    val bats = remember(season) { seasonBats(season) }
    val stars = remember { seasonStars() }
    Canvas(modifier.clearAndSetSemantics { }) {
        val seconds = clock.time.floatValue
        val presence = clock.presence
        if (season == Season.CHRISTMAS) christmas(seconds, presence, particles, stars, front)
        else halloween(seconds, presence, particles, bats, clock.spider, front)
    }
}

private fun DrawScope.christmas(seconds: Float, presence: Float, snow: List<SeasonParticle>, stars: List<SeasonStar>, front: Boolean) {
    val dp = density
    // Behind: the sky and the far snow. In front: the near snow, the edges and the sleigh.
    if (!front && presence > 0f) stars.forEach { star ->
        val glint = sin(seconds * star.speed + star.phase).coerceAtLeast(0f)
        if (glint > 0f) fourPointStar(Offset(star.x * size.width, star.y * size.height), star.size * dp * (.6f + glint * .4f),
            StovaColors.Snow.copy(alpha = glint * glint * .7f * presence))
    }
    if (!front && presence > 0f) visit(seconds, 23f, 1.4f)?.let { t ->
        // A shooting star from the upper right, its tail fading behind it.
        val start = Offset(size.width * .82f, size.height * .08f)
        val head = start + Offset(-size.width * .22f, size.height * .12f) * t
        val tail = Offset(size.width * .05f, -size.height * .028f)
        repeat(6) { i -> drawLine(StovaColors.Snow.copy(alpha = (1f - i / 6f) * .8f * (1f - t) * presence),
            head + tail * (i / 6f), head + tail * ((i + 1) / 6f), dp * (1.6f - i * .2f), StrokeCap.Round) }
    }
    if (front && presence > 0f) visit(seconds, 47f, 16f, offset = 30f)?.let { t -> sleigh(t, seconds) }
    if (presence > 0f) snow.forEach { flake ->
        if ((flake.radius >= 1.2f) != front) return@forEach
        val travelled = (flake.start + seconds * flake.speed) % 1f
        val y = -.04f + travelled * 1.08f
        val x = flake.x + sin(seconds * .5f + flake.phase) * (.008f + flake.radius * .003f)
        // Only the screen edges fade; nearer (larger) flakes are a little brighter.
        val fade = minOf(1f, travelled * 8f, (1f - travelled) * 8f) * presence * (.45f + minOf(flake.radius, 3f) * .15f)
        val center = Offset(x * size.width, y * size.height)
        val radius = flake.radius * dp
        val tint = StovaColors.Snow.copy(alpha = fade * if (front) .5f else .6f)
        when {
            flake.radius < 2.2f -> drawCircle(tint, radius * .8f, center)
            flake.radius < 4f -> repeat(3) { arm ->
                val angle = arm * PI.toFloat() / 3f + flake.phase
                val offset = Offset(cos(angle), sin(angle)) * radius * 1.6f
                drawLine(tint, center - offset, center + offset, dp, StrokeCap.Round)
            }
            else -> crystal(center, radius * 1.5f, flake.phase + seconds * .25f, tint)
        }
    }
    if (!front) return
    drift()
    tree(Offset(24 * dp, size.height - 6 * dp), 58 * dp, seconds)
    tree(Offset(size.width - 26 * dp, size.height - 5 * dp), 44 * dp, seconds + 1.3f)
    garland(seconds)
}

/** Six arms with side branches, slowly turning: the near flakes that make the snow feel close. */
private fun DrawScope.crystal(center: Offset, arm: Float, angle: Float, tint: Color) {
    repeat(6) { i ->
        val a = angle + i * PI.toFloat() / 3f
        val dir = Offset(cos(a), sin(a))
        drawLine(tint, center, center + dir * arm, density * 1.2f, StrokeCap.Round)
        val base = center + dir * arm * .58f
        for (side in listOf(-1f, 1f)) {
            val b = a + side * PI.toFloat() / 4f
            drawLine(tint, base, base + Offset(cos(b), sin(b)) * arm * .34f, density, StrokeCap.Round)
        }
    }
}

private fun DrawScope.fourPointStar(center: Offset, radius: Float, tint: Color) {
    drawLine(tint, center - Offset(radius, 0f), center + Offset(radius, 0f), density, StrokeCap.Round)
    drawLine(tint, center - Offset(0f, radius), center + Offset(0f, radius), density, StrokeCap.Round)
    drawCircle(tint, density * .9f, center)
}

/** A string of lights across the very top with a glass bauble swaying under each swag. */
private fun DrawScope.garland(seconds: Float) {
    val dp = density
    val wire = Path().apply {
        var x = 0f
        moveTo(0f, garlandY(0f) * dp)
        while (x < size.width) { x += 6 * dp; lineTo(x, garlandY(x / size.width) * dp) }
    }
    drawPath(wire, StovaColors.Wire, style = Stroke(1.5f * dp))
    christmasLights(size.width / dp).forEach { bulb ->
        val twinkle = .6f + .4f * sin(seconds * 1.6f + bulb.phase)
        val colour = StovaColors.Bulbs[bulb.colour]
        val top = Offset(bulb.x * size.width, bulb.y * dp)
        drawCircle(colour.copy(alpha = .16f * twinkle), 8 * dp, top + Offset(0f, 5 * dp))
        drawRect(StovaColors.Wire, top - Offset(1.5f * dp, 0f), Size(3 * dp, 2.5f * dp))
        drawOval(colour.copy(alpha = .55f + .45f * twinkle), top + Offset(-2.5f * dp, 2 * dp), Size(5 * dp, 7.5f * dp))
    }
    repeat(GARLAND_SWAGS) { i ->
        val x = (i + .5f) / GARLAND_SWAGS
        val hook = Offset(x * size.width, garlandY(x) * dp)
        val sway = sin(seconds * .8f + i) * .14f
        val ball = hook + Offset(sin(sway), cos(sway)) * (6 * dp)
        val colour = StovaColors.Bulbs[(i * 2) % StovaColors.Bulbs.size]
        drawLine(StovaColors.Wire, hook, ball, dp * .8f)
        drawCircle(colour, 4.5f * dp, ball + Offset(0f, 3 * dp))
        drawCircle(StovaColors.Snow.copy(alpha = .7f), 1.2f * dp, ball + Offset(-1.4f * dp, 1.6f * dp))
    }
}

/** Soft mounds of settled snow along the bottom edge, behind the rows. */
private fun DrawScope.drift() {
    val dp = density
    val ground = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width) {
            val rise = (11f + 5f * sin(x / dp * .021f) + 3.5f * sin(x / dp * .057f + 1f)) * dp
            lineTo(x, size.height - rise); x += 8 * dp
        }
        lineTo(size.width, size.height); close()
    }
    drawPath(ground, StovaColors.Snow.copy(alpha = .5f))
}

/** A small fir with snow on its tiers, a gold star and a few lights, standing in the drift. */
private fun DrawScope.tree(foot: Offset, height: Float, seconds: Float) {
    repeat(3) { i ->
        val top = foot.y - height * (1f - i * .26f)
        val bottom = foot.y - height * (.32f - i * .16f).coerceAtLeast(.08f)
        val half = height * (.22f + i * .1f)
        val tier = Path().apply { moveTo(foot.x, top); lineTo(foot.x + half, bottom); lineTo(foot.x - half, bottom); close() }
        drawPath(tier, StovaColors.Fir)
        val cap = Path().apply {
            moveTo(foot.x, top); lineTo(foot.x + half * .35f, top + (bottom - top) * .35f)
            lineTo(foot.x - half * .35f, top + (bottom - top) * .35f); close()
        }
        drawPath(cap, StovaColors.Snow.copy(alpha = .8f))
        repeat(2) { k ->
            val light = Offset(foot.x + half * (if (k == 0) -.5f else .45f), bottom - (bottom - top) * .2f)
            drawCircle(StovaColors.Bulbs[(i + k) % StovaColors.Bulbs.size].copy(alpha = .6f + .4f * sin(seconds * 2f + i + k)),
                1.6f * density, light)
        }
    }
    fourPointStar(Offset(foot.x, foot.y - height - 2 * density), 4 * density, StovaColors.Bulbs[2])
}

/** Santa's sleigh and three reindeer crossing high, right to left, as a moonlit silhouette. */
private fun DrawScope.sleigh(t: Float, seconds: Float) {
    val dp = density
    val x = size.width * (1.12f - t * 1.3f)
    val y = size.height * .17f + sin(seconds * 1.4f) * 4 * dp
    val tint = StovaColors.Snow.copy(alpha = .5f * minOf(1f, t * 8f, (1f - t) * 8f))
    repeat(3) { i ->
        val deer = Offset(x - (26 + i * 22) * dp, y - (6 + i * 3) * dp + sin(seconds * 5f + i) * 1.5f * dp)
        drawOval(tint, deer - Offset(7 * dp, 3 * dp), Size(14 * dp, 6 * dp))
        drawCircle(tint, 2.6f * dp, deer + Offset(-8 * dp, -4 * dp))
        drawLine(tint, deer + Offset(-9 * dp, -6 * dp), deer + Offset(-12 * dp, -11 * dp), dp)
        drawLine(tint, deer + Offset(-7 * dp, -6 * dp), deer + Offset(-5 * dp, -11 * dp), dp)
        val stride = sin(seconds * 9f + i) * 3 * dp
        for (leg in listOf(-5f, 5f)) drawLine(tint, deer + Offset(leg * dp, 2 * dp), deer + Offset(leg * dp + stride, 8 * dp), dp)
    }
    drawLine(tint, Offset(x - 22 * dp, y - 4 * dp), Offset(x - 70 * dp, y - 12 * dp), dp * .6f)
    val body = Path().apply {
        moveTo(x - 20 * dp, y - 6 * dp); lineTo(x + 8 * dp, y - 6 * dp); lineTo(x + 12 * dp, y - 16 * dp)
        lineTo(x + 16 * dp, y - 16 * dp); lineTo(x + 12 * dp, y + 2 * dp); lineTo(x - 16 * dp, y + 2 * dp); close()
    }
    drawPath(body, tint)
    drawLine(tint, Offset(x - 22 * dp, y + 6 * dp), Offset(x + 14 * dp, y + 6 * dp), dp * 1.2f, StrokeCap.Round)
    drawCircle(tint, 4 * dp, Offset(x - 2 * dp, y - 11 * dp))
}

private fun DrawScope.halloween(seconds: Float, presence: Float, embers: List<SeasonParticle>, bats: List<SeasonBat>, drop: Float, front: Boolean) {
    val dp = density
    // Behind: most embers. In front: the webs, a few embers, bats, the visitors, mist and lanterns.
    if (front) {
        cobweb(Offset.Zero, 1f, 1f)
        cobweb(Offset(size.width, 0f), -1f, 1f)
    }
    if (front && presence > 0f) visit(seconds, 53f, 14f, offset = 20f)?.let { t -> witch(t, seconds) }
    if (presence > 0f) embers.forEachIndexed { index, ember ->
        if ((index % 4 == 0) != front) return@forEachIndexed
        val travelled = (ember.start + seconds * ember.speed) % 1f
        val y = -.04f + (1f - travelled) * 1.08f
        val x = ember.x + sin(seconds * .5f + ember.phase) * .01f
        val flicker = .7f + .3f * sin(seconds * 6f + ember.phase * 3f)
        val fade = minOf(1f, travelled * 8f, (1f - travelled) * 8f) * presence * (.55f + ember.radius * .15f) * flicker
        val center = Offset(x * size.width, y * size.height)
        val radius = ember.radius * dp
        drawLine(StovaColors.Ember.copy(alpha = fade * .5f), center, center + Offset(radius, -radius * 2f), dp, StrokeCap.Round)
        drawCircle(StovaColors.Ember.copy(alpha = fade * .8f), radius * .55f, center)
    }
    if (!front) return
    if (presence > 0f) bats.forEach { bat ->
        val x = ((bat.start + seconds * bat.speed) % 1.3f - .15f) * size.width
        val y = (bat.y + sin(seconds * .7f + bat.phase) * .02f) * size.height
        bat(Offset(x, y), bat.size * dp * 1.3f, sin(seconds * 7f + bat.phase), StovaColors.Spider.copy(alpha = .6f * presence))
    }
    if (presence > 0f) visit(seconds, 31f, 9f, offset = 8f)?.let { t ->
        val column = ((seconds / 31f).toInt() * .37f + .2f) % .8f + .1f
        ghost(Offset(size.width * column, size.height * (.92f - t * .5f)), t, seconds)
    }
    mist(seconds, presence)
    lantern(Offset(30 * dp, size.height - 18 * dp), 15 * dp, seconds)
    lantern(Offset(size.width - 32 * dp, size.height - 16 * dp), 12 * dp, seconds + 2f)
    if (drop > 0f) {
        // Outside the safe edge: hangs from the top bar without covering a control or label.
        val x = size.width - 22 * dp
        val y = 26 * dp + drop * 105 * dp
        val body = Offset(x, y)
        val tintSpider = StovaColors.Spider.copy(alpha = .7f)
        drawLine(tintSpider.copy(alpha = .35f), Offset(x, 0f), body, dp)
        drawOval(tintSpider, body - Offset(4 * dp, 6 * dp), Size(8 * dp, 12 * dp))
        repeat(4) { leg ->
            for (side in listOf(-1f, 1f)) {
                val from = body + Offset(side * 3 * dp, (leg - 1.5f) * 2 * dp)
                val bend = from + Offset(side * 6 * dp, (leg - 1.5f) * 3 * dp)
                drawLine(tintSpider, from, bend, dp)
                drawLine(tintSpider, bend, bend + Offset(side * 2 * dp, 4 * dp), dp)
            }
        }
    }
}

/** A bat: body, two ears and wings with a scalloped trailing edge; [flap] -1..1 lifts and drops the tips. */
private fun DrawScope.bat(center: Offset, span: Float, flap: Float, tint: Color) {
    val tip = span * .42f * flap
    val wings = Path().apply {
        moveTo(center.x, center.y - span * .08f)
        for (side in listOf(-1f, 1f)) {
            fun at(dx: Float, dy: Float) = Offset(center.x + side * dx * span, center.y + dy * span)
            val start = at(0f, -.08f); if (side > 0f) lineTo(start.x, start.y)
            val edge = at(.5f, -.3f); val end = at(1f, 0f)
            quadraticTo(edge.x, edge.y - tip * .5f, end.x, end.y - tip)
            // Three scallops back to the body.
            for ((reach, dip) in listOf(.74f to .16f, .5f to .2f, .26f to .18f)) {
                val point = at(reach, dip); val control = at(reach + .1f, .02f)
                quadraticTo(control.x, control.y - tip * reach, point.x, point.y - tip * reach * .4f)
            }
            val back = at(0f, .2f); lineTo(back.x, back.y)
            if (side < 0f) moveTo(center.x, center.y - span * .08f)
        }
    }
    drawPath(wings, tint)
    drawOval(tint, center - Offset(span * .1f, span * .2f), Size(span * .2f, span * .42f))
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(center.x + side * span * .02f, center.y - span * .16f)
            lineTo(center.x + side * span * .1f, center.y - span * .32f)
            lineTo(center.x + side * span * .1f, center.y - span * .14f); close()
        }
        drawPath(ear, tint)
    }
}

/** A corner web: threads fanning out from [corner] with sagging rings between them. */
internal fun DrawScope.cobweb(corner: Offset, xSign: Float, ySign: Float, reach: Float = 74 * density, alpha: Float = .32f) {
    val tint = StovaColors.Spider.copy(alpha = alpha)
    val ends = List(6) { i ->
        val a = (i / 5f) * PI.toFloat() / 2f
        Offset(cos(a) * xSign, sin(a) * ySign)
    }
    ends.forEach { drawLine(tint, corner, corner + it * reach, density * .8f) }
    for (ring in 1..4) {
        val r = reach * ring / 4.6f
        val web = Path()
        ends.forEachIndexed { i, dir ->
            val point = corner + dir * r
            if (i == 0) web.moveTo(point.x, point.y)
            else {
                val mid = corner + (dir + ends[i - 1]) * (r * .42f)
                web.quadraticTo(mid.x, mid.y, point.x, point.y)
            }
        }
        drawPath(web, tint, style = Stroke(density * .7f))
    }
}

/** Low mist drifting along the bottom edge in soft lobes. */
private fun DrawScope.mist(seconds: Float, presence: Float) {
    if (presence <= 0f) return
    repeat(6) { i ->
        val x = ((i / 6f + seconds * (.006f + i * .002f)) % 1.3f - .15f) * size.width
        val center = Offset(x, size.height - 22 * density)
        val radius = (90f + i * 18f) * density
        drawCircle(Brush.radialGradient(listOf(StovaColors.Mist.copy(alpha = .16f * presence), Color.Transparent), center, radius),
            radius, center)
    }
}

/** A carved pumpkin with a flickering candle inside. */
private fun DrawScope.lantern(center: Offset, radius: Float, seconds: Float) {
    val glow = .7f + .3f * sin(seconds * 9f) * sin(seconds * 3.3f)
    drawCircle(StovaColors.Ember.copy(alpha = .07f * glow), radius * 1.8f, center)
    // A deeper orange shell, so the candlelit face reads at a glance.
    drawOval(androidx.compose.ui.graphics.lerp(StovaColors.Ember, StovaColors.Ink, .35f), center - Offset(radius * 1.15f, radius), Size(radius * 2.3f, radius * 2f))
    drawLine(StovaColors.Fir, center - Offset(0f, radius), center - Offset(-radius * .15f, radius * 1.4f), density * 2f, StrokeCap.Round)
    val face = StovaColors.Bulbs[2].copy(alpha = glow)
    for (side in listOf(-1f, 1f)) {
        val eye = Path().apply {
            moveTo(center.x + side * radius * .45f, center.y - radius * .45f)
            lineTo(center.x + side * radius * .2f, center.y - radius * .05f)
            lineTo(center.x + side * radius * .7f, center.y - radius * .05f); close()
        }
        drawPath(eye, face)
    }
    val mouth = Path().apply {
        moveTo(center.x - radius * .6f, center.y + radius * .2f)
        lineTo(center.x - radius * .2f, center.y + radius * .4f); lineTo(center.x, center.y + radius * .25f)
        lineTo(center.x + radius * .2f, center.y + radius * .4f); lineTo(center.x + radius * .6f, center.y + radius * .2f)
        lineTo(center.x + radius * .3f, center.y + radius * .6f); lineTo(center.x - radius * .3f, center.y + radius * .6f); close()
    }
    drawPath(mouth, face)
}

/** A small ghost that rises from the bottom, sways and fades away. */
private fun DrawScope.ghost(at: Offset, t: Float, seconds: Float) {
    val dp = density
    val alpha = .4f * minOf(1f, t * 5f, (1f - t) * 3f)
    val x = at.x + sin(seconds * 1.5f) * 10 * dp
    val w = 13 * dp
    val body = Path().apply {
        moveTo(x - w, at.y + 14 * dp)
        lineTo(x - w, at.y)
        arcTo(androidx.compose.ui.geometry.Rect(x - w, at.y - w, x + w, at.y + w), 180f, 180f, false)
        lineTo(x + w, at.y + 14 * dp)
        repeat(4) { i ->
            val edge = x + w - (i + 1) * w / 2f
            lineTo(edge + w / 4f, at.y + (if (i % 2 == 0) 9f else 15f) * dp); lineTo(edge, at.y + 14 * dp)
        }
        close()
    }
    drawPath(body, StovaColors.Snow.copy(alpha = alpha))
    for (side in listOf(-1f, 1f)) drawCircle(StovaColors.Ink.copy(alpha = alpha * 1.6f), 2 * dp, Offset(x + side * 4.5f * dp, at.y - 2 * dp))
}

/** A witch on her broom crossing high, left to right, as a moonlit silhouette. */
private fun DrawScope.witch(t: Float, seconds: Float) {
    val dp = density
    val x = size.width * (-.12f + t * 1.3f)
    val y = size.height * .2f + sin(seconds * 1.2f) * 6 * dp
    val tint = StovaColors.Spider.copy(alpha = .5f * minOf(1f, t * 8f, (1f - t) * 8f))
    drawLine(tint, Offset(x - 26 * dp, y + 4 * dp), Offset(x + 22 * dp, y - 2 * dp), dp * 1.6f, StrokeCap.Round)
    repeat(5) { i -> drawLine(tint, Offset(x - 26 * dp, y + 4 * dp), Offset(x - 36 * dp, y + (i - 1f) * 3 * dp), dp) }
    val figure = Path().apply {
        moveTo(x - 6 * dp, y + 2 * dp); lineTo(x + 6 * dp, y + 1 * dp); lineTo(x + 2 * dp, y - 12 * dp); close()
    }
    drawPath(figure, tint)
    drawCircle(tint, 3.2f * dp, Offset(x + 2 * dp, y - 14 * dp))
    val hat = Path().apply {
        moveTo(x - 4 * dp, y - 16 * dp); lineTo(x + 8 * dp, y - 17 * dp); lineTo(x - 2 * dp, y - 29 * dp); close()
    }
    drawPath(hat, tint)
}
