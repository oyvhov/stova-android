package app.stova.kids

import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.viewinterop.AndroidView
import app.stova.core.theme.KidsColors

/** Pip's view, as wide as it is tall times this; the screen sizes and places him by it. */
fun pipAspect(threeD: Boolean): Float = if (threeD) PIP3D_ASPECT else PIP_ASPECT
/** The line his feet stand on, as a share of his view's height. */
fun pipFeet(threeD: Boolean): Float = if (threeD) PIP3D_FEET else PIP_FEET

/** Whether this TV draws Pip in 3D; decided once, before anything is drawn. */
@Composable
fun rememberPip3d(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) { PipScene.available(context) }
}

/**
 * Pip as the TV can show him best: in real 3D where the renderer loads, drawn flat otherwise
 * (and in unit tests). Either way [motion] poses him; the 3D view draws only on his frames.
 */
@Composable
fun PipFigure(mood: PipMood, outfit: PipOutfit, motion: PipMotion, part: DayPart, threeD: Boolean, modifier: Modifier = Modifier) {
    var failed by remember { mutableStateOf(!threeD) }
    if (failed) { Pip(mood, outfit, motion, modifier); return }
    var scene by remember { mutableStateOf<PipScene?>(null) }
    val looks by rememberUpdatedState(Triple(mood, outfit, part))
    Box(modifier) {
        // His shadow on the card, under the 3D view, smaller while he is up in the air.
        Canvas(Modifier.fillMaxSize()) {
            val spread = 1f - 0.45f * motion.air
            val w = size.width * 0.42f * spread
            drawOval(KidsColors.Shadow.copy(alpha = 0.28f * (1f - 0.5f * motion.air)),
                Offset(size.width / 2 - w / 2, size.height * PIP3D_FEET - size.height * 0.018f), Size(w, size.height * 0.036f))
        }
        AndroidView(factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                scene = runCatching { PipScene(context, this) }.onFailure { failed = true }.getOrNull()
            }
        }, Modifier.fillMaxSize())
    }
    DisposableEffect(Unit) { onDispose { motion.onFrame = null; scene?.destroy(); scene = null } }
    SideEffect { motion.onFrame = { t -> scene?.render(motion, looks.first, looks.second, looks.third, t) } }
    LaunchedEffect(mood, outfit, part, scene) { motion.poke() }
}
