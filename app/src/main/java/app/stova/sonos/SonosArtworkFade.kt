package app.stova.sonos

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import app.stova.core.theme.stovaMotion

internal typealias SonosArtLoader = @Composable (String, Modifier, () -> Unit, () -> Unit) -> Unit

/** The old image stays opaque until the new one has decoded and covered it. */
@Composable
internal fun SonosArtworkFade(path: String?, backdrop: Boolean, sharedCover: Boolean = false, load: SonosArtLoader) {
    if (path == null) return
    var settled by remember { mutableStateOf<String?>(null) }
    var ready by remember(path) { mutableStateOf(false) }
    val cached = sharedCover && LocalSonosCovers.current?.get(path) != null
    val opacity = remember(path) { Animatable(if (settled == null && cached) 1f else 0f) }
    val motion = stovaMotion
    LaunchedEffect(path, ready, motion) {
        if (ready && settled != path) {
            opacity.animateTo(1f, motion.fade(if (backdrop) motion.ambient else motion.standard))
            settled = path
        }
    }
    Box(Modifier.fillMaxSize()) {
        for (layer in listOfNotNull(settled?.takeUnless { it == path }, path)) key(layer) {
            val shared = if (sharedCover && (layer == settled || ready || cached)) {
                LocalSonosCoverModifier.current(layer).clip(MaterialTheme.shapes.small)
            } else Modifier
            Box(Modifier.fillMaxSize().then(shared).graphicsLayer {
                alpha = if (layer == settled || (ready && motion.still)) 1f else opacity.value
            }) {
                load(layer, Modifier.fillMaxSize(),
                    { if (layer == path) ready = true },
                    { if (layer == path && settled != path) ready = false })
            }
        }
    }
}
