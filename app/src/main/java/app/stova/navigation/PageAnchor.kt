package app.stova.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

internal val LocalPageOrigin = staticCompositionLocalOf<(TransformOrigin) -> Unit> { {} }

internal fun pageOrigin(bounds: Rect, root: IntSize): TransformOrigin {
    if (root.width <= 0 || root.height <= 0) return TransformOrigin.Center
    return TransformOrigin((bounds.center.x / root.width).coerceIn(0f, 1f),
        (bounds.center.y / root.height).coerceIn(0f, 1f))
}

/** Read the trigger's current position on activation, after any scroll and focus motion. */
internal class PageAnchor(private val publish: (TransformOrigin) -> Unit) {
    private var coordinates: LayoutCoordinates? = null
    val modifier = Modifier.onGloballyPositioned { coordinates = it }

    fun open(navigate: () -> Unit) {
        val source = coordinates?.takeIf { it.isAttached }
        publish(source?.let { pageOrigin(it.boundsInRoot(), it.findRootCoordinates().size) } ?: TransformOrigin.Center)
        navigate()
    }
}

@Composable
internal fun rememberPageAnchor(onOrigin: (TransformOrigin) -> Unit = LocalPageOrigin.current): PageAnchor {
    val publish = rememberUpdatedState(onOrigin)
    return remember { PageAnchor { publish.value(it) } }
}
