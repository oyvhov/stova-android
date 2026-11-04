package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaActionDialog
import app.stova.ha.cards.rememberResumedNow

/** Secondary controls keep the main player within one TV viewport. Opening does nothing. */
@Composable internal fun SonosOptionsDialog(room: SonosRoom, native: SonosRoom?, player: SonosRoom?, busy: Boolean, failed: Boolean,
    onAction: (SonosAction) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    StovaActionDialog(onDismiss) { close ->
        Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 460.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SonosLabels(stringResource(R.string.sonos_more), room.displayName, MaterialTheme.typography.headlineSmall, Modifier.weight(1f))
                StovaButton(stringResource(R.string.ha_close), close, Modifier.focusRequester(first), compact = true, quiet = true)
            }
            if (failed) Text(stringResource(R.string.sonos_action_failed), color = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (room.supports(8)) StovaButton(stringResource(if (room.muted) R.string.sonos_unmute else R.string.sonos_mute),
                        { onAction(SonosAction.Mute(!room.muted)) }, enabled = room.available, compact = true)
                    if (player != null) {
                        if (player.supports(32768)) StovaButton(stringResource(if (player.shuffle) R.string.sonos_shuffle_on else R.string.sonos_shuffle_off),
                            { onAction(SonosAction.Shuffle(!player.shuffle)) }, primary = player.shuffle, enabled = player.available, compact = true)
                        if (player.supports(262144)) StovaButton(stringResource(when (player.repeat) { "one" -> R.string.sonos_repeat_one; "all" -> R.string.sonos_repeat_all; else -> R.string.sonos_repeat_off }),
                            { onAction(SonosAction.Repeat(when (player.repeat) { "off" -> "all"; "all" -> "one"; else -> "off" })) }, enabled = player.available, compact = true)
                    }
                }
                if (player != null && player.duration > 0 && player.supports(2)) {
                    val now by rememberResumedNow(1000, player.playing)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StovaButton(stringResource(R.string.sonos_seek_back), { onAction(SonosAction.Seek(player.position(now) - 15)) }, compact = true, enabled = player.available)
                        StovaButton(stringResource(R.string.sonos_seek_forward), { onAction(SonosAction.Seek(player.position(now) + 15)) }, compact = true, enabled = player.available)
                    }
                }
                if (native?.supports(2048) == true && native.sources.isNotEmpty()) {
                    Text(stringResource(R.string.ha_media_source), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        native.sources.forEach { source -> StovaButton(source, { onAction(SonosAction.Source(source)) },
                            primary = source == native.source, compact = true, enabled = native.available) }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { first.requestFocus() }
}
