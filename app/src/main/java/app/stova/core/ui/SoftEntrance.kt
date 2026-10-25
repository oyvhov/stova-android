package app.stova.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.stova.core.theme.stovaMotion

/** Only the arriving controls exist; their layer settles without an outgoing focus tree. */
@Composable
fun Modifier.softEntrance(key: Any?): Modifier {
    val motion = stovaMotion
    val progress = remember(key) { Animatable(if (motion.still) 1f else 0f) }
    LaunchedEffect(progress, motion) {
        if (motion.still) progress.snapTo(1f) else progress.animateTo(1f, motion.enter())
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}
