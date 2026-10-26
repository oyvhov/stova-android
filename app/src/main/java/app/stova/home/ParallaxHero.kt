package app.stova.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.stova.core.theme.LocalReduceMotion
import app.stova.media.SpotlightEntry

/**
 * How far Home's scrolling view has left its hero, for the full-screen backdrop behind it. Only
 * [progress] reads scroll state, and only layers and drawing call it, so nothing recomposes per frame.
 */
@Stable
class HomeScrollShift {
    internal var list by mutableStateOf<LazyListState?>(null)
    internal var heroPx by mutableFloatStateOf(1f)
    /** 0 with the hero in full view, 1 once the rows have taken the screen. */
    fun progress(): Float {
        val state = list ?: return 0f
        return if (state.firstVisibleItemIndex > 0) 1f else (state.firstVisibleItemScrollOffset / heroPx).coerceIn(0f, 1f)
    }
}

val LocalHomeScrollShift = staticCompositionLocalOf<HomeScrollShift?> { null }

/** A scrolling header. Only its layer reads scroll state; the media screen never recomposes per frame. */
@Composable
internal fun ParallaxHero(entry: SpotlightEntry?, height: Dp, list: LazyListState) {
    val still = LocalReduceMotion.current
    val heightPx = with(LocalDensity.current) { height.toPx().coerceAtLeast(1f) }
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        HeroPanel(entry, Modifier.fillMaxWidth().height(height).graphicsLayer {
            val offset = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else heightPx
            translationY = if (still) 0f else offset * 0.35f
            alpha = if (still) 1f else (1f - offset / heightPx).coerceIn(0f, 1f)
        })
    }
}
