package app.stova.sonos

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.*

/** Two independent D-pad targets: the music opens its page; the small button controls playback. */
@Composable fun SonosNowPlaying(room: SonosRoom, player: SonosRoom, busy: Boolean, failed: Boolean,
    onOpen: () -> Unit, onPlayPause: () -> Unit, modifier: Modifier = Modifier,
    artwork: @Composable (String, Modifier) -> Unit = { _, _ -> }) {
    val source = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val tv = room.tvAudio || player.tvAudio
    if (tv) return
    val shape = MaterialTheme.shapes.medium
    val title = player.title ?: stringResource(R.string.sonos_playing)
    val subtitle = if (failed) stringResource(R.string.sonos_action_failed) else
        listOfNotNull(room.name, if (!player.playing) stringResource(R.string.sonos_paused) else player.artist).joinToString(" · ")
    Row(modifier.widthIn(max = 280.dp).background(MaterialTheme.colorScheme.surface, shape), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).focusOutline(source, shape).clickable(source, null, role = Role.Button, onClick = onOpen)
            .padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(34.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(StovaIcons.Play, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Crossfade(player.artwork, animationSpec = stovaMotion.fade(stovaMotion.ambient), label = "sonos-chip-art") { path ->
                    if (path != null) artwork(path, Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.size(44.dp).focusOutline(playSource, shape).clickable(playSource, null,
            enabled = !busy && room.available && player.available && player.supports(if (player.playing) 1 else 16384),
            role = Role.Button, onClick = onPlayPause), contentAlignment = Alignment.Center) {
            Icon(if (player.playing) StovaIcons.Pause else StovaIcons.Play,
                stringResource(if (player.playing) R.string.ha_pause else R.string.ha_play), Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
