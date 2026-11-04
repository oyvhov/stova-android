package app.stova.sonos

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline

/**
 * A round icon control: the icon says what it does, TalkBack hears [description]. Pressing dips it
 * slightly in the layer phase; [emphasized] gives play/pause a soft disc so it leads without shouting.
 */
@Composable internal fun SonosIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, size: Dp = 48.dp, emphasized: Boolean = false, quiet: Boolean = false,
    symbol: (@Composable () -> Unit)? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = stovaMotion
    val press = animateFloatAsState(if (pressed && !motion.still) .92f else 1f, motion.settle(), label = "sonos-press")
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(size).focusOutline(source, CircleShape).graphicsLayer { scaleX = press.value; scaleY = press.value }
        .clip(CircleShape).then(if (emphasized) Modifier.background(colors.onSurface.copy(alpha = .14f)) else Modifier)
        .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        val tint = if (!enabled) colors.onSurfaceVariant.copy(alpha = .45f) else if (quiet) colors.onSurfaceVariant else colors.onSurface
        // Play turning into pause (and back) crossfades instead of jumping.
        if (symbol != null) symbol() else Crossfade(icon, animationSpec = motion.fade(motion.quick), label = "sonos-icon") { shown ->
            Icon(shown, null, Modifier.size(size * .42f), tint = tint)
        }
    }
}

/**
 * Previous, play/pause and next. Support and availability decide what is enabled; a pending
 * action never disables the focused one (the caller ignores repeats). [entry] lands on the first
 * usable control, whose Left goes to [left].
 */
@Composable internal fun SonosTransport(room: SonosRoom, player: SonosRoom?, onAction: (SonosAction) -> Unit, entry: FocusRequester,
    left: FocusRequester, playFocus: FocusRequester, large: Boolean = false, modifier: Modifier = Modifier,
    volumeFocus: FocusRequester = FocusRequester.Default) {
    val ready = room.available && player?.available == true
    val previous = ready && player?.supports(16) == true
    val play = ready && player?.supports(if (player.playing) 1 else 16384) == true
    val next = ready && player?.supports(32) == true
    val playing = player?.playing == true
    fun entryModifier(first: Boolean) = (if (first) Modifier.focusRequester(entry).focusProperties { this.left = left } else Modifier)
        .focusProperties { up = volumeFocus }
    val small = if (large) 52.dp else 48.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (large) 14.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SonosIconButton(StovaIcons.Previous, stringResource(R.string.ha_action_previous), { onAction(SonosAction.Previous) },
            entryModifier(previous).testTag("sonos-previous"), previous, small, quiet = large)
        SonosIconButton(if (playing) StovaIcons.Pause else StovaIcons.Play, stringResource(if (playing) R.string.ha_pause else R.string.ha_play),
            { onAction(SonosAction.PlayPause) }, entryModifier(!previous && play).focusRequester(playFocus).testTag("sonos-play"), play,
            if (large) 68.dp else 58.dp, emphasized = true)
        SonosIconButton(StovaIcons.Next, stringResource(R.string.ha_v2_next), { onAction(SonosAction.Next) },
            entryModifier(!previous && !play && next).testTag("sonos-next"), next, small, quiet = large)
    }
}
