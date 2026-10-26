package app.stova.core.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.zIndex
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.LocalStrongFocus
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.stovaMotion
import app.stova.home.LocalRowHasFocus
import app.stova.profiles.FocusEffect
import app.stova.profiles.resolved
import app.stova.core.theme.StovaFocus

/** Shared focus for artwork and controls: calm lift and surface light by default.
 * Animation state is read in drawing/layer phases; reduced motion keeps the surface cue still.
 */
@Composable
fun Modifier.focusOutline(source: InteractionSource, shape: Shape, zoom: Boolean = false, dimSurfaceAlpha: Float = 1f): Modifier {
    val focused by source.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current
    val motion = stovaMotion
    val effect = LocalAppearance.current.focusEffect.resolved()
    val dim = zoom && (effect == FocusEffect.LIGHT_UP || reduceMotion) && LocalRowHasFocus.current && !focused
    val brightness = animateFloatAsState(if (dim) 0.72f else 1f, motion.fade(motion.focusRing), label = "focus-brightness")
    val showRing = LocalStrongFocus.current || effect == FocusEffect.OUTLINE || effect == FocusEffect.RING_ZOOM
    val alpha = animateFloatAsState(
        targetValue = if (focused && showRing) 1f else 0f,
        animationSpec = motion.fade(motion.focusRing),
        label = "focus-outline",
    )
    val scale = animateFloatAsState(
        if (!focused || reduceMotion) 1f else when (effect) {
            FocusEffect.LIFT -> if (zoom) StovaFocus.ArtworkScale else StovaFocus.ControlScale
            FocusEffect.ZOOM, FocusEffect.RING_ZOOM -> if (zoom) 1.05f else 1.02f
            else -> 1f
        },
        motion.fade(motion.focusZoom), label = "focus-scale")
    val emphasis = animateFloatAsState(if (focused && !showRing) 1f else 0f, motion.fade(motion.focusRing), label = "focus-surface")
    val lift = animateFloatAsState(if (focused && !reduceMotion && effect == FocusEffect.LIFT) 1f else 0f,
        motion.fade(motion.focusZoom), label = "focus-lift")
    val color = MaterialTheme.colorScheme.primary
    val light = MaterialTheme.colorScheme.onSurface
    val ink = MaterialTheme.colorScheme.background
    val width = if (LocalStrongFocus.current) 4.dp else LocalAppearance.current.ringWidth.dp.dp
    return zIndex(if (focused) 1f else 0f).graphicsLayer {
        scaleX = scale.value; scaleY = scale.value
        translationY = -StovaFocus.Lift.toPx() * lift.value
        this.shape = shape
    }.drawWithCache {
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
            // Artwork and icon-pack colours stay exact; only control surfaces receive the light wash.
            if (!zoom && emphasis.value > 0.001f) drawOutline(artOutline, SolidColor(light.copy(alpha = StovaFocus.SurfaceAlpha * emphasis.value)))
            // Dim the art itself, keeping it opaque over the wallpaper.
            if (brightness.value < 0.999f) drawOutline(artOutline, SolidColor(ink.copy(alpha = (1f - brightness.value) * dimSurfaceAlpha)))
            val a = alpha.value
            if (a > 0.001f) {
                inset(edge) { drawOutline(outline, SolidColor(color.copy(alpha = a)), style = style) }
            }
        }
    }
}
