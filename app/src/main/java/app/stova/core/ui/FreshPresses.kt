package app.stova.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo

/** OK, Enter and the keypad's Enter: the keys that choose. */
internal val confirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * For a window opened by holding OK, such as a dialog, a side panel or the Settings icon's menu:
 * the opening hold may keep repeating after focus enters the window. Only a fresh press inside it
 * owns the release that follows, and with it the action.
 */
@Composable
internal fun Modifier.freshPressesOnly(): Modifier {
    val presses = remember { mutableSetOf<Key>() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) { if (!windowFocused) presses.clear() }
    return onPreviewKeyEvent { event ->
        if (event.key !in confirmKeys) false else when (event.type) {
            KeyEventType.KeyDown -> {
                if (event.nativeKeyEvent.repeatCount == 0 && !event.nativeKeyEvent.isLongPress) presses.add(event.key)
                event.key !in presses
            }
            KeyEventType.KeyUp -> !presses.remove(event.key) || event.nativeKeyEvent.isCanceled
            else -> false
        }
    }
}
