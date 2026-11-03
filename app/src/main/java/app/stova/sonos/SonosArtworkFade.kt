package app.stova.sonos

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import app.stova.core.theme.stovaMotion

internal typealias SonosArtLoader = @Composable (String, Modifier, () -> Unit, () -> Unit) -> Unit

/** The old image stays opaque until the new one has decoded and covered it. */
@Composable
internal fun SonosArtworkFade(path: String?, backdrop: Boolean, load: SonosArtLoader) {
    if (path == null) return
    var settled by remember { mutableStateOf<String?>(null) }
    var ready by remember(path) { mutableStateOf(false) }
    val opacity = remember(path) { Animatable(0f) }
    val motion = stovaMotion
    LaunchedEffect(path, ready, motion) {
        if (ready && settled != path) {
            opacity.animateTo(1f, motion.fade(if (backdrop) motion.ambient else motion.standard))
            settled = path
        }
    }
    Box(Modifier.fillMaxSize()) {
        for (layer in listOfNotNull(settled?.takeUnless { it == path }, path)) key(layer) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = if (layer == settled || (ready && motion.still)) 1f else opacity.value
            }) {
                load(layer, Modifier.fillMaxSize(),
                    { if (layer == path) ready = true },
                    { if (layer == path) settled = null })
            }
        }
    }
}
