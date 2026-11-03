package app.stova.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

internal val LocalPageOpenContext = staticCompositionLocalOf<PageOpenContext?> { null }
internal val LocalPageReturnFocus = staticCompositionLocalOf<String?> { null }

internal fun pageOrigin(bounds: Rect, root: IntSize): TransformOrigin {
    if (root.width <= 0 || root.height <= 0) return TransformOrigin.Center
    return TransformOrigin((bounds.center.x / root.width).coerceIn(0f, 1f),
        (bounds.center.y / root.height).coerceIn(0f, 1f))
}

/** Read the trigger's current position on activation, after any scroll and focus motion. */
internal class PageAnchor(private val focus: FocusRequester, private val openFrom: (TransformOrigin, () -> Unit) -> Unit) {
    private var coordinates: LayoutCoordinates? = null
    val modifier = Modifier.onGloballyPositioned { coordinates = it }.focusRequester(focus)

    fun open(navigate: () -> Unit) {
        val source = coordinates?.takeIf { it.isAttached }
        openFrom(source?.let { pageOrigin(it.boundsInRoot(), it.findRootCoordinates().size) } ?: TransformOrigin.Center, navigate)
    }
}

@Composable
internal fun rememberPageAnchor(key: String, onOrigin: (TransformOrigin) -> Unit = {}): PageAnchor {
    val context = rememberUpdatedState(LocalPageOpenContext.current)
    val publish = rememberUpdatedState(onOrigin)
    val returnKey = LocalPageReturnFocus.current
    val focus = remember(key) { FocusRequester() }
    LaunchedEffect(returnKey, key) {
        if (returnKey == key) {
            // Let the pane's usual first focus settle before restoring the opener.
            repeat(2) { withFrameNanos { } }
            repeat(6) {
                if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
                withFrameNanos { }
            }
        }
    }
    return remember(key, focus) { PageAnchor(focus) { origin, navigate ->
        publish.value(origin)
        context.value?.from(PageOpening(key, origin), navigate) ?: navigate()
    } }
}
