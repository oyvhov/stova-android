package app.stova.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.stova.core.theme.stovaMotion

/**
 * Every dialog and centred menu in Stova. The scrim and the content fade in while the panel settles
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
    StovaOverlay(onDismissRequest, properties, Alignment.Center,
        enter = scaleIn(motion.enter(), initialScale = PANEL_FROM),
        exit = scaleOut(motion.exit(), targetScale = PANEL_TO)) { content() }
}

/**
 * The long-press panel: it slides in from the right edge over a dimmed screen and fills the
 * height. Back first asks [onBack], which returns true when it went back a page inside the panel;
 * otherwise the panel slides out and then reports [onDismissRequest]. Same key guard as
 * [StovaDialog]: the held OK that opened it chooses nothing.
 */
@Composable
internal fun StovaSheetWindow(
    onDismissRequest: () -> Unit,
    onBack: () -> Boolean,
    content: @Composable () -> Unit,
) {
    val motion = stovaMotion
    StovaOverlay(onDismissRequest, DialogProperties(), Alignment.CenterEnd,
        enter = slideInHorizontally(motion.enter()) { it } + fadeIn(motion.enter()),
        exit = slideOutHorizontally(motion.exit()) { it } + fadeOut(motion.exit()),
        onBack = onBack, sideScrim = true) { content() }
}

@Composable
private fun StovaOverlay(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    alignment: Alignment,
    enter: EnterTransition,
    exit: ExitTransition,
    onBack: () -> Boolean = { false },
    sideScrim: Boolean = false,
    content: @Composable () -> Unit,
) {
    val motion = stovaMotion
    val still = motion.still
    val visible = remember { MutableTransitionState(still).apply { targetState = true } }
    val dismiss by rememberUpdatedState(onDismissRequest)
    val back by rememberUpdatedState(onBack)
    val exited = visible.isIdle && !visible.currentState
    LaunchedEffect(exited) { if (exited) dismiss() }
    fun close() { if (still) dismiss() else visible.targetState = false }
    Dialog(
        onDismissRequest = { if (!back()) close() },
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            // The scrim is drawn here, so the window always spans the screen.
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = properties.decorFitsSystemWindows,
        ),
    ) {
        AnimatedVisibility(visible, enter = fadeIn(motion.enter()), exit = fadeOut(motion.exit()), label = "dialog") {
            Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                // A panel with pages answers Back itself, one page at a time, before the window sees it.
                if (sideScrim && event.key in backKeys) {
                    if (event.type == KeyEventType.KeyUp && !back()) close()
                    true
                } else false
            }.freshPressesOnly(), contentAlignment = alignment) {
                val shade = MaterialTheme.colorScheme.background
                // A side panel leaves the screen readable on the far side and darkens towards itself.
                val scrim = if (sideScrim) Modifier.background(Brush.horizontalGradient(
                    listOf(shade.copy(alpha = SIDE_SCRIM_FAR), shade.copy(alpha = SIDE_SCRIM_NEAR))))
                else Modifier.background(shade.copy(alpha = SCRIM_ALPHA))
                Box(Modifier.matchParentSize().then(scrim)
                    .pointerInput(still, properties.dismissOnClickOutside) {
                        detectTapGestures { if (properties.dismissOnClickOutside) close() }
                    })
                Box(
                    Modifier.pointerInput(Unit) { detectTapGestures { } }
                        .animateEnterExit(enter = enter, exit = exit, label = "panel"),
                ) { content() }
            }
        }
    }
}

/** The platform's own dialog dim, kept so the picture behind a dialog looks as it always did. */
private const val SCRIM_ALPHA = 0.6f
private const val SIDE_SCRIM_FAR = 0.35f
private const val SIDE_SCRIM_NEAR = 0.8f
private const val PANEL_FROM = 0.985f
private const val PANEL_TO = 0.98f
private val backKeys = setOf(Key.Back, Key.Escape)
