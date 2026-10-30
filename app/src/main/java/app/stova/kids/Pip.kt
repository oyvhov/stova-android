package app.stova.kids

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import app.stova.core.theme.KidsColors as K
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** How Pip feels, which his eyes, lids, light mouth and arms show. */
enum class PipMood { HAPPY, CURIOUS, EXCITED, WAVE, SLEEPY, ASLEEP }

/** What Pip wears: one thing for each season, and a nightcap at bedtime. */
enum class PipOutfit { NONE, SANTA, WITCH, BEANIE, BUNNY, FLAG, FLOWER, SUNGLASSES, SCARF, NIGHTCAP }

fun KidsSeason.outfit(): PipOutfit = when (this) {
    KidsSeason.CHRISTMAS -> PipOutfit.SANTA
    KidsSeason.HALLOWEEN -> PipOutfit.WITCH
    KidsSeason.WINTER -> PipOutfit.BEANIE
    KidsSeason.EASTER -> PipOutfit.BUNNY
    KidsSeason.MAY_17 -> PipOutfit.FLAG
    KidsSeason.SPRING -> PipOutfit.FLOWER
    KidsSeason.SUMMER -> PipOutfit.SUNGLASSES
    KidsSeason.AUTUMN -> PipOutfit.SCARF
}

/** Pip's drawing is 200 × 300 units: room for a tall hat above him and his shadow under his feet. */
const val PIP_ASPECT = 200f / 300f
private const val GROUND = 286f
/** The line his feet stand on, as a share of his drawing's height. */
const val PIP_FEET = GROUND / 300f
/** His head is drawn in its own units and sits this far down in the drawing. */
private const val HEAD_Y = 34f

/**
 * Pip, the little robot on the children's Home: brushed steel and worn paint, lens eyes with metal
 * lids, a light for a mouth and two short legs. He is drawn, not an image, so he stays sharp on
 * any TV, and his whole pose comes from [motion], which the screen steps once a frame.
 */
@Composable
fun Pip(mood: PipMood, outfit: PipOutfit, motion: PipMotion, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.width / 200f
        // A cheer shows on his face whatever he felt a moment before.
        val face = if (motion.cheer > 0.05f && mood != PipMood.ASLEEP) PipMood.EXCITED else mood
        withTransform({ scale(unit, unit, Offset.Zero) }) { drawPip(face, outfit, motion) }
    }
}

private fun DrawScope.drawPip(mood: PipMood, outfit: PipOutfit, m: PipMotion) {
    val sitDrop = 30f * m.sit
    val walking = min(m.stride, 1.2f)
    val bob = -abs(sin(m.gait * PI).toFloat()) * 4f * walking + sitDrop + m.breath * 1.2f - m.air * 4f
    // A shadow that stays on the card and shrinks while he is up in the air.
    val spread = 1f - 0.45f * m.air
    drawOval(K.Shadow.copy(alpha = 0.26f * (1f - 0.5f * m.air)), Offset(100f - 44f * spread, GROUND - 6f), Size(88f * spread, 11f))
    rotate(m.lean, Offset(100f, GROUND)) {
        legs(m, bob, sitDrop)
        translate(0f, bob) {
            // Squash and stretch about his hips: wider as he squashes, taller as he stretches.
            withTransform({ scale(2f - m.squash, m.squash, Offset(100f, 236f)) }) {
                neck()
                body(m.glow)
                arms(m, walking)
                if (outfit == PipOutfit.SCARF) translate(0f, HEAD_Y) { scarf() }
                withTransform({ translate(0f, HEAD_Y + m.headDy); rotate(m.headTilt, Offset(100f, 138f)) }) {
                    head(mood, outfit, m)
                }
            }
        }
    }
}

// Legs and feet: planted on the ground while the body above them bobs.

private fun DrawScope.legs(m: PipMotion, bob: Float, sitDrop: Float) {
    val walking = min(m.stride, 1.2f)
    for (side in listOf(-1f, 1f)) {
        val step = sin(m.gait * PI).toFloat()
        var lift = max(0f, if (side < 0) step else -step) * 13f * walking
        if (side > 0) lift += 9f * m.tap
        lift += 16f * m.air
        val hip = Offset(100f + side * 16f, 232f + bob)
        val footX = 100f + side * (22f + 10f * m.sit - 4f * m.air)
        val foot = Offset(footX, GROUND - 7f - lift + sitDrop * 0.15f)
        val bend = 4f + 7f * m.air + lift * 0.5f + 6f * m.sit
        val knee = Offset((hip.x + foot.x) / 2f + side * bend, (hip.y + foot.y) / 2f - 2f)
        limb(hip, knee, 11f); limb(knee, foot + Offset(0f, -6f), 10f)
        joint(knee, 6.5f)
        shoe(foot, side, m.sit)
    }
}

private fun DrawScope.limb(from: Offset, to: Offset, width: Float) {
    drawLine(K.SteelDark, from, to, width + 2f, StrokeCap.Round)
    drawLine(K.SteelMid, from, to, width, StrokeCap.Round)
    drawLine(K.SteelLight.copy(alpha = 0.7f), from + Offset(-width * 0.22f, 0f), to + Offset(-width * 0.22f, 0f), width * 0.25f, StrokeCap.Round)
}

private fun DrawScope.joint(at: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(K.SteelLight, K.SteelMid, K.SteelDark), at + Offset(-r * 0.35f, -r * 0.35f), r * 1.4f), r, at)
}

/** A rubber-soled shoe with a painted cap; seen from the front when he sits, soles out. */
private fun DrawScope.shoe(at: Offset, side: Float, sit: Float) {
    val w = 38f - 8f * sit; val h = 15f + 10f * sit
    val topLeft = Offset(at.x - w / 2 + side * 3f, at.y - h / 2)
    drawRoundRect(K.Rubber, topLeft + Offset(0f, 3f), Size(w, h), CornerRadius(h / 2))
    drawRoundRect(Brush.verticalGradient(listOf(K.PaintLight, K.Paint, K.PaintDark), topLeft.y, topLeft.y + h), topLeft, Size(w, h - 3f), CornerRadius(h / 2))
    drawOval(K.SteelLight.copy(alpha = 0.45f), topLeft + Offset(w * 0.18f, 2f), Size(w * 0.35f, 3.5f))
    if (sit > 0.4f) for (i in 0..2) drawLine(K.RubberLight, Offset(topLeft.x + 8f, topLeft.y + 7f + i * 4f), Offset(topLeft.x + w - 8f, topLeft.y + 7f + i * 4f), 1.4f)
}

// The body, with the neck that carries his head.

private fun DrawScope.neck() {
    for (i in 0..3) drawRoundRect(if (i % 2 == 0) K.Rubber else K.RubberLight, Offset(86f + i, 166f + i * 3.6f), Size(28f - 2f * i, 4.2f), CornerRadius(2f))
}

private fun DrawScope.body(glow: Float) {
    val torso = RoundRect(60f, 178f, 140f, 238f, CornerRadius(26f))
    val path = Path().apply { addRoundRect(torso) }
    drawPath(path, Brush.horizontalGradient(0f to K.PaintDark, 0.22f to K.Paint, 0.40f to K.PaintLight, 0.68f to K.Paint, 1f to K.PaintDark, startX = 60f, endX = 140f))
    clipPath(path) {
        drawRect(Brush.verticalGradient(listOf(K.Shadow.copy(alpha = 0.38f), Color.Transparent), 178f, 194f), Offset(60f, 178f), Size(80f, 16f))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, K.Shadow.copy(alpha = 0.28f)), 212f, 238f), Offset(60f, 212f), Size(80f, 26f))
        // Where the paint has worn through to the steel.
        for ((x, y, w, h) in listOf(floatArrayOf(61f, 204f, 5f, 3f), floatArrayOf(135f, 192f, 4f, 6f), floatArrayOf(64f, 228f, 6f, 3f),
            floatArrayOf(130f, 231f, 5f, 3f), floatArrayOf(97f, 235f, 8f, 2.5f), floatArrayOf(137f, 214f, 3f, 5f))) {
            drawOval(K.SteelMid, Offset(x, y), Size(w, h)); drawOval(K.SteelLight.copy(alpha = 0.6f), Offset(x + 0.5f, y), Size(w * 0.5f, h * 0.5f))
        }
        drawLine(K.PaintDark.copy(alpha = 0.5f), Offset(70f, 182f), Offset(70f, 236f), 1.2f)
        drawLine(K.PaintDark.copy(alpha = 0.5f), Offset(130f, 182f), Offset(130f, 236f), 1.2f)
    }
    // The brushed chest plate, its rivets, a speaker and the light that is his heart.
    drawRoundRect(Brush.verticalGradient(listOf(K.SteelLight, K.Steel, K.SteelMid), 190f, 228f), Offset(76f, 190f), Size(48f, 38f), CornerRadius(12f))
    drawRoundRect(K.SteelDeep, Offset(76f, 190f), Size(48f, 38f), CornerRadius(12f), style = Stroke(1.3f))
    for (p in listOf(Offset(81f, 195f), Offset(119f, 195f), Offset(81f, 223f), Offset(119f, 223f))) {
        drawCircle(K.SteelDark, 1.9f, p); drawCircle(K.SteelLight, 0.8f, p + Offset(-0.5f, -0.5f))
    }
    for (y in listOf(196f, 200f)) drawLine(K.SteelDark, Offset(89f, y), Offset(111f, y), 1.5f, StrokeCap.Round)
    drawCircle(K.Glass, 8f, Offset(100f, 213f))
    drawCircle(Brush.radialGradient(listOf(K.Heart.copy(alpha = 0.55f * glow), Color.Transparent), Offset(100f, 213f), 15f), 15f, Offset(100f, 213f))
    drawCircle(Brush.radialGradient(listOf(K.IrisCore, K.Heart), Offset(98f, 211f), 7f), 5.5f * (0.85f + 0.15f * glow), Offset(100f, 213f))
    drawCircle(K.IrisCore.copy(alpha = 0.8f), 1.6f, Offset(97.5f, 210.5f))
}

private fun DrawScope.arms(m: PipMotion, walking: Float) {
    val swing = sin(m.gait * PI).toFloat() * 7f * walking
    for (side in listOf(-1f, 1f)) {
        val shoulder = Offset(100f + side * 40f, 194f)
        var elbow = Offset(100f + side * 50f, 214f - side * swing * 0.4f)
        var hand = Offset(100f + side * 54f, 232f - side * swing)
        // Both arms go up when he cheers; the right one waves hello.
        val up = m.cheer
        elbow = lerp(elbow, Offset(100f + side * 56f, 176f), up)
        hand = lerp(hand, Offset(100f + side * 60f, 150f), up)
        if (side > 0 && m.wave > 0f && up < 0.5f) {
            elbow = Offset(162f, 178f)
            hand = Offset(160f + 16f * m.wave, 148f)
        }
        limb(shoulder, elbow, 9f); limb(elbow, hand, 8f)
        joint(elbow, 5.5f); joint(shoulder, 8.5f)
        claw(elbow, hand)
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Three little fingers that carry on from his forearm. */
private fun DrawScope.claw(elbow: Offset, hand: Offset) {
    val d = (hand - elbow).let { it / max(0.001f, hypot(it.x, it.y)) }
    for (a in listOf(-0.5f, 0f, 0.5f)) {
        val dir = Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
        drawLine(K.SteelDark, hand, hand + dir * 8f, 3.4f, StrokeCap.Round)
    }
    drawCircle(Brush.radialGradient(listOf(K.Steel, K.SteelDark), hand + Offset(-1.5f, -1.5f), 7f), 5.2f, hand)
}

// The head, in its own units: shell, ears, antenna, glass face, eyes, light mouth and what he wears.

private fun DrawScope.head(mood: PipMood, outfit: PipOutfit, m: PipMotion) {
    val hatHidesAntenna = outfit in setOf(PipOutfit.SANTA, PipOutfit.WITCH, PipOutfit.BEANIE, PipOutfit.NIGHTCAP)
    ears()
    if (!hatHidesAntenna) rotate(m.antenna, Offset(100f, 40f)) { antenna(outfit, m.glow) }
    shell()
    face(mood, m)
    when (outfit) {
        PipOutfit.SANTA -> floppyHat(K.SantaRed, K.Fur, K.Fur, stars = false)
        PipOutfit.NIGHTCAP -> floppyHat(K.Nightcap, K.BeanieStripe, K.Star, stars = true)
        PipOutfit.WITCH -> witchHat()
        PipOutfit.BEANIE -> beanie()
        PipOutfit.BUNNY -> bunnyEars()
        PipOutfit.SUNGLASSES -> sunglasses()
        PipOutfit.SCARF -> leaf(Offset(150f, 46f), K.Scarf)
        PipOutfit.FLAG -> rosette()
        else -> Unit
    }
    if (m.sparkle > 0f) sparkles(m.sparkle)
    if (mood == PipMood.ASLEEP) sleepyZs(m.snore)
}

private val shellShape = RoundRect(28f, 38f, 172f, 138f, CornerRadius(50f), CornerRadius(50f), CornerRadius(34f), CornerRadius(34f))

private fun DrawScope.shell() {
    val path = Path().apply { addRoundRect(shellShape) }
    drawPath(path, Brush.verticalGradient(0f to K.SteelLight, 0.45f to K.Steel, 0.85f to K.SteelMid, 1f to K.SteelDark, startY = 38f, endY = 138f))
    clipPath(path) {
        drawRect(Brush.horizontalGradient(0f to K.SteelLight.copy(alpha = 0.5f), 0.3f to Color.Transparent, 0.72f to Color.Transparent,
            1f to K.SteelDeep.copy(alpha = 0.4f), startX = 28f, endX = 172f), Offset(28f, 38f), Size(144f, 100f))
        // Fine brushing lines across the steel.
        for (i in 0..9) drawLine(K.SteelLight.copy(alpha = 0.12f), Offset(30f, 44f + i * 9.5f), Offset(170f, 46f + i * 9.5f), 0.8f)
    }
    drawOval(Brush.radialGradient(listOf(K.SteelLight, Color.Transparent), Offset(66f, 54f), 32f), Offset(40f, 44f), Size(56f, 22f))
    drawArc(K.SteelMid, 205f, 130f, false, Offset(50f, 42f), Size(100f, 40f), style = Stroke(1.6f, cap = StrokeCap.Round))
    drawPath(path, K.SteelDeep.copy(alpha = 0.55f), style = Stroke(1.6f))
    for (p in listOf(Offset(37f, 82f), Offset(163f, 82f))) {
        drawCircle(K.SteelMid, 3.2f, p); drawLine(K.SteelDeep, p + Offset(-2f, -1f), p + Offset(2f, 1f), 1.2f)
    }
}

private fun DrawScope.ears() {
    for (x in listOf(24f, 176f)) {
        val c = Offset(x, 90f)
        drawCircle(K.SteelDeep, 18f, c)
        drawCircle(Brush.radialGradient(listOf(K.SteelLight, K.Steel, K.SteelDark), c + Offset(-5f, -6f), 24f), 16.5f, c)
        drawCircle(K.SteelDeep, 10.5f, c, style = Stroke(2f))
        drawCircle(Brush.radialGradient(listOf(K.PaintLight, K.PaintDark), c + Offset(-2f, -2f), 8f), 6f, c)
        drawLine(K.PaintDark, c + Offset(-3f, 0f), c + Offset(3f, 0f), 1.4f, StrokeCap.Round)
    }
}

private fun DrawScope.antenna(outfit: PipOutfit, glow: Float) {
    val top = if (outfit == PipOutfit.FLAG) -22f else 14f
    drawLine(K.SteelDark, Offset(100f, 42f), Offset(100f, top), 4f, StrokeCap.Round)
    drawLine(K.SteelLight, Offset(99.2f, 40f), Offset(99.2f, top + 2f), 1.1f, StrokeCap.Round)
    drawOval(Brush.verticalGradient(listOf(K.SteelLight, K.SteelDark), 32f, 44f), Offset(88f, 32f), Size(24f, 12f))
    when (outfit) {
        PipOutfit.FLOWER -> flower(Offset(100f, 8f))
        PipOutfit.FLAG -> flag(Offset(102f, -22f))
        else -> {
            drawCircle(Brush.radialGradient(listOf(K.Bulb.copy(alpha = 0.45f * glow), Color.Transparent), Offset(100f, 11f), 18f), 18f, Offset(100f, 11f))
            drawCircle(Brush.radialGradient(listOf(K.IrisCore, K.Bulb), Offset(97f, 8f), 9f), 7.5f, Offset(100f, 11f))
            drawCircle(K.IrisCore.copy(alpha = 0.8f), 2.2f, Offset(97.5f, 8.5f))
        }
    }
}

private fun DrawScope.face(mood: PipMood, m: PipMotion) {
    val plate = Path().apply { addRoundRect(RoundRect(44f, 60f, 156f, 126f, CornerRadius(28f))) }
    drawPath(plate, Brush.verticalGradient(listOf(K.GlassEdge, K.Glass), 60f, 126f))
    // Blush lights behind the glass.
    val warm = if (mood in setOf(PipMood.HAPPY, PipMood.WAVE, PipMood.EXCITED)) 0.5f else 0.22f
    for (x in listOf(58f, 142f)) drawCircle(Brush.radialGradient(listOf(K.Cheek.copy(alpha = warm), Color.Transparent), Offset(x, 110f), 11f), 11f, Offset(x, 110f))
    eyes(mood, m)
    mouth(mood, m.talk)
    // The glass: a reflection across the top, a bright rim and a dark bezel.
    clipPath(plate) {
        val shine = Path().apply { moveTo(48f, 74f); quadraticTo(70f, 58f, 120f, 60f); lineTo(118f, 66f); quadraticTo(74f, 66f, 50f, 84f); close() }
        drawPath(shine, K.IrisCore.copy(alpha = 0.10f))
    }
    drawPath(plate, K.SteelDeep, style = Stroke(3.5f))
    drawPath(plate, K.SteelLight.copy(alpha = 0.18f), style = Stroke(1f))
}

private fun DrawScope.eyes(mood: PipMood, m: PipMotion) {
    val lx = m.lookX.coerceIn(-1f, 1f); val ly = m.lookY.coerceIn(-1f, 1f)
    for ((i, x) in listOf(78f, 122f).withIndex()) {
        val c = Offset(x, 92f)
        val (moodTop, bottom) = when (mood) {
            PipMood.HAPPY, PipMood.WAVE -> 0.12f to 0.28f
            PipMood.CURIOUS -> (if (i == 0) 0.30f else 0f) to 0.05f
            PipMood.EXCITED -> 0f to 0.16f
            PipMood.SLEEPY -> 0.58f to 0.12f
            PipMood.ASLEEP -> 1f to 0f
        }
        val top = max(moodTop, m.blink)
        val iris = when { mood == PipMood.EXCITED -> 13.5f; mood == PipMood.CURIOUS && i == 1 -> 13f; else -> 11.5f }
        drawCircle(Brush.radialGradient(listOf(K.Iris.copy(alpha = 0.28f * (1f - top)), Color.Transparent), c, 24f), 24f, c)
        drawCircle(Brush.radialGradient(listOf(K.Steel, K.SteelDark, K.SteelDeep), c + Offset(-6f, -7f), 26f), 19f, c)
        val hole = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - 15.5f, c.y - 15.5f, c.x + 15.5f, c.y + 15.5f)) }
        clipPath(hole) {
            drawCircle(K.Pupil, 15.5f, c)
            val ic = c + Offset(lx * 3.5f, ly * 3f)
            drawCircle(Brush.radialGradient(0f to K.IrisCore, 0.35f to K.Iris, 1f to K.IrisDeep, center = ic, radius = iris), iris, ic)
            drawCircle(K.IrisDeep.copy(alpha = 0.7f), 7.5f, ic, style = Stroke(1.2f))
            drawCircle(K.Pupil, 4.6f, ic + Offset(lx * 1f, ly * 1f))
            drawCircle(K.IrisCore, 3.2f, ic + Offset(-4.5f, -5f))
            drawCircle(K.IrisCore.copy(alpha = 0.7f), 1.5f, ic + Offset(4f, 4f))
            // Metal lids: the top one blinks and droops, the bottom one squints when he smiles.
            val lid = Brush.verticalGradient(listOf(K.SteelLight, K.Steel, K.SteelMid), c.y - 16f, c.y + 16f)
            if (top > 0f) {
                drawRect(lid, Offset(c.x - 16f, c.y - 16f), Size(32f, 32f * top))
                drawLine(K.SteelDeep, Offset(c.x - 16f, c.y - 16f + 32f * top), Offset(c.x + 16f, c.y - 16f + 32f * top), 1.6f)
            }
            if (bottom > 0f) {
                drawOval(lid, Offset(c.x - 21f, c.y + 16f - 32f * bottom), Size(42f, 40f))
                drawOval(K.SteelDeep, Offset(c.x - 21f, c.y + 16f - 32f * bottom), Size(42f, 40f), style = Stroke(1.4f))
            }
        }
        drawCircle(K.SteelDeep, 15.5f, c, style = Stroke(1.2f))
    }
}

/** Seven lights behind the glass: a smile, a little o, a yawn, or words when he talks. */
private fun DrawScope.mouth(mood: PipMood, talk: Float) {
    val dim = if (mood == PipMood.ASLEEP || mood == PipMood.SLEEPY) 0.45f else 1f
    fun led(at: Offset, r: Float = 2f) {
        drawCircle(K.Iris.copy(alpha = 0.28f * dim), r * 2f, at)
        drawCircle(K.Iris.copy(alpha = dim), r, at)
    }
    if (talk > 0.05f) {
        for (i in 0 until 7) {
            val h = 2.5f + 8f * talk * abs(sin(i * 1.7f + talk * 5f))
            drawRoundRect(K.Iris.copy(alpha = 0.3f), Offset(78f + i * 7f - 2.6f, 113f - h / 2 - 1.5f), Size(5.2f, h + 3f), CornerRadius(2.6f))
            drawRoundRect(K.Iris, Offset(78f + i * 7f - 1.4f, 113f - h / 2), Size(2.8f, h), CornerRadius(1.4f))
        }
        return
    }
    when (mood) {
        PipMood.CURIOUS -> for (i in 0 until 6) { val a = i * PI / 3; led(Offset(102f + 4.5f * cos(a).toFloat(), 114f + 4.5f * sin(a).toFloat()), 1.6f) }
        PipMood.EXCITED -> {
            for (i in 0 until 7) led(Offset(79f + i * 7f, 110f + (9 - (i - 3) * (i - 3)) * 0.55f))
            for (i in 1 until 6) led(Offset(79f + i * 7f, 116f + (9 - (i - 3) * (i - 3)) * 0.9f), 1.7f)
        }
        PipMood.SLEEPY, PipMood.ASLEEP -> for (i in 1 until 6) led(Offset(79f + i * 7f, 114f), 1.6f)
        else -> for (i in 0 until 7) led(Offset(79f + i * 7f, 110f + (9 - (i - 3) * (i - 3)) * 0.45f))
    }
}

private fun DrawScope.sparkles(p: Float) {
    for (i in 0 until 8) {
        val a = i * PI / 4 + 0.3
        val d = 70f + 80f * p
        val at = Offset(100f + d * cos(a).toFloat(), 88f + d * sin(a).toFloat() * 0.8f)
        drawPath(sparkle(at, 9f * (1f - p) + 2f), K.Star.copy(alpha = (1f - p).coerceIn(0f, 1f)))
    }
}

private fun DrawScope.sleepyZs(phase: Float) {
    for (i in 0..2) {
        val t = (phase + i / 3f) % 1f
        val size = 8f + 4f * i
        val at = Offset(150f + 14f * i + 6f * t, 30f - 22f * i - 18f * t)
        val z = Path().apply { moveTo(at.x, at.y); lineTo(at.x + size, at.y); lineTo(at.x, at.y + size); lineTo(at.x + size, at.y + size) }
        drawPath(z, K.IrisCore.copy(alpha = (1f - t) * 0.9f), style = Stroke(2.6f, cap = StrokeCap.Round))
    }
}

// What he wears, in his head's units.

private fun DrawScope.floppyHat(cloth: Color, band: Color, bobble: Color, stars: Boolean) {
    val dome = Path().apply { moveTo(36f, 52f); cubicTo(36f, 4f, 164f, 4f, 164f, 52f); close() }
    val tip = Path().apply { moveTo(112f, 12f); cubicTo(150f, 0f, 178f, 14f, 188f, 44f); lineTo(176f, 48f); cubicTo(168f, 32f, 154f, 26f, 138f, 30f); close() }
    drawPath(dome, Brush.verticalGradient(listOf(cloth, cloth, K.Shadow.copy(alpha = 0.3f).compositeOn(cloth)), 4f, 52f)); drawPath(tip, cloth)
    if (stars) { drawPath(sparkle(Offset(70f, 30f), 5f), K.Star); drawPath(sparkle(Offset(112f, 22f), 4f), K.Star); drawPath(sparkle(Offset(150f, 26f), 3.5f), K.Star) }
    drawCircle(Brush.radialGradient(listOf(bobble, K.ShellShade), Offset(183f, 46f), 14f), 10f, Offset(186f, 50f))
    drawRoundRect(Brush.verticalGradient(listOf(band, K.ShellShade), 40f, 60f), Offset(30f, 40f), Size(140f, 19f), CornerRadius(9.5f))
}

private fun Color.compositeOn(under: Color): Color {
    val a = alpha
    return Color(red * a + under.red * (1 - a), green * a + under.green * (1 - a), blue * a + under.blue * (1 - a), 1f)
}

private fun DrawScope.witchHat() {
    drawOval(K.WitchPurple, Offset(16f, 34f), Size(168f, 24f))
    drawOval(K.Shadow.copy(alpha = 0.25f), Offset(30f, 42f), Size(140f, 12f))
    val cone = Path().apply { moveTo(56f, 46f); cubicTo(66f, 10f, 82f, -18f, 112f, -36f); cubicTo(108f, -14f, 120f, 8f, 144f, 46f); close() }
    drawPath(cone, Brush.horizontalGradient(listOf(K.WitchPurple, K.WitchPurple, K.Shadow.copy(alpha = 0.35f).compositeOn(K.WitchPurple)), 56f, 144f))
    clipPath(cone) { drawRect(K.WitchBand, Offset(40f, 30f), Size(120f, 12f)) }
    drawRoundRect(K.Star, Offset(94f, 30f), Size(12f, 12f), CornerRadius(2f), style = Stroke(2.5f))
    drawPath(sparkle(Offset(92f, 4f), 5f), K.Star)
}

private fun DrawScope.beanie() {
    val dome = Path().apply { moveTo(34f, 56f); cubicTo(34f, 4f, 166f, 4f, 166f, 56f); close() }
    drawPath(dome, K.Beanie)
    clipPath(dome) {
        drawRect(K.BeanieStripe, Offset(30f, 22f), Size(140f, 6f)); drawRect(K.BeanieStripe, Offset(30f, 34f), Size(140f, 4f))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, K.Shadow.copy(alpha = 0.25f)), 100f, 166f), Offset(30f, 0f), Size(140f, 60f))
    }
    drawRoundRect(K.BeanieStripe, Offset(30f, 44f), Size(140f, 18f), CornerRadius(9f))
    for (x in 0..8) drawLine(K.Beanie.copy(alpha = 0.35f), Offset(42f + x * 15f, 46f), Offset(42f + x * 15f, 60f), 2f)
    drawCircle(Brush.radialGradient(listOf(K.BeanieStripe, K.ShellShade), Offset(96f, 4f), 16f), 13f, Offset(100f, 8f))
}

private fun DrawScope.bunnyEars() {
    drawArc(K.SteelDeep, 200f, 140f, false, Offset(40f, 36f), Size(120f, 40f), style = Stroke(5f, cap = StrokeCap.Round))
    for ((x, lean) in listOf(74f to -6f, 126f to 6f)) {
        drawOval(K.Fur, Offset(x - 13f + lean, -18f), Size(26f, 62f))
        drawOval(K.BunnyPink, Offset(x - 6f + lean, -8f), Size(12f, 42f))
    }
}

private fun DrawScope.sunglasses() {
    for (x in listOf(60f, 104f)) {
        val lens = RoundRect(x, 76f, x + 36f, 106f, CornerRadius(12f))
        drawPath(Path().apply { addRoundRect(lens) }, K.Lens)
        clipPath(Path().apply { addRoundRect(lens) }) { drawLine(K.LensShine.copy(alpha = 0.55f), Offset(x + 4f, 106f), Offset(x + 26f, 72f), 7f) }
        drawPath(Path().apply { addRoundRect(lens) }, K.Frame, style = Stroke(3f))
    }
    drawLine(K.Frame, Offset(96f, 86f), Offset(104f, 86f), 3f, StrokeCap.Round)
}

private fun DrawScope.scarf() {
    drawRoundRect(K.Scarf, Offset(62f, 132f), Size(76f, 18f), CornerRadius(9f))
    drawRoundRect(K.Scarf, Offset(112f, 142f), Size(17f, 40f), CornerRadius(6f))
    for (y in listOf(152f, 164f)) drawRect(K.ScarfStripe, Offset(112f, y), Size(17f, 4f))
    for (x in listOf(74f, 92f, 110f, 128f)) drawRect(K.ScarfStripe, Offset(x, 134f), Size(4f, 14f))
    for (i in 0..3) drawLine(K.Scarf, Offset(114f + i * 4.5f, 182f), Offset(114f + i * 4.5f, 188f), 2f, StrokeCap.Round)
}

private fun DrawScope.leaf(at: Offset, colour: Color) {
    val leaf = Path().apply { moveTo(at.x, at.y); quadraticTo(at.x + 14f, at.y - 14f, at.x + 22f, at.y - 2f); quadraticTo(at.x + 10f, at.y + 10f, at.x, at.y); close() }
    drawPath(leaf, colour)
    drawLine(K.Trunk, Offset(at.x, at.y), Offset(at.x + 18f, at.y - 3f), 1.5f, StrokeCap.Round)
}

private fun DrawScope.flower(at: Offset) {
    drawOval(K.Leaf, Offset(at.x - 18f, at.y + 14f), Size(16f, 8f))
    for (i in 0 until 5) {
        val a = i * 2 * PI / 5 - PI / 2
        drawCircle(if (i % 2 == 0) K.Petal else K.PetalPink, 7f, Offset(at.x + 8f * cos(a).toFloat(), at.y + 8f * sin(a).toFloat()))
    }
    drawCircle(K.FlowerHeart, 5.5f, at)
}

private fun DrawScope.flag(at: Offset) {
    val w = 44f; val h = 32f
    drawRect(K.FlagRed, at, Size(w, h))
    drawRect(K.FlagWhite, Offset(at.x + 12f, at.y), Size(8f, h)); drawRect(K.FlagWhite, Offset(at.x, at.y + 12f), Size(w, 8f))
    drawRect(K.FlagBlue, Offset(at.x + 14f, at.y), Size(4f, h)); drawRect(K.FlagBlue, Offset(at.x, at.y + 14f), Size(w, 4f))
}

private fun DrawScope.rosette() {
    translate(0f, -HEAD_Y) { for ((i, c) in listOf(K.FlagRed, K.FlagWhite, K.FlagBlue).withIndex()) drawCircle(c, 9f - i * 3f, Offset(134f, 196f)) }
}

/** A four-pointed twinkle. */
internal fun sparkle(centre: Offset, r: Float) = Path().apply {
    moveTo(centre.x, centre.y - r)
    quadraticTo(centre.x, centre.y, centre.x + r, centre.y)
    quadraticTo(centre.x, centre.y, centre.x, centre.y + r)
    quadraticTo(centre.x, centre.y, centre.x - r, centre.y)
    quadraticTo(centre.x, centre.y, centre.x, centre.y - r)
    close()
}
