package app.stova.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.stova.core.theme.stovaMotion
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

private data class StepPosition(val key: Any?, val depth: Int)
private class PreviousStep(var position: StepPosition)

/** Moving one level keeps the surrounding frame still and the old focus tree out of the way. */
@Composable
internal fun Modifier.stepEntrance(key: Any?, depth: Int): Modifier {
    val motion = stovaMotion
    val layoutDirection = LocalLayoutDirection.current
    val position = StepPosition(key, depth)
    val previous = remember { PreviousStep(position) }
    val direction = remember(position) { depth.compareTo(previous.position.depth) }
    val progress = remember(position) { Animatable(if (motion.still || previous.position == position) 1f else 0f) }
    SideEffect { previous.position = position }
    LaunchedEffect(progress, motion) {
        if (motion.still) progress.snapTo(1f) else if (progress.value < 1f) progress.animateTo(1f, motion.enter())
    }
    if (motion.still) return this
    return graphicsLayer {
        val remaining = 1f - progress.value
        val start = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
        alpha = progress.value
        translationX = start * direction * remaining * 16.dp.toPx()
    }
}

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
