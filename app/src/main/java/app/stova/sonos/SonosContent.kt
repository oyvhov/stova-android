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
import androidx.compose.ui.draw.drawBehind
import androidx.activity.compose.BackHandler
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
    roomsOpen: Boolean = true, onRoomsOpen: (Boolean) -> Unit = {},
    /** The music alone on the screen; hoisted in [SonosScreen]. Back returns to this page. */
    immersive: Boolean = false, onImmersive: (Boolean) -> Unit = {}) {
    val room = state.rooms.find { it.id == selectedId } ?: state.rooms.firstOrNull()
    val native = room?.let { coordinator(it, state.rooms) }
    val player = room?.let { sonosPlaybackRoom(it, state) }
    val colors = MaterialTheme.colorScheme
    val first = remember { FocusRequester() }
    val playbackFocus = remember { FocusRequester() }
    val moreFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val fullscreenFocus = remember { FocusRequester() }
    val immersiveNow by rememberUpdatedState(immersive)
    var focusFullscreen by remember { mutableStateOf(false) }
    fun leaveImmersive() { focusFullscreen = true; onImmersive(false) }
    LaunchedEffect(immersive) {
        if (!immersive && focusFullscreen) { withFrameNanos { }; fullscreenFocus.requestFocus(); focusFullscreen = false }
    }
    // Set by the side menu's button: where focus goes once the list has appeared or gone.
    var focusAfterToggle by remember { mutableStateOf<Boolean?>(null) }
    // Availability and support decide what is enabled. A pending action (busy) never disables the
    // focused control, or focus would jump to a neighbour on the TV; repeats are ignored instead.
    val guarded: (SonosAction) -> Unit = { action -> if (!busy) onAction(action) }
    val canPlay = room?.available == true && player?.available == true && player.supports(if (player.playing) 1 else 16384)
    val rooms = if (roomsOpen) first else FocusRequester.Default
    var more by remember(room?.id) { mutableStateOf(false) }
    var restoreMore by remember { mutableStateOf(false) }
    val playbackAvailable = room?.available == true && player?.available == true &&
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
    // Keep the same loaded backdrop while the controls change viewing mode. Remounting the
    // authenticated image here makes a visible empty frame even when the sleeve is cached.
    Box(Modifier.fillMaxSize().background(colors.background)) {
    val tvAudio = native?.tvAudio == true || room?.tvAudio == true || player?.tvAudio == true
    val shownArt = player?.artwork?.takeIf { backgroundArt && !tvAudio && room?.available == true && player.available }
    key(backgroundArt, tvAudio, shownArt == null) {
        Crossfade(shownArt, animationSpec = stovaMotion.fade(stovaMotion.ambient), label = "sonos-art") { path ->
            if (path != null) artwork(path, Modifier.fillMaxSize())
        }
    }
    if (immersive && room != null) {
        BackHandler { leaveImmersive() }
        SonosModeReveal("immersive") {
            SonosImmersive(room, native, player, busy, actionFailed, backgroundArt, artwork, guarded, ::leaveImmersive)
        }
    } else SonosModeReveal("page") {
    Box(Modifier.fillMaxSize()) {
        // Dark matte ink preserves contrast; no blur.
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
                if (room != null) SonosIconButton(StovaIcons.Expand, stringResource(R.string.sonos_fullscreen), { onImmersive(true) },
                    Modifier.focusRequester(fullscreenFocus).testTag("sonos-fullscreen"))
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
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true || room.tvAudio }, Modifier.size(160.dp), artwork)
                        SonosPlayback(room, player, guarded, playbackFocus, rooms, playFocus)
                    } else Row(Modifier.heightIn(min = cover + 32.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        SonosCover(player?.artwork.takeUnless { native?.tvAudio == true || room.tvAudio }, Modifier.size(cover), artwork)
                        Column(Modifier.weight(1f)) { SonosPlayback(room, player, guarded, playbackFocus, rooms, playFocus) }
                    }
                    if (actionFailed) Text(stringResource(R.string.sonos_action_failed), color = colors.error)
                    // The room's own speaker beside its volume; no words on screen, the rail speaks to TalkBack.
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SonosSpeakerArt(room.speaker, rememberSonosVisual(room, state), Modifier.width(132.dp).height(56.dp),
                            "sonos-stage-${room.id}", room.available, waves = true)
                        room.volume?.let { volume ->
                            SonosVolumeRail(room.id, volume, room.available && room.supports(4), busy,
                                stringResource(R.string.sonos_volume_room, room.name), onAction, Modifier.weight(1f))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StovaButton(stringResource(R.string.sonos_library), onBrowse, Modifier.focusProperties { left = rooms },
                            icon = StovaIcons.Rows, compact = true, quiet = true,
                            enabled = native?.available == true && native.supports(131072))
                        StovaButton(stringResource(R.string.sonos_group), onGroup, icon = StovaIcons.Link, compact = true, quiet = true,
                            enabled = native?.available == true && native.supports(524288))
                        StovaButton(stringResource(R.string.sonos_more), { restoreMore = true; more = true }, Modifier.focusRequester(moreFocus),
                            compact = true, quiet = true)
                        if (onBrowseMusicAssistant != null) StovaButton(stringResource(R.string.sonos_ma), onBrowseMusicAssistant,
                            compact = true, quiet = true)
                    }
                }
            }
        }
        SeasonalScene(Modifier.matchParentSize(), front = true, clock = seasonClock)
    }
    }
    }
    if (more && room != null && !immersive) SonosOptionsDialog(room, native, player, busy, actionFailed, guarded) { more = false }
    LaunchedEffect(state.loading, state.rooms.isEmpty()) {
        if (!state.loading) { withFrameNanos { }
            // Restoring full screen, or entering it during this frame, removes the page controls.
            // The immersive view requests its own focus; never target the detached page here.
            if (!immersiveNow) {
                if (roomsOpen || room == null) first.requestFocus() else if (canPlay) playFocus.requestFocus() else moreFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun SonosRoomButton(room: SonosRoom, visual: SonosRoomVisual, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    // The chosen room's plate eases in; read only in the draw phase.
    val plate = androidx.compose.animation.animateColorAsState(if (active) colors.surfaceVariant else colors.surface,
        stovaMotion.fade(stovaMotion.quick), label = "sonos-room")
    Row(modifier.fillMaxWidth().testTag("sonos-room-${room.id}").focusOutline(source, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium).drawBehind { drawRect(plate.value) }
        .semantics { selected = active }.clickable(source, null, role = Role.Tab, onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // The speaker's light already says whether music plays; no separate play glyph.
        SonosSpeakerArt(room.speaker, visual, Modifier.size(52.dp, 48.dp), "sonos-speaker-${room.id}", room.available)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Status, and the model when HA knows it: the one place on the page that names the speaker.
            Text(listOfNotNull(stringResource(when { !room.available -> R.string.sonos_unavailable; room.tvAudio -> R.string.sonos_tv; room.members.size > 1 -> R.string.sonos_grouped; visual.playing -> R.string.sonos_playing; else -> R.string.sonos_ready }),
                room.speaker.label).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable internal fun SonosCover(path: String?, modifier: Modifier, artwork: @Composable (String, Modifier) -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        // A new sleeve crossfades over the old; losing the sleeve (TV, silence) removes it at once.
        key(path == null) { Crossfade(path, animationSpec = stovaMotion.fade(stovaMotion.standard), label = "sonos-cover") { shown ->
            if (shown != null) artwork(shown, Modifier.fillMaxSize())
        } }
    }
}

/** Page and full screen arrive with a short fade and settle in the layer phase; no size animates. */
@Composable private fun SonosModeReveal(mode: String, content: @Composable () -> Unit) {
    val motion = stovaMotion
    val reveal = remember(mode) { androidx.compose.animation.core.Animatable(if (motion.still) 1f else 0f) }
    LaunchedEffect(reveal) { reveal.animateTo(1f, motion.enter()) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal.value; val s = .985f + .015f * reveal.value; scaleX = s; scaleY = s }) { content() }
}

/** Status, title and artist; a new track crossfades in. TV audio says so and shows no stale track. */
@Composable internal fun SonosTrackText(room: SonosRoom, player: SonosRoom?, tv: Boolean, largeType: Boolean, immersive: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!immersive) Text(if (player?.playing == true && !tv) stringResource(R.string.sonos_playing_room, room.name) else stringResource(R.string.sonos_now),
            style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Crossfade(Triple(if (tv) null else player?.title, if (tv) null else player?.artist, if (tv) null else player?.album),
            animationSpec = stovaMotion.fade(stovaMotion.standard), label = "sonos-track") { (title, artist, album) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (tv) stringResource(R.string.sonos_tv) else title?.takeIf(String::isNotBlank) ?: stringResource(R.string.sonos_no_track),
                    style = if (immersive && !largeType) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                    maxLines = if (largeType) 1 else 2, overflow = TextOverflow.Ellipsis)
                artist?.let { Text(it, style = if (immersive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = if (immersive) colors.onSurfaceVariant else colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (!immersive) album?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        if (!room.available || player?.available != true) Text(stringResource(R.string.sonos_unavailable))
    }
}

/** The room list arrives with a short fade and slide in the layer phase; its width never animates. */
@Composable private fun SonosRoomsPanel(content: @Composable () -> Unit) {
    val motion = stovaMotion
    val reveal = remember { androidx.compose.animation.core.Animatable(if (motion.still) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, motion.enter()) }
    Box(Modifier.fillMaxHeight().graphicsLayer { alpha = reveal.value; translationX = -24.dp.toPx() * (1f - reveal.value) }) { content() }
}

@Composable private fun SonosPlayback(room: SonosRoom, player: SonosRoom?, onAction: (SonosAction) -> Unit, entry: FocusRequester,
    rooms: FocusRequester, playFocus: FocusRequester) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SonosTrackText(room, player, player?.tvAudio == true || room.tvAudio, LocalDensity.current.fontScale >= 1.5f)
        SonosTransport(room, player, onAction, entry, rooms, playFocus)
        // TV audio keeps the last track's timing in HA; never show it as music.
        if (player?.tvAudio != true && !room.tvAudio && player != null && player.duration > 0) SonosProgress(player)
    }
}

@Composable internal fun SonosProgress(player: SonosRoom) {
    val now by rememberResumedNow(1000, player.playing)
    val position = player.position(now)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(progress = { position.toFloat() / player.duration }, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            trackColor = MaterialTheme.colorScheme.outlineVariant, drawStopIndicator = {})
        Text(stringResource(R.string.sonos_progress, mediaTime(position), mediaTime(player.duration)), style = MaterialTheme.typography.bodySmall)

    }
}
private fun mediaTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
