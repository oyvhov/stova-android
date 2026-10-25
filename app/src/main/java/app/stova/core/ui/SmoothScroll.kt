package app.stova.core.ui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import app.stova.core.theme.MotionTokens

/** Retargetable TV scrolling. Re-measure remaining distance as lazy items arrive, never snap. */
@OptIn(ExperimentalFoundationApi::class)
suspend fun LazyListState.smoothScrollToItem(index: Int, motion: MotionTokens) {
    if (motion.still) {
        scrollToItem(index)
        return
    }
    scroll {
        val layout = LazyLayoutScrollScope(this@smoothScrollToItem, this)
        if (layout.calculateDistanceTo(index) == 0) return@scroll
        var previous = 0f
        animate(0f, 1f, animationSpec = motion.travel()) { progress, _ ->
            val remaining = layout.calculateDistanceTo(index).toFloat()
            val fraction = if (progress >= 1f) 1f else (progress - previous) / (1f - previous)
            scrollBy(remaining * fraction)
            previous = progress
        }
    }
}
