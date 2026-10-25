package app.stova.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * The one set of durations for every transition in Stova. Only alpha, scale and offset are ever
 * animated, always in a graphics layer; nothing keeps moving once a transition has ended. Under
 * reduced motion every value is zero, so each transition becomes a plain swap.
 */
@Immutable
data class MotionTokens(
    /** Small things: a label, a chip, a section crossfade, every exit. */
    val quick: Int,
    /** Pages and dialogs arriving. */
    val standard: Int,
    /** The spotlight text leaving. */
    val emphasized: Int,
    /** Ambient changes nobody waits for: the backdrop and the spotlight text arriving. */
    val ambient: Int,
    val focusRing: Int,
    val focusZoom: Int,
) {
    val still: Boolean get() = standard == 0

    /** Something arriving settles with a calm deceleration. */
    fun <T> enter(millis: Int = standard, delayMillis: Int = 0): TweenSpec<T> =
        tween(millis, if (still) 0 else delayMillis, StovaMotion.Enter)

    /** Something leaving gets out of the way quickly. */
    fun <T> exit(millis: Int = quick): TweenSpec<T> = tween(millis, 0, StovaMotion.Exit)

    /** A symmetric fade, for crossfades and values that go both ways. */
    fun <T> fade(millis: Int = quick): TweenSpec<T> = tween(millis, 0, FastOutSlowInEasing)
}

object StovaMotion {
    const val QuickMillis = 150
    const val StandardMillis = 240
    const val EmphasizedMillis = 360
    const val AmbientMillis = 900
    const val FocusRingMillis = 110
    const val FocusZoomMillis = 180

    /** Material's emphasized decelerate: fast out of the gate, a long soft landing. */
    val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Material's emphasized accelerate: a quiet start, then gone. */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    val Full = MotionTokens(QuickMillis, StandardMillis, EmphasizedMillis, AmbientMillis, FocusRingMillis, FocusZoomMillis)
    val Still = MotionTokens(0, 0, 0, 0, 0, 0)

    fun tokens(reduceMotion: Boolean): MotionTokens = if (reduceMotion) Still else Full
}

/** The tokens for the active profile: all zero when it asks for reduced motion. */
val stovaMotion: MotionTokens
    @Composable get() = StovaMotion.tokens(LocalReduceMotion.current)
