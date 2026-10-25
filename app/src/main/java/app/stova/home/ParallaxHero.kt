package app.stova.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.stova.core.theme.LocalReduceMotion
import app.stova.media.SpotlightEntry

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
