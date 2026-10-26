package app.stova.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
internal class SeasonalClock {
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
internal fun rememberSeasonalClock(): SeasonalClock {
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

internal data class SeasonParticle(val x: Float, val start: Float, val radius: Float, val speed: Float, val phase: Float)
/** Spread over the whole screen. Larger flakes fall faster, so the snow has depth without blur. */
internal fun seasonParticles(season: Season): List<SeasonParticle> {
    val random = Random(season.ordinal * 7919)
    return List(if (season == Season.CHRISTMAS) 56 else 34) {
        val radius = .8f + random.nextFloat() * 2f
        SeasonParticle(random.nextFloat(), random.nextFloat(), radius, .012f + radius * .009f, random.nextFloat() * 2f * PI.toFloat())
    }
}

/** A few bats crossing the upper screen on long, slow loops at Halloween. */
internal data class SeasonBat(val start: Float, val y: Float, val size: Float, val speed: Float, val phase: Float)
internal fun seasonBats(season: Season): List<SeasonBat> {
    if (season != Season.HALLOWEEN) return emptyList()
    val random = Random(31)
    return List(3) { SeasonBat(random.nextFloat(), .1f + random.nextFloat() * .3f, 9f + random.nextFloat() * 6f,
        .018f + random.nextFloat() * .01f, random.nextFloat() * 2f * PI.toFloat()) }
}
internal fun spiderDrop(idleSeconds: Float) = ((idleSeconds - 45f) / 5f).coerceIn(0f, 1f)

/** One Canvas behind Home's content, no input or accessibility nodes; the decor covers the whole screen. */
@Composable
fun SeasonalScene(modifier: Modifier = Modifier) {
    val p = LocalAppearance.current
    val season = p.season()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!seasonalMotionEnabled(season, p.seasonalDecor, LocalReduceMotion.current, lifecycle.isAtLeast(Lifecycle.State.STARTED))) return
    // The idle origin resets on each key and on entering Home. No wall-clock jump after resume.
    val clock = rememberSeasonalClock()
    val particles = remember(season) { seasonParticles(season) }
    val bats = remember(season) { seasonBats(season) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val seconds = clock.time.floatValue
        val presence = clock.presence
        val rising = season == Season.HALLOWEEN
        val tint = if (rising) StovaColors.Ember else StovaColors.Snow
        if (presence > 0f) particles.forEach { particle ->
            val travelled = (particle.start + seconds * particle.speed) % 1f
            val y = -.04f + (if (rising) 1f - travelled else travelled) * 1.08f
            val x = particle.x + sin(seconds * .5f + particle.phase) * .01f
            // Only the screen edges fade; nearer (larger) flakes are a little brighter.
            val fade = minOf(1f, travelled * 8f, (1f - travelled) * 8f) * presence * (.55f + particle.radius * .15f)
            val center = Offset(x * size.width, y * size.height)
            val radius = particle.radius * density
            if (rising) drawLine(tint.copy(alpha = fade * .5f), center, center + Offset(radius, -radius * 2f), density, StrokeCap.Round)
            else if (particle.radius < 2.2f) drawCircle(tint.copy(alpha = fade * .5f), radius * .8f, center)
            else repeat(3) { arm ->
                val angle = arm * PI.toFloat() / 3f + particle.phase
                val offset = Offset(cos(angle), sin(angle)) * radius * 1.6f
                drawLine(tint.copy(alpha = fade * .55f), center - offset, center + offset, density, StrokeCap.Round)
            }
        }
        if (presence > 0f) bats.forEach { bat ->
            val x = ((bat.start + seconds * bat.speed) % 1.3f - .15f) * size.width
            val y = (bat.y + sin(seconds * .7f + bat.phase) * .02f) * size.height
            val span = bat.size * density
            val flap = sin(seconds * 7f + bat.phase) * span * .45f
            val wings = Path().apply {
                moveTo(x - span, y - flap)
                quadraticTo(x - span * .5f, y + span * .15f, x, y)
                quadraticTo(x + span * .5f, y + span * .15f, x + span, y - flap)
                quadraticTo(x + span * .45f, y + span * .45f, x, y + span * .3f)
                quadraticTo(x - span * .45f, y + span * .45f, x - span, y - flap)
                close()
            }
            drawPath(wings, StovaColors.Spider.copy(alpha = .45f * presence))
        }
        val drop = if (rising) clock.spider else 0f
        if (drop > 0f) {
            // Outside the safe edge: hangs from the top bar without covering a control or label.
            val x = size.width - 22 * density
            val y = 26 * density + drop * 105 * density
            val body = Offset(x,y)
            val tintSpider = StovaColors.Spider.copy(alpha=.7f)
            drawLine(tintSpider.copy(alpha=.35f),Offset(x,0f),body,density)
            drawOval(tintSpider,body-Offset(4*density,6*density),androidx.compose.ui.geometry.Size(8*density,12*density))
            repeat(4) { leg ->
                for (side in listOf(-1f,1f)) {
                    val from=body+Offset(side*3*density,(leg-1.5f)*2*density)
                    val bend=from+Offset(side*6*density,(leg-1.5f)*3*density)
                    drawLine(tintSpider,from,bend,density)
                    drawLine(tintSpider,bend,bend+Offset(side*2*density,4*density),density)
                }
            }
        }
    }
}
