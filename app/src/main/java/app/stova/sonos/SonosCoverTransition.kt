package app.stova.sonos

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.stova.core.theme.stovaMotion
import app.stova.ha.cards.HaImage
import app.stova.ha.cards.HaImageSource

internal const val SONOS_WIDGET_FOCUS = "sonos-widget"
internal val LocalSonosCoverModifier = compositionLocalOf<@Composable (String) -> Modifier> { { Modifier } }
internal val LocalSonosCovers = compositionLocalOf<SonosCovers?> { null }

@Composable
internal fun SharedTransitionScope.sonosCoverMotion(path: String, visibility: AnimatedVisibilityScope): Modifier {
    val motion = stovaMotion
    return if (motion.still) Modifier else Modifier.sharedElement(rememberSharedContentState(path), visibility,
        boundsTransform = { _, _ -> motion.enter() })
}

/** A decoded sleeve must be available to the incoming page before its first frame. */
internal class SonosCovers {
    private val images = mutableStateMapOf<String, ImageBitmap>()
    private val loadOrder = ArrayDeque<String>()

    operator fun get(path: String?): ImageBitmap? = path?.let(images::get)

    fun store(path: String, image: ImageBitmap) {
        loadOrder.remove(path)
        loadOrder.addLast(path)
        images[path] = image
        if (loadOrder.size > 3) images.remove(loadOrder.removeFirst())
    }
}

@Composable
internal fun SonosCoverImage(path: String, source: HaImageSource?, modifier: Modifier, ready: () -> Unit, failed: () -> Unit) {
    val covers = LocalSonosCovers.current
    val image = covers?.get(path)
    if (image != null) {
        Image(image, null, modifier, contentScale = ContentScale.Crop)
        SideEffect { ready() }
    } else {
        HaImage(path, source, modifier, contentScale = ContentScale.Crop, onLoad = ready, onError = failed,
            artworkSize = 1280, onBitmap = { covers?.store(path, it) })
    }
}
