package app.stova.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.LocalStrongFocus
import app.stova.core.theme.StovaLayout

/**
 * Keyboard and remote focus, drawn and nothing else: a 3 dp line (4 dp with strong focus) in the
 * text colour, inside the element's own shape. No scale, no glow, no shadow and no change of size,
 * so a focused card never pushes its neighbours. Only the draw phase reads the fade.
 */
@Composable
fun Modifier.focusOutline(source: InteractionSource, shape: Shape): Modifier {
    val focused by source.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current
    val alpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else StovaLayout.FocusFadeMillis),
        label = "focus-outline",
    )
    val color = MaterialTheme.colorScheme.onSurface
    val width = if (LocalStrongFocus.current) 4.dp else 3.dp
    return drawWithCache {
        val stroke = width.toPx()
        val edge = stroke / 2
        val outline = shape.createOutline(
            Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
            layoutDirection,
            this,
        )
        val style = Stroke(stroke)
        onDrawWithContent {
            drawContent()
            val a = alpha.value
            if (a > 0.001f) {
                inset(edge) { drawOutline(outline, SolidColor(color.copy(alpha = a)), style = style) }
            }
        }
    }
}
