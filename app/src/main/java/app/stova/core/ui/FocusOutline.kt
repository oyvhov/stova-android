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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.LocalStrongFocus
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.stovaMotion
import app.stova.home.LocalRowHasFocus
import app.stova.profiles.FocusEffect

/** An accent ring inside the artwork. Optional zoom changes only drawing, never row layout.
 * Animation state is read in drawing/layer phases; reduced motion applies the ring immediately.
 */
@Composable
fun Modifier.focusOutline(source: InteractionSource, shape: Shape, zoom: Boolean = false): Modifier {
    val focused by source.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current
    val motion = stovaMotion
    val effect = LocalAppearance.current.focusEffect
    val dim = zoom && effect == FocusEffect.LIGHT_UP && LocalRowHasFocus.current && !focused
    val brightness = animateFloatAsState(if (dim) 0.72f else 1f, motion.fade(motion.focusRing), label = "focus-brightness")
    // Controls always retain their ring. Artwork uses the selected effect.
    val showRing = !zoom || effect == FocusEffect.RING || effect == FocusEffect.RING_ZOOM || (reduceMotion && effect == FocusEffect.ZOOM)
    val alpha = animateFloatAsState(
        targetValue = if (focused && showRing) 1f else 0f,
        animationSpec = motion.fade(motion.focusRing),
        label = "focus-outline",
    )
    val scale = animateFloatAsState(
        if (zoom && focused && !reduceMotion && (effect == FocusEffect.ZOOM || effect == FocusEffect.RING_ZOOM)) 1.05f else 1f,
        motion.fade(motion.focusZoom), label = "focus-scale")
    val color = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.background
    val width = if (LocalStrongFocus.current) 4.dp else LocalAppearance.current.ringWidth.dp.dp
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }.drawWithCache {
        val artOutline = shape.createOutline(size, layoutDirection, this)
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
            // Dim the art itself, keeping it opaque over the wallpaper.
            if (brightness.value < 0.999f) drawOutline(artOutline, SolidColor(ink.copy(alpha = 1f - brightness.value)))
            val a = alpha.value
            if (a > 0.001f) {
                inset(edge) { drawOutline(outline, SolidColor(color.copy(alpha = a)), style = style) }
            }
        }
    }
}
