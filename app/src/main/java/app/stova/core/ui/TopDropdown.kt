package app.stova.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.stova.core.theme.StovaDropdown
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion

/**
 * The small card that drops from the top bar: the Settings icon's menu and the smart-home chips
 * share it. A little see-through and never blurred, it grows from below its anchor, stays inside
 * the screen's safe edge and closes on Back. [gap] is the space under the anchor; [alignEnd] lines
 * its right edge up with the anchor's, else its left edge.
 */
@Composable
fun TopDropdown(onDismiss: () -> Unit, width: Dp, gap: Dp = 10.dp, alignEnd: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    val position = remember(density, gap, alignEnd) {
        with(density) { BelowAnchor(gap.roundToPx(), StovaLayout.TvSafeEdge.roundToPx(), alignEnd) }
    }
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        val motion = stovaMotion
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(shown, enter = fadeIn(motion.enter()) + scaleIn(motion.enter(), initialScale = .92f,
            transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0f))) {
            Column(Modifier.width(width).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = StovaDropdown.SurfaceAlpha))
                .onKeyEvent { event ->
                    if (event.key != Key.Back && event.key != Key.Escape) false
                    else { if (event.type == KeyEventType.KeyUp) onDismiss(); true }
                }
                // Holding OK may have opened it; that hold's repeats and release must not choose anything.
                .freshPressesOnly()
                .padding(vertical = 10.dp), content = content)
        }
    }
}

/** Under the anchor, moved in from the screen's edge when it would cross it. */
private class BelowAnchor(private val gap: Int, private val edge: Int, private val alignEnd: Boolean) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = if (alignEnd) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val last = (windowSize.width - edge - popupContentSize.width).coerceAtLeast(edge)
        return IntOffset(x.coerceIn(edge, last), anchorBounds.bottom + gap)
    }
}
