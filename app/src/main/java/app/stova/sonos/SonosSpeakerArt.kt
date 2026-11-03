package app.stova.sonos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.stova.core.theme.SonosFinish
import app.stova.core.theme.StovaMotion
import app.stova.core.theme.stovaMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Whether a drawn speaker is breathing right now; read by tests, not shown. */
val SonosSpeakerActive = SemanticsPropertyKey<Boolean>("SonosSpeakerActive")

@Composable internal fun rememberSonosVisual(room: SonosRoom, state: SonosState): SonosRoomVisual {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    return sonosRoomVisual(room, state, lifecycle.isAtLeast(Lifecycle.State.RESUMED), stovaMotion.still)
}

/**
 * A front view of the actual Sonos form, seen slightly from above. Geometry is built once per size;
 * while music plays only the status light breathes and, with [waves], the outline softly echoes
 * outwards at a fixed pace. It shows that music plays, never how loud: Stova has no audio levels.
 */
@Composable internal fun SonosSpeakerArt(speaker: SonosSpeaker, visual: SonosRoomVisual?, modifier: Modifier, tag: String,
    available: Boolean = true, waves: Boolean = false) {
    // A speaker scrolled out of the page, or clipped away by the window, does not tick. Visibility
    // is judged from the clipped layout bounds when layout moves (scrolling), never per animation frame.
    var visible by remember { mutableStateOf(true) }
    val active = visual?.active == true && visible
    val playing = visual?.playing == true
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        phase.floatValue = 0f
        if (!active) return@LaunchedEffect
        var start = -1L
        while (true) {
            withFrameMillis { now ->
                if (start < 0) start = now
                phase.floatValue = ((now - start) % StovaMotion.SpeakerBreathMillis) / StovaMotion.SpeakerBreathMillis.toFloat()
            }
            delay(StovaMotion.SpeakerFrameMillis)
        }
    }
    Spacer(modifier.testTag(tag).semantics { this[SonosSpeakerActive] = active }
        .onVisibilityChanged(minFractionVisible = .01f) { visible = it }.drawWithCache {
        val shape = speakerGeometry(speaker.shape, size, waves)
        // Only the draw phase reads the clock: no recomposition or layout per frame.
        onDrawBehind { drawSpeaker(shape, if (active) phase.floatValue else null, playing, available) }
    })
}

/** Low, wide speakers get a low stage, so their name sits right under them. */
internal fun sonosStageHeight(shape: SonosShape) = when (shape) {
    SonosShape.BEAM, SonosShape.ARC, SonosShape.RAY, SonosShape.PLAYBAR, SonosShape.PLAYBASE -> 40.dp
    SonosShape.COMPONENT -> 56.dp
    else -> 76.dp
}

private class SpeakerGeometry(
    val front: Path, val top: Path, val body: Rect, val grille: Path?, val dots: List<Offset>, val dot: Float,
    val slats: List<Pair<Offset, Offset>>, val opening: Path?, val ridge: Pair<Offset, Offset>?,
    val light: Offset, val lightRadius: Float, val floor: Rect, val reach: Float, val generic: Boolean,
)

private class Form(val aspect: Float, val radius: Float, val topDepth: Float, val grilleInset: Float, val grilleTop: Float,
    val texture: Texture = Texture.DOTS, val taper: Float = 0f, val pinch: Float = 0f)
private enum class Texture { DOTS, SLATS, NONE }

/** Proportions from the products' published front dimensions; very long bars are shortened so they still read at TV size. */
private fun form(shape: SonosShape) = when (shape) {
    SonosShape.ONE -> Form(.745f, .17f, .07f, .06f, .11f)
    SonosShape.ERA_100 -> Form(.66f, .42f, .06f, .07f, .1f)
    SonosShape.ERA_300 -> Form(1.62f, .2f, .07f, .06f, .09f, pinch = .085f)
    SonosShape.FIVE -> Form(1.79f, .3f, .07f, .05f, .07f)
    SonosShape.BEAM -> Form(6.4f, .46f, .14f, .1f, .12f)
    SonosShape.ARC -> Form(8f, .5f, .16f, .1f, .1f)
    SonosShape.RAY -> Form(7f, .2f, .14f, .1f, .34f, Texture.SLATS)
    SonosShape.PLAYBAR -> Form(7.4f, .3f, .14f, .1f, .14f)
    SonosShape.PLAYBASE -> Form(9f, .2f, .22f, .14f, .18f)
    SonosShape.MOVE -> Form(.667f, .3f, .06f, .07f, .1f, taper = .07f)
    SonosShape.ROAM -> Form(.37f, .5f, .035f, .14f, .1f)
    // Sub: 402 × 389 mm face with the acoustic slot through it.
    SonosShape.SUB -> Form(1.03f, .14f, .05f, 0f, 0f, Texture.NONE)
    // Sub Mini: an upright cylinder, Ø230 × 305 mm, flat top, a tall centre tunnel.
    SonosShape.SUB_MINI -> Form(.754f, .08f, .11f, 0f, 0f, Texture.NONE)
    SonosShape.COMPONENT -> Form(3.4f, .16f, .3f, 0f, 0f, Texture.NONE)
    // No real model: a soft, generic shape drawn faintly, so it never passes for a One.
    SonosShape.UNKNOWN -> Form(.8f, .3f, .05f, .1f, .14f)
}

private fun speakerGeometry(shape: SonosShape, size: Size, waves: Boolean): SpeakerGeometry {
    val f = form(shape)
    val margin = if (waves) .13f else .03f
    val floorGap = size.height * .07f
    val w = size.width * (1 - 2 * margin)
    val h = size.height * (1 - 2 * margin) - floorGap
    val bodyH = min(h, w / f.aspect)
    val bodyW = bodyH * f.aspect
    val left = (size.width - bodyW) / 2
    val top = (size.height - bodyH - floorGap) / 2
    val body = Rect(left, top, left + bodyW, top + bodyH)
    val depth = bodyH * f.topDepth
    val frontRect = Rect(body.left, body.top + depth, body.right, body.bottom)
    val radius = min(frontRect.width, frontRect.height) * f.radius
    val front = outline(frontRect, radius, f)
    val inset = bodyW * .015f
    // The same silhouette a little higher and narrower: the top surface seen from slightly above.
    val topFace = outline(Rect(body.left + inset, body.top, body.right - inset, body.bottom - depth), radius, f)
    val short = min(frontRect.width, frontRect.height)
    val grilleRect = Rect(frontRect.left + short * f.grilleInset, frontRect.top + frontRect.height * f.grilleTop,
        frontRect.right - short * f.grilleInset, frontRect.bottom - short * f.grilleInset)
    val grille = if (f.texture == Texture.NONE) null else outline(grilleRect, max(0f, radius - short * f.grilleInset), f)
    val pitch = max(2.6f, short / 22f)
    val dots = if (f.texture != Texture.DOTS) emptyList() else buildList {
        var y = grilleRect.top + pitch / 2
        var row = 0
        while (y < grilleRect.bottom) {
            var x = grilleRect.left + pitch / 2 + if (row % 2 == 1) pitch / 2 else 0f
            while (x < grilleRect.right) { add(Offset(x, y)); x += pitch }
            y += pitch * .87f; row++
        }
    }
    val slats = if (f.texture != Texture.SLATS) emptyList() else buildList {
        var y = grilleRect.top + pitch / 2
        while (y < grilleRect.bottom) { add(Offset(grilleRect.left, y) to Offset(grilleRect.right, y)); y += pitch * 1.1f }
    }
    val opening = when (shape) {
        SonosShape.SUB -> slot(frontRect, .66f, .15f)
        SonosShape.SUB_MINI -> slot(frontRect, .19f, .5f, frontRect.height * .06f)
        else -> null
    }
    val ridge = if (shape == SonosShape.ROAM) Offset(frontRect.center.x - frontRect.width * .12f, frontRect.top + radius * .6f) to
        Offset(frontRect.center.x - frontRect.width * .12f, frontRect.bottom - radius * .6f) else null
    val light = when (shape) {
        SonosShape.COMPONENT -> Offset(frontRect.left + frontRect.width * .1f, frontRect.center.y)
        SonosShape.SUB -> Offset(frontRect.center.x, frontRect.bottom - frontRect.height * .1f)
        SonosShape.SUB_MINI -> Offset(frontRect.right - frontRect.width * .14f, frontRect.bottom - frontRect.height * .14f)
        else -> Offset(frontRect.center.x, (frontRect.top + grilleRect.top) / 2)
    }
    val floor = Rect(body.left - bodyW * .05f, body.bottom - floorGap * .5f, body.right + bodyW * .05f, body.bottom + floorGap)
    return SpeakerGeometry(front, topFace, body, grille, dots, pitch * .44f, slats, opening, ridge, light,
        max(1.4f, min(short * .025f, 4.5f)), floor, if (waves) min(size.width, size.height) * margin * .9f else 0f,
        shape == SonosShape.UNKNOWN)
}

/** Rounded front: straight sides, a Move's gentle taper, or the Era 300's waist. */
private fun outline(r: Rect, radius: Float, f: Form): Path {
    if (f.taper == 0f && f.pinch == 0f) return Path().apply { addRoundRect(RoundRect(r, CornerRadius(radius))) }
    val t = r.width * f.taper
    val p = r.width * f.pinch
    val rr = min(radius, r.height / 2)
    return Path().apply {
        moveTo(r.left + t + rr, r.top)
        lineTo(r.right - t - rr, r.top)
        quadraticTo(r.right - t, r.top, r.right - t + t * rr / r.height, r.top + rr)
        quadraticTo(r.right - t / 2 - p * 2, r.center.y, r.right - t * rr / r.height, r.bottom - rr)
        quadraticTo(r.right, r.bottom, r.right - rr, r.bottom)
        lineTo(r.left + rr, r.bottom)
        quadraticTo(r.left, r.bottom, r.left + t * rr / r.height, r.bottom - rr)
        quadraticTo(r.left + t / 2 + p * 2, r.center.y, r.left + t - t * rr / r.height, r.top + rr)
        quadraticTo(r.left + t, r.top, r.left + t + rr, r.top)
        close()
    }
}

private fun slot(r: Rect, width: Float, height: Float, drop: Float = 0f): Path {
    val h = r.height * height
    val w = r.width * width
    val y = r.center.y + drop
    val slot = Rect(r.center.x - w / 2, y - h / 2, r.center.x + w / 2, y + h / 2)
    return Path().apply { addRoundRect(RoundRect(slot, CornerRadius(min(w, h) / 2))) }
}

private fun DrawScope.drawSpeaker(g: SpeakerGeometry, phase: Float?, playing: Boolean, available: Boolean) {
    val fade = (if (available) 1f else .45f) * (if (g.generic) .6f else 1f)
    // A soft contact shadow grounds the speaker on the page.
    drawOval(Brush.radialGradient(listOf(SonosFinish.Floor.copy(alpha = .7f * fade), SonosFinish.Floor.copy(alpha = 0f)),
        g.floor.center, g.floor.width / 2), g.floor.topLeft, g.floor.size)
    if (phase != null && g.reach > 0f) for (offset in floatArrayOf(0f, .5f)) {
        val p = (phase + offset) % 1f
        val sx = 1 + 2 * g.reach * p / g.body.width
        val sy = 1 + 2 * g.reach * p / g.body.height
        withTransform({ scale(sx, sy, g.body.center) }) {
            drawPath(g.front, SonosFinish.Light.copy(alpha = .16f * (1 - p) * min(1f, p * 4)), style = Stroke(1.2f))
        }
    }
    drawPath(g.top, Brush.verticalGradient(listOf(SonosFinish.ShellTop, SonosFinish.Shell), g.body.top, g.body.top + g.body.height * .25f), alpha = fade)
    drawPath(g.front, Brush.horizontalGradient(0f to SonosFinish.ShellShade, .22f to SonosFinish.Shell, .6f to SonosFinish.Shell,
        1f to SonosFinish.ShellShade, startX = g.body.left, endX = g.body.right), alpha = fade)
    g.grille?.let { grille ->
        drawPath(grille, SonosFinish.Grille, alpha = fade)
        clipPath(grille) {
            if (g.dots.isNotEmpty()) drawPoints(g.dots, PointMode.Points, SonosFinish.GrilleDot.copy(alpha = fade), g.dot, StrokeCap.Round)
            g.slats.forEach { (a, b) -> drawLine(SonosFinish.GrilleDot.copy(alpha = fade), a, b, g.dot) }
        }
    }
    g.opening?.let { opening ->
        drawPath(opening, SonosFinish.Opening, alpha = fade)
        clipPath(opening) { drawRect(Brush.verticalGradient(listOf(SonosFinish.ShellShade, SonosFinish.Opening), g.body.top, g.body.center.y)) }
    }
    g.ridge?.let { (a, b) -> drawLine(SonosFinish.Rim.copy(alpha = .5f * fade), a, b, 1f) }
    // A fine rim catches the light along the top edge and gives the body a crisp edge on dark pages.
    drawPath(g.front, Brush.verticalGradient(listOf(SonosFinish.Rim, SonosFinish.Rim.copy(alpha = 0f)), g.body.top, g.body.center.y),
        alpha = fade, style = Stroke(1f))
    if (!available) return
    val breath = phase?.let { .55f + .45f * (.5f - .5f * cos(2 * PI.toFloat() * it)) } ?: 1f
    val light = if (playing) SonosFinish.Light.copy(alpha = breath) else SonosFinish.LightIdle
    if (playing) drawCircle(SonosFinish.Light.copy(alpha = .18f * breath), g.lightRadius * 2.6f, g.light)
    drawCircle(light, g.lightRadius, g.light)
}
