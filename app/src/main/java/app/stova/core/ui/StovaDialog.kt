package app.stova.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.stova.core.theme.stovaMotion

/**
 * Every dialog, menu and panel in Stova. The scrim and the content fade in while the panel settles
 * from 0.985, the exit is quicker, and under reduced motion it simply appears and disappears.
 * Back and a touch outside still close it: the request plays the exit first and reaches
 * [onDismissRequest] when the panel has gone. Focus goes into the content as before, because
 * the content is composed from the first frame.
 *
 * The window itself neither dims nor animates (`Theme.Stova.Dialog`), so this layer is the only
 * one, and it obeys the motion tokens.
 */
@Composable
fun StovaDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val motion = stovaMotion
    val still = motion.still
    val visible = remember { MutableTransitionState(still).apply { targetState = true } }
    val dismiss by rememberUpdatedState(onDismissRequest)
    val confirmPresses = remember { mutableSetOf<Key>() }
    val exited = visible.isIdle && !visible.currentState
    LaunchedEffect(exited) { if (exited) dismiss() }
    Dialog(
        onDismissRequest = { if (still) dismiss() else visible.targetState = false },
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            // The scrim is drawn here, so the window always spans the screen.
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = properties.decorFitsSystemWindows,
        ),
    ) {
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(windowFocused) { if (!windowFocused) confirmPresses.clear() }
        AnimatedVisibility(visible, enter = fadeIn(motion.enter()), exit = fadeOut(motion.exit()), label = "dialog") {
            Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.key !in confirmKeys) false else when (event.type) {
                    KeyEventType.KeyDown -> {
                        // The opening hold may keep repeating after focus enters this window.
                        // Only a fresh down here owns the subsequent release and its action.
                        if (event.nativeKeyEvent.repeatCount == 0 && !event.nativeKeyEvent.isLongPress) confirmPresses.add(event.key)
                        event.key !in confirmPresses
                    }
                    KeyEventType.KeyUp -> !confirmPresses.remove(event.key) || event.nativeKeyEvent.isCanceled
                    else -> false
                }
            }, contentAlignment = Alignment.Center) {
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.background.copy(alpha = SCRIM_ALPHA))
                    .pointerInput(still, properties.dismissOnClickOutside) {
                        detectTapGestures { if (properties.dismissOnClickOutside) {
                            if (still) dismiss() else visible.targetState = false
                        } }
                    })
                Box(
                    Modifier.pointerInput(Unit) { detectTapGestures { } }.animateEnterExit(
                        enter = scaleIn(motion.enter(), initialScale = PANEL_FROM),
                        exit = scaleOut(motion.exit(), targetScale = PANEL_TO),
                        label = "panel",
                    ),
                ) { content() }
            }
        }
    }
}

/** The platform's own dialog dim, kept so the picture behind a dialog looks as it always did. */
private const val SCRIM_ALPHA = 0.6f
private const val PANEL_FROM = 0.985f
private const val PANEL_TO = 0.98f
private val confirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
