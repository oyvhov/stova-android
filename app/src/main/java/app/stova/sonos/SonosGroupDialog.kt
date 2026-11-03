package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*

/** A local draft: choosing rooms never starts or moves their playback before Apply. */
@Composable
fun SonosGroupDialog(anchor: SonosRoom, rooms: List<SonosRoom>, busy: Boolean, failed: Boolean,
    onApply: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    val initial = remember(anchor.id) { anchor.members.toSet() }
    var selected by remember(anchor.id) { mutableStateOf(initial) }
    val changedOutside = initial != anchor.members.toSet()
    val first = remember { FocusRequester() }
    StovaDialog(onDismiss) {
        Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 460.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.sonos_group), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.sonos_group_hint, anchor.name), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(4.dp)) {
                items(rooms, key = { it.id }) { room ->
                    // Each row shows that room's own speaker, still: the dialog is a quiet draft.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SonosSpeakerArt(room.speaker, null, Modifier.size(48.dp, 44.dp), "sonos-speaker-${room.id}", room.available)
                        val model = room.speaker.label
                        if (room.id == anchor.id) Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(stringResource(R.string.sonos_group_anchor, room.name), style = MaterialTheme.typography.titleMedium)
                            model?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        else if (!room.available) Text(stringResource(R.string.sonos_room_unavailable, room.name), Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else SettingsSwitchRow(room.name, room.id in selected, { checked ->
                            if (!busy) selected = if (checked) selected + room.id else selected - room.id
                        }, subtitle = listOfNotNull(model, stringResource(R.string.sonos_move_group)
                            .takeIf { room.coordinatorId != anchor.id && room.members.size > 1 }).joinToString(" · ").ifEmpty { null },
                            modifier = Modifier.weight(1f))
                    }
                }
            }
            if (changedOutside || failed) Text(stringResource(if (changedOutside) R.string.sonos_group_changed else R.string.sonos_action_failed), color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.sonos_cancel), onDismiss, Modifier.focusRequester(first))
                StovaButton(stringResource(R.string.sonos_apply), { onApply(selected) }, primary = true,
                    enabled = !busy && !changedOutside && groupPlan(anchor, rooms, selected) != null && selected != initial)
            }
        }
    }
    LaunchedEffect(Unit) { first.requestFocus() }
}
