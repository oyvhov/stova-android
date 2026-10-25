package app.stova.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

/** Activity-level reporting sees keys even inside a dialog and never consumes them. */
object SeasonalInput {
    val generation = MutableStateFlow(0L)
    fun report() { generation.value += 1 }
}

internal fun seasonalMotionEnabled(season: Season, decor: SeasonalDecor, reduced: Boolean, started: Boolean) =
    season != Season.NONE && decor == SeasonalDecor.FULL && !reduced && started

/** A draw-phase clock: at most one update per 50 ms, no frame work while hidden or reduced. */
@Composable
internal fun rememberSeasonalTime(): State<Float> {
    val p = LocalAppearance.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val enabled = seasonalMotionEnabled(p.season(), p.seasonalDecor, LocalReduceMotion.current, lifecycle.isAtLeast(Lifecycle.State.STARTED))
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled, p.season()) {
        if (!enabled) return@LaunchedEffect
        var previous = 0L
        while (true) {
            withFrameMillis { frame ->
                if (previous != 0L) time.floatValue += ((frame - previous) / 1000f).coerceAtMost(.1f)
                previous = frame
            }
            delay(50)
        }
    }
    return time
}

internal data class SeasonParticle(val x: Float, val start: Float, val radius: Float, val speed: Float, val phase: Float)
internal fun seasonParticles(season: Season): List<SeasonParticle> {
    val random = Random(season.ordinal * 7919)
    return List(if (season == Season.CHRISTMAS) 18 else 12) {
        SeasonParticle(.64f + random.nextFloat() * .34f, random.nextFloat(),
            1.1f + random.nextFloat() * 2f, .025f + random.nextFloat() * .025f, random.nextFloat() * 2f * PI.toFloat())
    }
}
internal fun spiderDrop(idleSeconds: Float) = ((idleSeconds - 45f) / 5f).coerceIn(0f, 1f)

/** One small Canvas, no input or accessibility nodes. Particles stay over the right-hand artwork. */
@Composable
fun SeasonalScene(modifier: Modifier = Modifier) {
    val p = LocalAppearance.current
    val season = p.season()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!seasonalMotionEnabled(season, p.seasonalDecor, LocalReduceMotion.current, lifecycle.isAtLeast(Lifecycle.State.STARTED))) return
    val time = rememberSeasonalTime()
    val particles = remember(season) { seasonParticles(season) }
    val input by SeasonalInput.generation.collectAsState()
    // Reset the idle origin on each key and on entering Home. No wall-clock jump after resume.
    val idleStart = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(input) { idleStart.floatValue = time.value }
    Canvas(modifier.clearAndSetSemantics { }) {
        val seconds = time.value
        val rising = season == Season.HALLOWEEN
        val tint = if (rising) StovaColors.Ember else StovaColors.Snow
        particles.forEach { particle ->
            val travelled = (particle.start + seconds * particle.speed) % 1f
            val y = .24f + (if (rising) 1f - travelled else travelled) * .34f
            val x = particle.x + sin(seconds * .8f + particle.phase) * .006f
            val fade = (1f - abs(travelled - .5f) * 2f).coerceIn(0f,1f)
            val center = Offset(x * size.width, y * size.height)
            val radius = particle.radius * density
            if (rising) drawLine(tint.copy(alpha = fade * .42f), center, center + Offset(radius, -radius * 2f), density, StrokeCap.Round)
            else repeat(3) { arm ->
                val angle = arm * PI.toFloat() / 3f
                val offset = Offset(cos(angle), sin(angle)) * radius * 1.7f
                drawLine(tint.copy(alpha = fade * .48f), center - offset, center + offset, density, StrokeCap.Round)
            }
        }
        val drop = if (rising) spiderDrop(seconds - idleStart.floatValue) else 0f
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
