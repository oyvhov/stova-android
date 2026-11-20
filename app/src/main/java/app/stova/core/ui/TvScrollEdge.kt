package app.stova.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaLayout

private val EdgeFadeHeight = 8.dp

/** Fade at the physical bottom by default. Home can place the seam at its chosen row inset. */
internal fun Modifier.bottomFade(margin: Dp = EdgeFadeHeight, enabled: () -> Boolean = { true }) = drawWithCache {
    val top = (size.height - margin.toPx()).coerceAtLeast(0f)
    val seam = minOf(EdgeFadeHeight.toPx(), size.height - top)
    val layer = Rect(0f, top, size.width, top + seam)
    val paint = Paint()
    val mask = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = top, endY = top + seam)
    onDrawWithContent {
        if (!enabled() || seam == 0f) {
            drawContent()
        } else {
            clipRect(bottom = top) { this@onDrawWithContent.drawContent() }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(layer, paint)
                clipRect(top = top, bottom = layer.bottom) { this@onDrawWithContent.drawContent() }
                drawRect(mask, topLeft = Offset(0f, top), size = Size(size.width, seam), blendMode = BlendMode.DstOut)
                canvas.restore()
            }
        }
    }
}

/** Keep the focused control above the fade, using the caller's existing scroll policy. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberTvScrollSpec(base: BringIntoViewSpec): BringIntoViewSpec {
    val inset = with(LocalDensity.current) { StovaLayout.TvSafeEdge.toPx() }
    return remember(base, inset) {
        object : BringIntoViewSpec by base {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                base.calculateScrollDistance(offset, size, (containerSize - inset).coerceAtLeast(0f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvScrollColumn(
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!scrollable) {
        Column(modifier, verticalArrangement = verticalArrangement, content = content)
        return
    }
    val scroll = rememberScrollState()
    val original = LocalBringIntoViewSpec.current
    CompositionLocalProvider(LocalBringIntoViewSpec provides rememberTvScrollSpec(original)) {
        Column(modifier.bottomFade { scroll.canScrollForward }
            .verticalScroll(scroll).padding(bottom = StovaLayout.TvSafeEdge), verticalArrangement = verticalArrangement) {
            // A horizontal row within the page still uses its own full width for focus scrolling.
            CompositionLocalProvider(LocalBringIntoViewSpec provides original) { content() }
        }
    }
}
