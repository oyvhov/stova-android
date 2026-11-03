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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import app.stova.home.SeasonalScene
import app.stova.home.rememberSeasonalClock

/** A controllable TV surface: no account, connection, network or storage work. */
@Composable
fun SonosContent(state: SonosState, selectedId: String?, onSelect: (String) -> Unit, onAction: (SonosAction) -> Unit,
    onGroup: () -> Unit, onBrowse: () -> Unit, onRetry: () -> Unit,
    busy: Boolean = false, actionFailed: Boolean = false, backgroundArt: Boolean = true,
    artwork: @Composable (String, Modifier) -> Unit = { _, _ -> }, onBack: () -> Unit = {}, onBrowseMusicAssistant: (() -> Unit)? = null,
    /** The rooms are a side menu: hidden, the music takes the page. Hoisted so it survives the library. */
    roomsOpen: Boolean = true, onRoomsOpen: (Boolean) -> Unit = {}) {
    val room = state.rooms.find { it.id == selectedId } ?: state.rooms.firstOrNull()
    val native = room?.let { coordinator(it, state.rooms) }
    val player = room?.let { sonosPlaybackRoom(it, state) }
    val colors = MaterialTheme.colorScheme
    val first = remember { FocusRequester() }
    val playbackFocus = remember { FocusRequester() }
    val moreFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    // Set by the side menu's button: where focus goes once the list has appeared or gone.
    var focusAfterToggle by remember { mutableStateOf<Boolean?>(null) }
    val canPlay = !busy && room?.available == true && player?.available == true && player.supports(if (player.playing) 1 else 16384)
    val rooms = if (roomsOpen) first else FocusRequester.Default
    var more by remember(room?.id) { mutableStateOf(false) }
    var restoreMore by remember { mutableStateOf(false) }
    val playbackAvailable = !busy && room?.available == true && player?.available == true &&
        (player.supports(16) || player.supports(if (player.playing) 1 else 16384) || player.supports(32))
    LaunchedEffect(roomsOpen) {
        val opened = focusAfterToggle ?: return@LaunchedEffect
        withFrameNanos { }
        // Opened: the chosen room. Hidden: what plays now, or the next control that is always there.
        if (opened) first.requestFocus() else if (canPlay) playFocus.requestFocus() else moreFocus.requestFocus()
        focusAfterToggle = null
    }
    LaunchedEffect(more) {
        if (!more && restoreMore) { withFrameNanos { }; moreFocus.requestFocus(); restoreMore = false }
    }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        // The square album fills the canvas too. Dark matte ink preserves contrast; no blur.
        key(backgroundArt, native?.tvAudio == true) { Crossfade(if (backgroundArt && native?.tvAudio != true) player?.artwork else null, animationSpec = stovaMotion.fade(stovaMotion.ambient), label = "sonos-art") { path ->
            if (path != null) artwork(path, Modifier.fillMaxSize())
        } }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.background.copy(alpha = .97f), colors.background.copy(alpha = .82f), colors.background.copy(alpha = .72f)))))
        val seasonClock = rememberSeasonalClock()
        SeasonalScene(Modifier.matchParentSize(), clock = seasonClock)
        Column(Modifier.fillMaxSize().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(StovaIcons.Sonos, null, Modifier.size(28.dp), tint = colors.onSurface)
                Text(stringResource(R.string.sonos_title), style = MaterialTheme.typography.headlineMedium)
                SonosSeasonMark()
                Spacer(Modifier.weight(1f))
                if (room != null) StovaButton(if (roomsOpen) stringResource(R.string.sonos_rooms_hide) else stringResource(R.string.sonos_rooms_show, room.name),
                    { focusAfterToggle = !roomsOpen; onRoomsOpen(!roomsOpen) }, Modifier.testTag("sonos-rooms-toggle"),
                    icon = StovaIcons.Rows, compact = true, quiet = true)
                StovaButton(stringResource(R.string.sonos_back), onBack, compact = true, quiet = true)
            }
            if (room == null) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = colors.onSurfaceVariant)
                    Text(stringResource(when { state.loading -> R.string.sonos_loading; state.failed -> R.string.sonos_connection_failed; else -> R.string.sonos_empty }),
                        style = MaterialTheme.typography.titleLarge)
                    if (!state.loading) StovaButton(stringResource(R.string.custom_retry), onRetry, Modifier.focusRequester(first))
                }
            } else Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (roomsOpen) SonosRoomsPanel {
                LazyColumn(Modifier.width(220.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(4.dp)) {
                    item { Text(stringResource(R.string.sonos_rooms), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                    items(state.rooms, key = { it.id }) { item ->
                        SonosRoomButton(item, rememberSonosVisual(item, state), item.id == room.id, { onSelect(item.id) },
                            (if (item.id == room.id) Modifier.focusRequester(first) else Modifier)
                                .focusProperties { right = if (playbackAvailable) playbackFocus else moreFocus })
                    }
                }
                }
                // Ordinary TV type fits on one screen. Large accessibility text may scroll;
                // it must never be shrunk to preserve the normal-density layout.
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val largeType = LocalDensity.current.fontScale >= 1.5f
                    // With the rooms hidden the freed width goes to the sleeve and the music, not to more controls.
                    val cover = if (roomsOpen) 190.dp else 236.dp
                    if (largeType) {
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true }, Modifier.size(160.dp), artwork)
                        SonosPlayback(room, player, !busy, onAction, playbackFocus, rooms, playFocus)
                    } else Row(Modifier.heightIn(min = cover + 32.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true }, Modifier.size(cover), artwork)
                        Column(Modifier.weight(1f)) { SonosPlayback(room, player, !busy, onAction, playbackFocus, rooms, playFocus) }
                    }
                    if (actionFailed) Text(stringResource(R.string.sonos_action_failed), color = colors.error)
                    // A stable speaker/volume row stays in the same place for solo and grouped rooms.
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SonosSpeakerArt(room.speaker, rememberSonosVisual(room, state), Modifier.fillMaxWidth().height(64.dp),
                                "sonos-stage-${room.id}", room.available, waves = true)
                            room.speaker.label?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.sonos_volume_room, room.name), style = MaterialTheme.typography.titleSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            room.volume?.let { volume ->
                                SonosVolume(room.id, volume, room.available && !busy && room.supports(4), onAction)
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StovaButton(stringResource(R.string.sonos_library), onBrowse, Modifier.focusProperties { left = rooms },
                            icon = StovaIcons.Rows, compact = true, quiet = true,
                            enabled = !busy && native?.available == true && native.supports(131072))
                        StovaButton(stringResource(R.string.sonos_group), onGroup, icon = StovaIcons.Link, compact = true, quiet = true,
                            enabled = !busy && native?.available == true && native.supports(524288))
                        StovaButton(stringResource(R.string.sonos_more), { restoreMore = true; more = true }, Modifier.focusRequester(moreFocus),
                            compact = true, quiet = true)
                        if (onBrowseMusicAssistant != null) StovaButton(stringResource(R.string.sonos_ma), onBrowseMusicAssistant,
                            enabled = !busy, compact = true, quiet = true)
                    }
                }
            }
        }
        SeasonalScene(Modifier.matchParentSize(), front = true, clock = seasonClock)
    }
    if (more && room != null) SonosOptionsDialog(room, native, player, busy, actionFailed, onAction) { more = false }
    LaunchedEffect(state.loading, state.rooms.isEmpty()) {
        if (!state.loading) { withFrameNanos { }
            if (roomsOpen || room == null) first.requestFocus() else if (canPlay) playFocus.requestFocus() else moreFocus.requestFocus() }
    }
}

@Composable
private fun SonosRoomButton(room: SonosRoom, visual: SonosRoomVisual, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().testTag("sonos-room-${room.id}").focusOutline(source, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium).background(if (active) colors.surfaceVariant else colors.surface)
        .semantics { selected = active }.clickable(source, null, role = Role.Tab, onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // The speaker's light already says whether music plays; no separate play glyph.
        SonosSpeakerArt(room.speaker, visual, Modifier.size(52.dp, 48.dp), "sonos-speaker-${room.id}", room.available)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(when { !room.available -> R.string.sonos_unavailable; room.tvAudio -> R.string.sonos_tv; room.members.size > 1 -> R.string.sonos_grouped; visual.playing -> R.string.sonos_playing; else -> R.string.sonos_ready }),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable internal fun SonosCover(path: String?, modifier: Modifier, artwork: @Composable (String, Modifier) -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (path != null) artwork(path, Modifier.fillMaxSize())
    }
}

/** The room list arrives with a short fade and slide in the layer phase; its width never animates. */
@Composable private fun SonosRoomsPanel(content: @Composable () -> Unit) {
    val motion = stovaMotion
    val reveal = remember { androidx.compose.animation.core.Animatable(if (motion.still) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, motion.enter()) }
    Box(Modifier.fillMaxHeight().graphicsLayer { alpha = reveal.value; translationX = -24.dp.toPx() * (1f - reveal.value) }) { content() }
}

@Composable private fun SonosPlayback(room: SonosRoom, player: SonosRoom?, enabled: Boolean, onAction: (SonosAction) -> Unit, entry: FocusRequester,
    rooms: FocusRequester, playFocus: FocusRequester) {
    val ready = enabled && room.available && player?.available == true
    val previous = ready && player?.supports(16) == true
    val play = ready && player?.supports(if (player.playing) 1 else 16384) == true
    val next = ready && player?.supports(32) == true
    fun entryModifier(first: Boolean) = if (first) Modifier.focusRequester(entry).focusProperties { left = rooms } else Modifier
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (player?.playing == true && !player.tvAudio) stringResource(R.string.sonos_playing_room, room.name) else stringResource(R.string.sonos_now),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (player?.tvAudio == true) stringResource(R.string.sonos_tv) else player?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.sonos_no_track),
            style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        player?.artist?.takeUnless { player.tvAudio }?.let { Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        player?.album?.takeUnless { player.tvAudio }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (!room.available || player?.available != true) Text(stringResource(R.string.sonos_unavailable))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StovaButton(stringResource(R.string.ha_action_previous), { onAction(SonosAction.Previous) }, modifier = entryModifier(previous), enabled = previous, compact = true, quiet = true)
            StovaButton(stringResource(if (player?.playing == true) R.string.ha_pause else R.string.ha_play), { onAction(SonosAction.PlayPause) },
                modifier = entryModifier(!previous && play).focusRequester(playFocus), primary = player?.playing != true, icon = if (player?.playing == true) StovaIcons.Pause else StovaIcons.Play,
                enabled = play, compact = true)
            StovaButton(stringResource(R.string.ha_v2_next), { onAction(SonosAction.Next) }, modifier = entryModifier(!previous && !play && next), enabled = next, compact = true, quiet = true)
        }
        if (player != null && player.duration > 0) SonosProgress(player)
    }
}

@Composable private fun SonosProgress(player: SonosRoom) {
    val now by rememberResumedNow(1000, player.playing)
    val position = player.position(now)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(progress = { position.toFloat() / player.duration }, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            trackColor = MaterialTheme.colorScheme.outlineVariant, drawStopIndicator = {})
        Text(stringResource(R.string.sonos_progress, mediaTime(position), mediaTime(player.duration)), style = MaterialTheme.typography.bodySmall)

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
