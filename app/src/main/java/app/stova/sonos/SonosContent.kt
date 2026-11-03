package app.stova.sonos

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.*
import app.stova.ha.cards.PanelSlider
import app.stova.ha.cards.rememberResumedNow

/** A controllable TV surface: no account, connection, network or storage work. */
@Composable
fun SonosContent(state: SonosState, selectedId: String?, onSelect: (String) -> Unit, onAction: (SonosAction) -> Unit,
    onGroup: () -> Unit, onBrowse: () -> Unit, onRetry: () -> Unit,
    busy: Boolean = false, actionFailed: Boolean = false, backgroundArt: Boolean = true,
    artwork: @Composable (String, Modifier) -> Unit = { _, _ -> }, onBack: () -> Unit = {}, onBrowseMusicAssistant: (() -> Unit)? = null) {
    val room = state.rooms.find { it.id == selectedId } ?: state.rooms.firstOrNull()
    val native = room?.let { coordinator(it, state.rooms) }
    val player = room?.let { sonosPlaybackRoom(it, state) }
    val colors = MaterialTheme.colorScheme
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        // The square album fills the canvas too. Dark matte ink preserves contrast; no blur.
        key(backgroundArt, native?.tvAudio == true) { Crossfade(if (backgroundArt && native?.tvAudio != true) player?.artwork else null, animationSpec = stovaMotion.fade(stovaMotion.ambient), label = "sonos-art") { path ->
            if (path != null) artwork(path, Modifier.fillMaxSize())
        } }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.background.copy(alpha = .97f), colors.background.copy(alpha = .82f), colors.background.copy(alpha = .72f)))))
        Column(Modifier.fillMaxSize().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(StovaIcons.Sonos, null, Modifier.size(28.dp), tint = colors.onSurface)
                Text(stringResource(R.string.sonos_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                StovaButton(stringResource(R.string.sonos_back), onBack, compact = true, quiet = true)
            }
            if (room == null) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = colors.onSurfaceVariant)
                    Text(stringResource(when { state.loading -> R.string.sonos_loading; state.failed -> R.string.sonos_connection_failed; else -> R.string.sonos_empty }),
                        style = MaterialTheme.typography.titleLarge)
                    if (!state.loading) StovaButton(stringResource(R.string.custom_retry), onRetry, Modifier.focusRequester(first))
                }
            } else Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                LazyColumn(Modifier.width(174.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(4.dp)) {
                    item { Text(stringResource(R.string.sonos_rooms), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                    items(state.rooms, key = { it.id }) { item ->
                        SonosRoomButton(item, item.id == room.id, { onSelect(item.id) },
                            if (item.id == room.id) Modifier.focusRequester(first) else Modifier)
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(4.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    val largeType = LocalDensity.current.fontScale >= 1.5f
                    if (largeType) {
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true }, Modifier.size(180.dp), artwork)
                        SonosPlayback(room, player, !busy, onAction)
                    } else Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true }, Modifier.width(220.dp).aspectRatio(1f), artwork)
                        Column(Modifier.weight(1f)) { SonosPlayback(room, player, !busy, onAction) }
                    }
                    if (actionFailed) Text(stringResource(R.string.sonos_action_failed), color = colors.error)
                    if (native != null && native.members.size > 1) Text(native.members.mapNotNull { id -> state.rooms.find { it.id == id }?.name }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StovaButton(stringResource(R.string.sonos_library), onBrowse, icon = StovaIcons.Rows, compact = true, quiet = true,
                            enabled = !busy && native?.available == true && native.supports(131072))
                        if (onBrowseMusicAssistant != null) StovaButton(stringResource(R.string.sonos_ma), onBrowseMusicAssistant,
                            icon = StovaIcons.Play, enabled = !busy, compact = true, quiet = true)
                        StovaButton(stringResource(R.string.sonos_group), onGroup, icon = StovaIcons.Link, compact = true, quiet = true,
                            enabled = !busy && native?.available == true && native.supports(524288))
                    }
                    Text(stringResource(R.string.sonos_volume_room, room.name), style = MaterialTheme.typography.titleSmall)
                    room.volume?.let { volume ->
                        SonosVolume(room.id, volume, room.available && !busy && room.supports(4), onAction)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (room.supports(8)) StovaButton(stringResource(if (room.muted) R.string.sonos_unmute else R.string.sonos_mute),
                            { onAction(SonosAction.Mute(!room.muted)) }, enabled = room.available && !busy, compact = true)
                        if (player != null) {
                            if (player.supports(32768)) StovaButton(stringResource(if (player.shuffle) R.string.sonos_shuffle_on else R.string.sonos_shuffle_off),
                                { onAction(SonosAction.Shuffle(!player.shuffle)) }, primary = player.shuffle, enabled = player.available && !busy, compact = true)
                            if (player.supports(262144)) StovaButton(stringResource(when (player.repeat) { "one" -> R.string.sonos_repeat_one; "all" -> R.string.sonos_repeat_all; else -> R.string.sonos_repeat_off }),
                                { onAction(SonosAction.Repeat(when (player.repeat) { "off" -> "all"; "all" -> "one"; else -> "off" })) }, enabled = player.available && !busy, compact = true)
                        }
                    }
                    if (native?.supports(2048) == true && native.sources.isNotEmpty()) {
                        Text(stringResource(R.string.ha_media_source), style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            native.sources.forEach { source -> StovaButton(source, { onAction(SonosAction.Source(source)) },
                                primary = source == native.source, compact = true, enabled = native.available && !busy) }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(state.loading, state.rooms.isEmpty()) {
        if (!state.loading) { withFrameNanos { }; first.requestFocus() }
    }
}

@Composable
private fun SonosRoomButton(room: SonosRoom, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().testTag("sonos-room-${room.id}").focusOutline(source, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium).background(if (active) colors.surfaceVariant else colors.surface)
        .semantics { selected = active }.clickable(source, null, role = Role.Tab, onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(room.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (room.playing && !room.tvAudio) Icon(StovaIcons.Play, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
        }
        Text(stringResource(when { !room.available -> R.string.sonos_unavailable; room.tvAudio -> R.string.sonos_tv; room.members.size > 1 -> R.string.sonos_grouped; room.playing -> R.string.sonos_playing; else -> R.string.sonos_ready }),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable internal fun SonosCover(path: String?, modifier: Modifier, artwork: @Composable (String, Modifier) -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (path != null) artwork(path, Modifier.fillMaxSize())
    }
}

@Composable private fun SonosPlayback(room: SonosRoom, player: SonosRoom?, enabled: Boolean, onAction: (SonosAction) -> Unit) {
    val ready = enabled && room.available && player?.available == true
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (player?.playing == true && !player.tvAudio) stringResource(R.string.sonos_playing_room, room.name) else stringResource(R.string.sonos_now),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (player?.tvAudio == true) stringResource(R.string.sonos_tv) else player?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.sonos_no_track),
            style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        player?.artist?.takeUnless { player.tvAudio }?.let { Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        player?.album?.takeUnless { player.tvAudio }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        if (!room.available || player?.available != true) Text(stringResource(R.string.sonos_unavailable))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StovaButton(stringResource(R.string.ha_action_previous), { onAction(SonosAction.Previous) }, enabled = ready && player?.supports(16) == true, compact = true, quiet = true)
            StovaButton(stringResource(if (player?.playing == true) R.string.ha_pause else R.string.ha_play), { onAction(SonosAction.PlayPause) },
                primary = player?.playing != true, icon = if (player?.playing == true) StovaIcons.Pause else StovaIcons.Play,
                enabled = ready && player?.supports(if (player.playing) 1 else 16384) == true, compact = true)
            StovaButton(stringResource(R.string.ha_v2_next), { onAction(SonosAction.Next) }, enabled = ready && player?.supports(32) == true, compact = true, quiet = true)
        }
        if (player != null && player.duration > 0) SonosProgress(player, ready, onAction)
    }
}

@Composable private fun SonosProgress(player: SonosRoom, enabled: Boolean, onAction: (SonosAction) -> Unit) {
    val now by rememberResumedNow(1000, player.playing)
    val position = player.position(now)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(progress = { position.toFloat() / player.duration }, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            trackColor = MaterialTheme.colorScheme.outlineVariant, drawStopIndicator = {})
        Text(stringResource(R.string.sonos_progress, mediaTime(position), mediaTime(player.duration)), style = MaterialTheme.typography.bodySmall)
        if (player.supports(2)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StovaButton(stringResource(R.string.sonos_seek_back), { onAction(SonosAction.Seek(position - 15)) }, compact = true, quiet = true, enabled = enabled)
            StovaButton(stringResource(R.string.sonos_seek_forward), { onAction(SonosAction.Seek(position + 15)) }, compact = true, quiet = true, enabled = enabled)
        }
    }
}
private fun mediaTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

/** Coalesce held D-pad changes. A track event must not snap a slider back while it is being edited. */
@Composable internal fun SonosVolume(id: String, volume: Int, enabled: Boolean, onAction: (SonosAction) -> Unit) {
    var draft by remember(id) { mutableIntStateOf(volume) }
    var editing by remember(id) { mutableStateOf(false) }
    val send by rememberUpdatedState(onAction)
    LaunchedEffect(volume, editing) { if (!editing) draft = volume }
    LaunchedEffect(draft, editing) {
        if (editing) { kotlinx.coroutines.delay(250); send(SonosAction.Volume(draft)); kotlinx.coroutines.delay(1500); editing = false }
    }
    PanelSlider("sonos-$id", stringResource(R.string.ha_action_volume), draft.toDouble(), stringResource(R.string.ha_percent, draft),
        0.0..100.0, enabled, step = 2.0) { draft = it.toInt(); editing = true }
}
