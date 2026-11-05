package app.stova.sonos

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.*
import app.stova.ha.cards.rememberResumedNow
import app.stova.home.SeasonalScene
import app.stova.home.rememberSeasonalClock
import kotlin.math.sin

/** A controllable TV surface: no account, connection, network or storage work. */
@Composable
fun SonosContent(state: SonosState, selectedId: String?, onSelect: (String) -> Unit, onAction: (SonosAction) -> Unit,
    onGroup: () -> Unit, onBrowse: () -> Unit, onRetry: () -> Unit,
    busy: Boolean = false, actionFailed: Boolean = false, backgroundArt: Boolean = true,
    artwork: @Composable (String, Modifier) -> Unit = { _, _ -> }, onBack: () -> Unit = {}, onBrowseMusicAssistant: (() -> Unit)? = null,
    /** The rooms are a side menu: hidden, the music takes the page. Hoisted so it survives the library. */
    roomsOpen: Boolean = true, onRoomsOpen: (Boolean) -> Unit = {},
    /** The music alone on the screen; hoisted in [SonosScreen]. Back returns to this page. */
    immersive: Boolean = false, onImmersive: (Boolean) -> Unit = {},
    preparedArtwork: String? = null, loadArtwork: SonosArtLoader? = null,
    shelf: List<SonosMedia> = emptyList(), shelfThumbnail: SonosThumbnail = { _, _, _, _ -> },
    shelfReturn: SonosMedia? = null, onShelfRestored: () -> Unit = {}, onBrowseMedia: (SonosMedia) -> Unit = {},
    onApplyGroup: ((SonosGroupDraft, (Boolean) -> Unit) -> Unit)? = null) {
    val room = state.rooms.find { it.id == selectedId } ?: state.rooms.firstOrNull()
    val native = room?.let { coordinator(it, state.rooms) }
    val player = room?.let { sonosPlaybackRoom(it, state) }
    val colors = MaterialTheme.colorScheme
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val first = remember { FocusRequester() }
    val playbackFocus = remember { FocusRequester() }
    val moreFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val volumeFocus = remember { FocusRequester() }
    val fullscreenFocus = remember { FocusRequester() }
    val groupAnchorFocus = remember { FocusRequester() }
    val groupCancelFocus = remember { FocusRequester() }
    val groupApplyFocus = remember { FocusRequester() }
    val roomList = rememberLazyListState()
    val playerScroll = rememberScrollState()
    var draft by remember { mutableStateOf<SonosGroupDraft?>(null) }
    var groupAttempted by remember { mutableStateOf(false) }
    var restoreRoomAfterGroup by remember { mutableStateOf(false) }
    val anchor = draft?.let { edit -> state.rooms.find { it.id == edit.anchorId } }
    val canApply = draft?.let { edit -> anchor?.let { edit.canApply(it, state.rooms) } } == true
    val wobble = remember { androidx.compose.animation.core.Animatable(1f) }
    val motion = stovaMotion
    fun beginGroup(candidate: SonosRoom) {
        val leader = coordinator(candidate, state.rooms)?.takeIf { it.available && it.supports(524288) } ?: return
        if (busy || draft != null) return
        groupAttempted = false
        draft = SonosGroupDraft(leader.id, leader.members.toSet())
        if (!roomsOpen) onRoomsOpen(true)
    }
    fun endGroup() { if (!busy) { draft = null; restoreRoomAfterGroup = true } }
    BackHandler(enabled = draft != null) { endGroup() }
    LaunchedEffect(draft?.anchorId, roomsOpen) {
        val target = draft?.anchorId ?: room?.id?.takeIf { restoreRoomAfterGroup } ?: return@LaunchedEffect
        if (!roomsOpen) return@LaunchedEffect
        val index = state.rooms.indexOfFirst { it.id == target } + 1
        if (index == 0) return@LaunchedEffect
        if (roomList.layoutInfo.visibleItemsInfo.none { it.key == target }) roomList.scrollToItem(index)
        withFrameNanos { }
        if (draft != null) groupAnchorFocus.requestFocus() else { first.requestFocus(); restoreRoomAfterGroup = false }
    }
    LaunchedEffect(draft?.anchorId, motion.still) {
        if (draft != null && !motion.still) { wobble.snapTo(0f); wobble.animateTo(1f, motion.fade(motion.ambient)) }
        else wobble.snapTo(1f)
    }
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
    var choosing by remember(native?.id) { mutableStateOf<SonosMedia?>(null) }
    var returnFromChoice by remember(native?.id) { mutableStateOf<SonosMedia?>(null) }
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
    val art = (preparedArtwork ?: player?.artwork)?.takeIf { it.isNotBlank() && !tvAudio && room?.available == true && player?.available == true }
    val load: SonosArtLoader = loadArtwork ?: { path, modifier, ready, _ ->
        artwork(path, modifier)
        SideEffect { ready() }
    }
    val shownArt = art.takeIf { backgroundArt }
    val expanded = immersive && room != null
    key(room?.id, player?.id, shownArt == null) {
        if (shownArt != null) {
            SonosArtworkFade(shownArt, backdrop = true, load = load)
            SonosArtworkShade(expanded)
        }
    }
    val seasonClock = rememberSeasonalClock()
    SeasonalScene(Modifier.matchParentSize(), clock = seasonClock)
    Box(Modifier.fillMaxSize().stepEntrance(expanded, if (expanded) 1 else 0)) {
    if (immersive && room != null) {
        BackHandler { leaveImmersive() }
        SonosImmersive(room, native, player, busy, actionFailed, backgroundArt, art, load, guarded, ::leaveImmersive)
    } else {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(if (largeType) 16.dp else 8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(StovaIcons.Sonos, null, Modifier.size(28.dp), tint = colors.onSurface)
                Text(stringResource(R.string.sonos_title), style = MaterialTheme.typography.headlineMedium)
                SonosSeasonMark()
                Spacer(Modifier.weight(1f))
                draft?.let { edit ->
                    StovaButton(stringResource(R.string.sonos_apply), {
                        if (!busy) {
                            groupAttempted = true
                            onApplyGroup?.invoke(edit) { ok ->
                                if (ok && draft?.anchorId == edit.anchorId) {
                                    draft = null
                                    restoreRoomAfterGroup = true
                                }
                            }
                        }
                    }, Modifier.focusRequester(groupApplyFocus), primary = true, enabled = canApply || busy, compact = true)
                    StovaButton(stringResource(R.string.sonos_cancel), ::endGroup, Modifier.focusRequester(groupCancelFocus), compact = true)
                }
                if (room != null && draft == null) StovaButton(if (roomsOpen) stringResource(R.string.sonos_rooms_hide) else stringResource(R.string.sonos_rooms_show, room.displayName),
                    { focusAfterToggle = !roomsOpen; onRoomsOpen(!roomsOpen) }, Modifier.testTag("sonos-rooms-toggle"),
                    icon = StovaIcons.Rows, compact = true, quiet = true)
                if (room != null && draft == null) SonosIconButton(StovaIcons.Expand, stringResource(R.string.sonos_fullscreen), { onImmersive(true) },
                    Modifier.focusRequester(fullscreenFocus).testTag("sonos-fullscreen"))
                if (draft == null) StovaButton(stringResource(R.string.sonos_back), onBack, compact = true, quiet = true)
            }
            if (room == null) {
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = colors.onSurfaceVariant)
                    Text(stringResource(when { state.loading -> R.string.sonos_loading; state.failed -> R.string.sonos_connection_failed; else -> R.string.sonos_empty }),
                        style = MaterialTheme.typography.titleLarge)
                    if (!state.loading) StovaButton(stringResource(R.string.custom_retry), onRetry, Modifier.focusRequester(first))
                }
            } else {
                val favorites: @Composable () -> Unit = {
                    if (shelf.isNotEmpty()) SonosShelf(shelf, shelfThumbnail, shelfReturn ?: returnFromChoice, {
                        if (shelfReturn != null) onShelfRestored()
                        returnFromChoice = null
                    }, up = if (room.volume != null) volumeFocus else if (playbackAvailable) playbackFocus else if (roomsOpen) first else fullscreenFocus) { item ->
                        if (!busy) {
                            if (item.expandable) onBrowseMedia(item) else if (item.playable) choosing = item
                        }
                    }
                }
                val footer: @Composable () -> Unit = {
                    if (draft == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StovaButton(stringResource(R.string.sonos_library), onBrowse, Modifier.focusProperties { left = rooms },
                            icon = StovaIcons.Rows, compact = true, quiet = true,
                            enabled = native?.available == true && native.supports(131072))
                        StovaButton(stringResource(R.string.sonos_group), { if (onApplyGroup == null) onGroup() else beginGroup(room) },
                            icon = StovaIcons.Link, compact = true, quiet = true,
                            enabled = native?.available == true && native.supports(524288))
                        StovaButton(stringResource(R.string.sonos_more), { restoreMore = true; more = true }, Modifier.focusRequester(moreFocus),
                            compact = true, quiet = true)
                        if (onBrowseMusicAssistant != null) StovaButton(stringResource(R.string.sonos_ma), onBrowseMusicAssistant, compact = true, quiet = true)
                    }
                }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (roomsOpen) SonosRoomsPanel {
                LazyColumn(Modifier.width(220.dp).fillMaxHeight(), state = roomList, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(4.dp)) {
                    item { Text(stringResource(R.string.sonos_rooms), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                    items(state.rooms, key = { it.id }) { item ->
                        val edit = draft
                        val isAnchor = edit?.anchorId == item.id
                        val longClick = if (edit == null && onApplyGroup != null && item.available &&
                            coordinator(item, state.rooms)?.let { it.available && it.supports(524288) } == true) ({ beginGroup(item) }) else null
                        SonosRoomButton(item, rememberSonosVisual(item, state), if (edit == null) item.id == room.id else item.id in edit.selected,
                            { if (edit == null) onSelect(item.id) else if (!busy && item.available) draft = edit.toggle(item.id) },
                            Modifier.then(if (item.id == room.id) Modifier.focusRequester(first) else Modifier)
                                .then(if (isAnchor) Modifier.focusRequester(groupAnchorFocus) else Modifier)
                                .focusProperties { right = if (edit != null) if (canApply) groupApplyFocus else groupCancelFocus
                                    else if (playbackAvailable) playbackFocus else moreFocus },
                            onLongClick = longClick, editing = edit != null, anchor = isAnchor,
                            rotation = { if (edit == null || motion.still || !item.available) 0f else {
                                val t = wobble.value
                                (sin(t * 4f * Math.PI).toFloat() * (1f - t) * 4f) * if (item.id.hashCode() % 2 == 0) 1f else -1f
                            } })
                    }
                }
                }
                // Ordinary TV type fits on one screen. Large accessibility text may scroll;
                // it must never be shrunk to preserve the normal-density layout.
                val edit = draft
                Column(Modifier.weight(1f).verticalScroll(playerScroll).padding(4.dp), verticalArrangement = Arrangement.spacedBy(if (largeType) 14.dp else 8.dp)) {
                    // Reserve the same space while Sonos fetches the shelf, so the controls stay put.
                    val cover = 164.dp
                    if (largeType) {
                        key(room.id, player?.id) { SonosCover(art, Modifier.size(160.dp), load) }
                        SonosPlayback(room, player, guarded, playbackFocus, rooms, playFocus)
                    } else Row(Modifier.heightIn(min = cover), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        key(room.id, player?.id) { SonosCover(art, Modifier.size(cover), load) }
                        Column(Modifier.weight(1f)) { SonosPlayback(room, player, guarded, playbackFocus, rooms, playFocus) }
                    }
                    val groupChanged = edit != null && (anchor?.available != true || anchor.members.toSet() != edit.initial)
                    if (groupChanged || actionFailed && (edit == null || groupAttempted)) {
                        Text(stringResource(if (groupChanged) R.string.sonos_group_changed else R.string.sonos_action_failed), color = colors.error)
                    }
                    room.volume?.let { volume ->
                        SonosVolumeRail(room.id, volume, room.available && room.supports(4), busy,
                            stringResource(R.string.sonos_volume_room, room.name), onAction, Modifier.fillMaxWidth().focusRequester(volumeFocus))
                    }
                    favorites()
                    footer()
                }
            }
            }
        }
    }
    }
    }
    SeasonalScene(Modifier.matchParentSize(), front = true, clock = seasonClock)
    }
    if (more && room != null && !immersive) SonosOptionsDialog(room, native, player, busy, actionFailed, guarded) { more = false }
    choosing?.let { item ->
        if (native != null && !immersive) SonosMediaActions(item, native, busy, guarded) {
            choosing = null
            returnFromChoice = item
        }
    }
    LaunchedEffect(state.loading, state.rooms.isEmpty()) {
        if (!state.loading) { withFrameNanos { }
            // Restoring full screen, or entering it during this frame, removes the page controls.
            // The immersive view requests its own focus; never target the detached page here.
            if (!immersiveNow && shelfReturn !in shelf) {
                if (roomsOpen || room == null) first.requestFocus() else if (canPlay) playFocus.requestFocus() else moreFocus.requestFocus()
            }
        }
    }
}

/** The artwork and its contrast layer stay in place when only the viewing mode changes. */
@Composable private fun SonosArtworkShade(expanded: Boolean) {
    val ink = MaterialTheme.colorScheme.background
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val motion = stovaMotion
    val matte = animateFloatAsState(if (expanded) .14f else .85f, motion.fade(motion.standard), label = "sonos-art-shade")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        0f to ink.copy(alpha = .62f), .22f to ink.copy(alpha = if (largeType) .66f else .08f),
        .48f to ink.copy(alpha = if (largeType) .78f else .18f),
        .70f to ink.copy(alpha = .84f), 1f to ink.copy(alpha = .98f),
    )))
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = matte.value }.background(ink))
}

@Composable
private fun SonosRoomButton(room: SonosRoom, visual: SonosRoomVisual, active: Boolean, onClick: () -> Unit, modifier: Modifier,
    onLongClick: (() -> Unit)?, editing: Boolean, anchor: Boolean, rotation: () -> Float) {
    val source = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    // The chosen room's plate eases in; read only in the draw phase.
    val plate = androidx.compose.animation.animateColorAsState(if (active) colors.surfaceVariant else colors.surface,
        stovaMotion.fade(stovaMotion.quick), label = "sonos-room")
    Row(modifier.fillMaxWidth().testTag("sonos-room-${room.id}").focusOutline(source, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium).drawBehind { drawRect(plate.value) }
        .semantics { selected = active }.combinedClickable(source, null, role = if (editing) Role.Checkbox else Role.Tab,
            onLongClickLabel = if (onLongClick != null) stringResource(R.string.sonos_group) else null,
            onLongClick = onLongClick, onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // The speaker's light already says whether music plays; no separate play glyph.
        SonosSpeakerArt(room.speaker, visual, Modifier.size(52.dp, 48.dp).graphicsLayer { rotationZ = rotation() }, "sonos-speaker-${room.id}", room.available)
        val status = when {
            !room.available -> R.string.sonos_unavailable
            room.tvAudio -> R.string.sonos_tv
            room.members.size > 1 && !editing -> R.string.sonos_grouped
            else -> null
        }
        val detail = listOfNotNull(status?.let { stringResource(it) }, room.speaker.label).joinToString(" · ")
        val roomDetail = if (anchor) stringResource(R.string.sonos_group_leader) else detail
        SonosLabels(room.displayName, roomDetail, MaterialTheme.typography.titleMedium, Modifier.weight(1f))
        if (editing && active) Icon(if (anchor) StovaIcons.Link else StovaIcons.Check,
            if (anchor) null else stringResource(R.string.sonos_group_selected), Modifier.size(20.dp),
            tint = colors.onSurface)
    }
}

@Composable internal fun SonosCover(path: String?, modifier: Modifier, load: SonosArtLoader) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(StovaIcons.Sonos, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        key(path == null) { SonosArtworkFade(path, backdrop = false, sharedCover = true, load = load) }
    }
}

/** Status, title and artist; a new track crossfades in. TV audio says so and shows no stale track. */
@Composable internal fun SonosTrackText(room: SonosRoom, player: SonosRoom?, tv: Boolean, largeType: Boolean, immersive: Boolean = false) {
    val alignment = if (immersive) Alignment.CenterHorizontally else Alignment.Start
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = alignment) {
        Crossfade((if (tv) null else player?.title) to (if (tv) null else player?.artist),
            animationSpec = stovaMotion.fade(stovaMotion.standard), label = "sonos-track") { (title, artist) ->
            SonosLabels(if (tv) stringResource(R.string.sonos_tv) else title?.takeIf(String::isNotBlank) ?: stringResource(R.string.sonos_no_track),
                artist, if (immersive && !largeType) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                titleLines = if (immersive && !largeType) 2 else 1, detailStyle = MaterialTheme.typography.titleMedium, alignment = alignment)
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

@Composable internal fun SonosProgress(player: SonosRoom, prominent: Boolean = false, modifier: Modifier = Modifier) {
    val now by rememberResumedNow(1000, player.playing)
    val position = player.position(now)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (prominent) Alignment.CenterHorizontally else Alignment.Start) {
        val colors = MaterialTheme.colorScheme
        LinearProgressIndicator(progress = { position.toFloat() / player.duration }, Modifier.fillMaxWidth(),
            color = if (prominent) colors.onSurface else colors.onSurfaceVariant,
            trackColor = if (prominent) colors.onSurface.copy(alpha = .45f) else colors.outlineVariant, drawStopIndicator = {})
        Text(stringResource(R.string.sonos_progress, mediaTime(position), mediaTime(player.duration)),
            style = if (prominent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)

    }
}
private fun mediaTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
