package app.stova.kids

import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Pip's body: a small physics model stepped once per frame, so he moves like a little machine
 * with weight. He walks along the top of the cards with a waddle, his steps keeping time with how
 * far he goes; he hops to another row on an arc and lands with a squash; his antenna swings on
 * its own spring and his eyes get there first. Left alone, he looks about, tilts his head, taps a
 * foot and in the end sits down; anything happening stands him straight up again.
 *
 * Only [step] changes anything, and it says whether another frame is wanted. When Pip has nothing
 * to do, no frames are drawn until [nextWakeMillis], so a quiet Home costs the TV nothing.
 *
 * [x], [y] are the top-left of his drawing in screen pixels, with his feet on [y] + feet line.
 * The pose is in degrees and in his own drawing units.
 */
@Stable
class PipMotion(private val random: Random = Random.Default) {
    var x by mutableFloatStateOf(0f); private set
    var y by mutableFloatStateOf(0f); private set
    private var vx = 0f
    /** How fast he is walking sideways, in screen pixels a second; his body turns that way. */
    val speedX get() = vx
    private var tx = 0f; private var ty = 0f
    private var placed = false

    /** Steps taken; each whole number is one step, left and right in turn. */
    var gait by mutableFloatStateOf(0f); private set
    /** 0 standing still … 1 walking, up to 1.5 running. */
    var stride by mutableFloatStateOf(0f); private set
    /** 0 on the ground … 1 legs tucked at the top of a hop. */
    var air by mutableFloatStateOf(0f); private set
    var sit by mutableFloatStateOf(0f); private set
    var lean by mutableFloatStateOf(0f); private set
    var headTilt by mutableFloatStateOf(0f); private set
    var headDy by mutableFloatStateOf(0f); private set
    var antenna by mutableFloatStateOf(0f); private set
    var squash by mutableFloatStateOf(1f); private set
    var breath by mutableFloatStateOf(0f); private set
    var tap by mutableFloatStateOf(0f); private set
    var glow by mutableFloatStateOf(0.85f); private set
    var lookX by mutableFloatStateOf(-0.4f); private set
    var lookY by mutableFloatStateOf(0.7f); private set
    var blink by mutableFloatStateOf(0f); private set
    var talk by mutableFloatStateOf(0f); private set
    var cheer by mutableFloatStateOf(0f); private set
    var wave by mutableFloatStateOf(0f); private set
    var sparkle by mutableFloatStateOf(0f); private set
    var snore by mutableFloatStateOf(0f); private set

    /** Pressed lately: he stays on his feet and breathes. Otherwise he rests, and sits after a while. */
    var lively = true
        set(value) { if (field != value) { field = value; restSince = time; wake.trySend(Unit) } }
    /** Reduced motion: he goes straight to his place and only blinks. */
    var still = false
    var asleep = false
        set(value) { if (field != value) { field = value; wake.trySend(Unit) } }
    /** Anything that needs him to move wakes him from a pause between idle moments. */
    private val wake = Channel<Unit>(Channel.CONFLATED)
    /** Where his eyes rest once he is there: down at the card under him. */
    var restLook = Offset(-0.45f, 0.75f)

    private var time = 0f
    private var lastNanos = 0L
    private var antennaV = 0f; private var squashV = 0f; private var tiltTarget = 0f
    private var jumpStart = -10f; private var jumpFrom = 0f; private var jumpLength = 0.45f; private var jumpHeight = 60f
    private var restSince = 0f
    private var inJump = false
    private var nextBlink = 1.5f; private var blinkStart = -10f; private var doubleBlink = false
    private var nextIdle = 4f; private var idleKind = Idle.NONE; private var idleStart = 0f; private var idleLook = Offset.Zero
    private var talkUntil = -1f; private var cheerStart = -10f; private var waveUntil = -1f

    private enum class Idle { NONE, GLANCE, SCAN, TILT, TAP, HOP }

    private val jumping get() = time - jumpStart < jumpLength
    private val tiltGoal get() = if (idleKind == Idle.TILT) tiltTarget else 0f

    /** Draws him after each step: set by the 3D view, which does not read Compose state. */
    var onFrame: ((Long) -> Unit)? = null

    /** Steps him every frame while he has something to do, and sleeps the rest of the time. */
    suspend fun run() {
        while (true) {
            if (wantsFrames()) withFrameNanos { step(it); onFrame?.invoke(it) }
            else {
                val wait = nextWakeMillis()
                val woken = withTimeoutOrNull(wait) { wake.receive() } != null
                resumeAfter(if (woken) 16L else wait)
                withFrameNanos { step(it); onFrame?.invoke(it) }
            }
        }
    }
    /** Something about him changed that is not motion (his mood, his clothes): draw him again. */
    fun poke() { wake.trySend(Unit) }

    fun moveTo(target: Offset) {
        wake.trySend(Unit)
        if (!placed || still) { tx = target.x; ty = target.y; x = tx; y = ty; vx = 0f; placed = true; return }
        if (abs(target.y - ty) > 12f || (jumping && abs(target.y - ty) > 1f)) {
            if (!jumping) { jumpStart = time; jumpFrom = y; squashV -= 1.4f }
            val rise = max(0f, jumpFrom - target.y)
            jumpHeight = 46f + rise * 0.35f
            jumpLength = (0.42f + abs(target.y - jumpFrom) / 2400f).coerceAtMost(0.7f)
        }
        tx = target.x; ty = target.y
        restSince = time
    }

    /** His light mouth moves for about as long as the words take. */
    fun say(text: String) { wake.trySend(Unit); talkUntil = time + (0.35f + text.length * 0.065f).coerceIn(0.6f, 3.2f); restSince = time }
    /** OK on a card: a hop with both arms up and a burst of sparkles. */
    fun celebrate() {
        wake.trySend(Unit)
        cheerStart = time; restSince = time
        if (!still && !jumping) { jumpStart = time; jumpFrom = y; jumpHeight = 70f; jumpLength = 0.5f; squashV -= 1.8f }
    }
    fun greet(seconds: Float = 2.6f) { wake.trySend(Unit); waveUntil = time + seconds }

    fun wantsFrames(): Boolean {
        val walking = abs(tx - x) > 0.5f || abs(vx) > 2f || stride > 0.01f || jumping || abs(ty - y) > 0.5f
        val settling = abs(antenna) > 0.2f || abs(antennaV) > 0.5f || abs(squash - 1f) > 0.003f || abs(squashV) > 0.01f ||
            abs(headTilt - tiltGoal) > 0.1f || abs(lean) > 0.1f || abs(sit - sitTarget()) > 0.005f || air > 0.005f
        val busy = time - blinkStart < 0.6f || idleKind != Idle.NONE || time < talkUntil || time - cheerStart < 1f || time < waveUntil
        return walking || settling || busy || (lively && !still && !asleep)
    }

    /** How long a resting Pip can wait for his next blink or idle moment. Asleep, he snores at a slow beat. */
    fun nextWakeMillis(): Long = if (asleep) 110L else (min(nextBlink, nextIdle) - time).coerceIn(0.05f, 6f).times(1000).toLong()

    /** Time passed without frames. */
    fun resumeAfter(millis: Long) { time += millis / 1000f; lastNanos = 0L; if (asleep) snore = (snore + millis / 2600f) % 1f }

    fun step(frameNanos: Long): Boolean {
        val dt = if (lastNanos == 0L) 1f / 60f else ((frameNanos - lastNanos) / 1e9f).coerceIn(0f, 1f / 30f)
        lastNanos = frameNanos
        time += dt
        walk(dt)
        pose(dt)
        eyes(dt)
        face()
        return wantsFrames()
    }

    private fun sitTarget() = if (asleep || (!lively && time - restSince > 5f && idleKind == Idle.NONE)) 1f else 0f

    private fun walk(dt: Float) {
        if (still) { x = tx; y = ty; vx = 0f; stride = 0f; return }
        // He has to stand up before he can walk anywhere.
        val dx = tx - x
        val standing = sit < 0.35f
        val far = abs(dx) > 700f
        val top = if (far) 1500f else 760f
        val brake = 3200f
        // Full speed, then braking to stop on the spot; the last few pixels ease in instead of chattering.
        val wanted = if (!standing) 0f else sign(dx) * minOf(top, sqrt(2f * brake * abs(dx)), abs(dx) * 14f)
        val accel = 4200f
        val before = vx
        vx += (wanted - vx).coerceIn(-accel * dt, accel * dt)
        x += vx * dt
        if (abs(dx) < 0.5f && abs(vx) < 6f) { x = tx; vx = 0f }
        val ax = (vx - before) / dt.coerceAtLeast(1e-4f)
        // Hops between rows follow an arc and land with a squash.
        if (jumping) {
            val p = ((time - jumpStart) / jumpLength).coerceIn(0f, 1f)
            val eased = p * p * (3f - 2f * p)
            y = jumpFrom + (ty - jumpFrom) * eased - jumpHeight * sin(p * PI).toFloat()
            air = sin(p * PI).toFloat()
            inJump = true
        } else {
            if (inJump) { inJump = false; air = 0f; squashV -= 1.3f }
            y += (ty - y) * min(1f, dt * 20f)
            air *= max(0f, 1f - dt * 12f)
        }
        // A step is about 46 px of ground; running takes longer strides.
        val stepLength = if (far) 80f else 46f
        if (!jumping) gait += abs(vx) * dt / stepLength
        val strideTarget = if (jumping) 0f else (abs(vx) / 520f).coerceIn(0f, 1.5f)
        stride += (strideTarget - stride) * min(1f, dt * 10f)
        if (stride < 0.01f && abs(vx) < 1f) { stride = 0f; gait = (gait * 2f).let { kotlin.math.round(it) } / 2f }
        // The antenna swings on its own spring, driven by how he starts and stops.
        val aa = -60f * antenna - 6.5f * antennaV - ax * 0.008f
        antennaV += aa * dt; antenna = (antenna + antennaV * dt).coerceIn(-28f, 28f)
        if (abs(vx) > 5f || jumping) restSince = time
    }

    private fun pose(dt: Float) {
        val sa = -150f * (squash - 1f) - 12f * squashV
        squashV += sa * dt; squash = (squash + squashV * dt).coerceIn(0.84f, 1.12f)
        // A waddle: he rocks onto each foot as he steps, and leans a little into the way he goes.
        val waddle = sin(gait * PI).toFloat() * 5f * stride.coerceAtMost(1f)
        lean += ((vx / 900f * 7f).coerceIn(-9f, 9f) + waddle - lean) * min(1f, dt * 14f)
        headDy += ((if (jumping) -air * 6f else 0f) - headDy) * min(1f, dt * 8f)
        headTilt += (tiltGoal - headTilt) * min(1f, dt * 6f)
        if (abs(headTilt - tiltGoal) < 0.1f) headTilt = tiltGoal
        val st = sitTarget()
        sit += (st - sit) * min(1f, dt * (if (st > sit) 2.2f else 9f))
        // Settled things are set exactly, so a resting Pip asks for no more frames.
        if (abs(sit - st) < 0.005f) sit = st
        if (abs(squash - 1f) < 0.003f && abs(squashV) < 0.01f) { squash = 1f; squashV = 0f }
        if (abs(lean) < 0.1f && stride == 0f) lean = 0f
        if (air < 0.005f) air = 0f
        if (abs(antenna) < 0.2f && abs(antennaV) < 0.5f) { antenna = 0f; antennaV = 0f }
        breath = if (lively && !still && !asleep) sin(time * 2.2f) else if (asleep) sin(time * 1.1f) else 0f
        tap = if (idleKind == Idle.TAP) max(0f, sin(((time - idleStart) / 0.28f) * PI).toFloat()) else 0f
        glow = if (asleep) 0.35f + 0.15f * sin(time * 1.2f) else if (lively && !still) 0.78f + 0.22f * sin(time * 3.1f) else 0.85f
        if (asleep) snore = (snore + dt / 2.6f) % 1f
    }

    private fun eyes(dt: Float) {
        if (time >= nextBlink && !asleep) {
            blinkStart = time; doubleBlink = random.nextFloat() < 0.22f
            nextBlink = time + 2.4f + random.nextFloat() * 3.6f
        }
        val since = time - blinkStart
        blink = when {
            asleep -> 1f
            since < 0.07f -> since / 0.07f
            since < 0.11f -> 1f
            since < 0.23f -> 1f - (since - 0.11f) / 0.12f
            doubleBlink && since < 0.30f -> (since - 0.23f) / 0.07f
            doubleBlink && since < 0.42f -> 1f - (since - 0.30f) / 0.12f
            else -> 0f
        }
        val moving = abs(tx - x) > 8f || jumping
        if (idleKind == Idle.NONE && time >= nextIdle && !moving && !asleep && sit < 0.5f) {
            val choices = listOf(Idle.GLANCE, Idle.GLANCE, Idle.SCAN, Idle.TILT, Idle.TAP, if (lively && !still) Idle.HOP else Idle.TILT)
            idleKind = choices[random.nextInt(choices.size)]
            idleStart = time
            idleLook = Offset(random.nextFloat() * 2f - 1f, random.nextFloat() * 1.4f - 0.7f)
            tiltTarget = if (random.nextBoolean()) 10f else -10f
            if (idleKind == Idle.HOP) { jumpStart = time; jumpFrom = y; jumpHeight = 34f; jumpLength = 0.38f; squashV -= 1.1f }
        }
        val idleAge = time - idleStart
        val idleLength = when (idleKind) { Idle.GLANCE -> 1.1f; Idle.SCAN -> 1.6f; Idle.TILT -> 1.5f; Idle.TAP -> 1.15f; Idle.HOP -> 0.6f; Idle.NONE -> 0f }
        if (idleKind != Idle.NONE && idleAge > idleLength) {
            idleKind = Idle.NONE
            nextIdle = time + (if (lively) 3.5f else 6f) + random.nextFloat() * 5f
        }
        val (wantX, wantY) = when {
            asleep -> 0f to 0.3f
            moving -> ((tx - x) / 140f).coerceIn(-1f, 1f) to ((ty - y) / 140f).coerceIn(-1f, 1f)
            time - cheerStart < 1f -> 0f to -0.35f
            idleKind == Idle.GLANCE -> idleLook.x to idleLook.y
            idleKind == Idle.SCAN -> sin((idleAge / 1.6f) * 2 * PI).toFloat() to -0.1f
            idleKind == Idle.TAP -> 0f to 0.9f
            else -> restLook.x to restLook.y
        }
        // Eyes are quick: they get there well before he does.
        val saccade = min(1f, dt * 16f)
        lookX += (wantX - lookX) * saccade; lookY += (wantY - lookY) * saccade
    }

    private fun face() {
        talk = if (time < talkUntil) 0.25f + 0.75f * abs(sin(time * 13f + sin(time * 4.7f) * 2f)) else max(0f, talk - 0.15f)
        val c = time - cheerStart
        cheer = if (c < 0.9f) (if (c < 0.15f) c / 0.15f else 1f - ((c - 0.15f) / 0.75f).coerceIn(0f, 1f)) else 0f
        sparkle = if (c < 1f) c else 0f
        wave = if (time < waveUntil) (sin(time * 9f) + 1f) / 2f else max(0f, wave - 0.1f)
    }
}
