package app.stova.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaMotion
import app.stova.core.theme.stovaMotion

/**
 * Rows that arrive together rise 10 dp and fade in, each [StovaMotion.StaggerMillis] after the one
 * above, for the first [StovaMotion.StaggerRows]. Without [enabled], and under reduced motion,
 * they are simply there. Layer phase only, so focus and layout are ready from the first frame.
 */
@Composable
fun Modifier.staggeredEntrance(order: Int, enabled: Boolean): Modifier {
    val motion = stovaMotion
    val progress = remember { Animatable(if (enabled && !motion.still) 0f else 1f) }
    LaunchedEffect(progress) {
        if (progress.value < 1f) progress.animateTo(1f, motion.enter(delayMillis = order.coerceIn(0, StovaMotion.StaggerRows) * StovaMotion.StaggerMillis))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 10.dp.toPx()
    }
}
