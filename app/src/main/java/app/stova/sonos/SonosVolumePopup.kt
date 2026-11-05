package app.stova.sonos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.confirmKeys
import app.stova.core.ui.freshPressesOnly

@Composable
internal fun SonosVolumePopup(room: SonosRoom, volume: Int?, busy: Boolean, onAction: (SonosAction) -> Unit,
    button: FocusRequester, modifier: Modifier = Modifier) {
    val enabled = volume != null && room.available && room.supports(4)
    val label = stringResource(R.string.sonos_volume_room, room.name)
    val draft = rememberSonosVolumeDraft(room.id, volume ?: 0, enabled, busy, onAction)
    val slider = remember { FocusRequester() }
    val motion = stovaMotion
    var open by remember(room.id) { mutableStateOf(false) }
    var restoreFocus by remember(room.id) { mutableStateOf(false) }
    val visible = remember(room.id) { MutableTransitionState(false) }
    SideEffect { visible.targetState = open && enabled }
    fun close() {
        if (!open) return
        draft.flush()
        open = false
        restoreFocus = true
    }
    val exited = visible.isIdle && !visible.currentState
    LaunchedEffect(open, enabled, exited) {
        if (!enabled && open) {
            open = false
            restoreFocus = false
        } else if (!open && restoreFocus) {
            withFrameNanos { }
            if (button.requestFocus() || exited || motion.still) restoreFocus = false
        }
    }
    Box {
        val value = stringResource(R.string.ha_percent, draft.value)
        val speakerHeight = sonosStageHeight(room.speaker.shape) * 1.25f
        // The popup's anchor includes the room name, so the slider opens below both.
        Column(Modifier.width(160.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SonosIconButton(StovaIcons.Speaker, label, { open = true }, modifier.height(speakerHeight + 24.dp).focusRequester(button)
                .testTag("sonos-volume-button").semantics { if (volume != null) stateDescription = value },
                enabled = enabled, size = 128.dp, quiet = true,
                symbol = { SonosSpeakerArt(room.speaker, null, Modifier.size(112.dp, speakerHeight),
                    "sonos-volume-speaker", available = room.available) })
            Text(room.displayName, Modifier.background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (enabled && (open || !motion.still && (visible.currentState || !visible.isIdle))) {
            val density = LocalDensity.current
            val gap = with(density) { 8.dp.roundToPx() }
            val edge = with(density) { StovaLayout.TvSafeEdge.roundToPx() }
            val position = remember(gap, edge) { VolumeBelowButton(gap, edge) }
            Popup(popupPositionProvider = position, onDismissRequest = ::close,
                properties = PopupProperties(focusable = open, dismissOnClickOutside = open)) {
                AnimatedVisibility(visible,
                    enter = if (motion.still) EnterTransition.None else fadeIn(motion.enter()) + slideInVertically(motion.enter()) { -gap },
                    exit = if (motion.still) ExitTransition.None else fadeOut(motion.exit()) + slideOutVertically(motion.exit()) { -gap },
                    label = "sonos-volume-panel") {
                    Box(Modifier.testTag("sonos-volume-panel").background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                        .freshPressesOnly().onPreviewKeyEvent { event ->
                            if (!open || event.key !in confirmKeys && event.key != Key.Back && event.key != Key.Escape) false
                            else { if (event.type == KeyEventType.KeyUp) close(); true }
                        }.padding(8.dp)) {
                        SonosVolumeSlider(room.id, draft, enabled, busy, label, Modifier.focusRequester(slider)
                            .focusProperties { canFocus = open }.then(if (open) Modifier else Modifier.semantics { hideFromAccessibility() }), vertical = true)
                    }
                    LaunchedEffect(open) { if (open) { withFrameNanos { }; slider.requestFocus() } }
                }
            }
        }
    }
}

private class VolumeBelowButton(private val gap: Int, private val edge: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
        popupContentSize: IntSize): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        return IntOffset(
            x.coerceIn(edge, (windowSize.width - edge - popupContentSize.width).coerceAtLeast(edge)),
            (anchorBounds.bottom + gap).coerceAtMost((windowSize.height - edge - popupContentSize.height).coerceAtLeast(edge)),
        )
    }
}
