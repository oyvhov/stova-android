package app.stova.core.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import app.stova.core.theme.MotionTokens
import app.stova.core.theme.StovaMotion
import java.util.WeakHashMap
import kotlin.math.sqrt

/**
 * Retargetable TV scrolling on a no-bounce spring. The remaining distance is re-measured as lazy
 * items arrive, and a press that interrupts a scroll keeps its speed, so holding Down glides
 * instead of starting from rest at every row. Never snaps, never overshoots.
 */
@OptIn(ExperimentalFoundationApi::class)
suspend fun LazyListState.smoothScrollToItem(index: Int, motion: MotionTokens) {
    if (motion.still) {
        scrollToItem(index)
        return
    }
    scroll {
        val layout = LazyLayoutScrollScope(this@smoothScrollToItem, this)
        val distance = layout.calculateDistanceTo(index).toFloat()
        if (distance == 0f) return@scroll
        val carried = glides[this@smoothScrollToItem]?.takeIf { it.fresh() }?.speed ?: 0f
        // Only speed in the new direction carries over, and never enough to pass the target.
        val start = (carried / distance).coerceIn(0f, MAX_CARRY * TRAVEL_OMEGA)
        var previous = 0f
        try {
            animate(0f, 1f, initialVelocity = start, animationSpec = motion.travel()) { progress, velocity ->
                val remaining = layout.calculateDistanceTo(index).toFloat()
                val fraction = if (progress >= 1f || previous >= 1f) 1f else (progress - previous) / (1f - previous)
                scrollBy(remaining * fraction.coerceIn(0f, 1f))
                previous = progress
                glides[this@smoothScrollToItem] = Glide(velocity * remaining / (1f - progress).coerceAtLeast(0.01f))
            }
        } finally {
            if (previous >= 1f) glides.remove(this@smoothScrollToItem)
        }
    }
}

/** The speed of an interrupted scroll in px/s, for the press that follows within a moment. */
private class Glide(val speed: Float, private val at: Long = System.nanoTime()) {
    fun fresh() = System.nanoTime() - at < CARRY_WINDOW_NANOS
}

private val glides = WeakHashMap<LazyListState, Glide>()
private const val CARRY_WINDOW_NANOS = 120_000_000L
private val TRAVEL_OMEGA = sqrt(StovaMotion.TravelStiffness)
/** A critically damped spring overshoots once its start speed passes omega; stay well under. */
private const val MAX_CARRY = 0.8f
