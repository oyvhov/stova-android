package app.stova.sonos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.ha.cards.HaImage
import app.stova.ha.cards.HaImageSource
import app.stova.profiles.BackgroundSource
import app.stova.profiles.Profile
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

fun sonosHomeRoom(prefs: SonosPrefs, profile: Profile, rooms: List<SonosRoom>, assistants: Map<String, SonosRoom> = emptyMap()): SonosRoom? {
    if (!prefs.enabled || !prefs.homeArtwork || profile.kid || profile.appearance.backgroundSource == BackgroundSource.THEME) return null
    // A soundbar using TV audio suppresses the music background for the entire Home, even if
    // another room plays music. Old track metadata on the soundbar must never take over the TV.
    if (rooms.any { it.available && it.entity.state in setOf("playing", "buffering") && it.tvAudio }) return null
    val state = SonosState(rooms, musicAssistant = assistants)
    val candidates = rooms.filter { it.available && !it.tvAudio && it.id == it.coordinatorId }.map { native ->
        val playback = sonosPlaybackRoom(native, state) ?: native
        if (playback.id == native.id) native else playback.copy(name = native.name)
    }.filter { it.playing && !it.artwork.isNullOrBlank() }
    val preferred = rooms.find { it.id == prefs.preferredRoom }?.coordinatorId
    return candidates.find { it.id == preferred || it.id == assistants[preferred]?.id } ?: candidates.firstOrNull()
}

val LocalHomeSonos = compositionLocalOf<SonosRoom?> { null }

data class SonosHomeSession(val state: SonosState, val busy: Boolean, val failed: Boolean, val playPause: (String) -> Unit)

private fun pauseEnds(player: SonosRoom) = player.entity.lastChanged + 60_000L

fun sonosWidgetRoom(prefs: SonosPrefs, state: SonosState, now: Long = System.currentTimeMillis()): SonosRoom? {
    if (!prefs.enabled || !prefs.showNowPlaying) return null
    val rooms = state.rooms.mapNotNull { room ->
        val player = sonosPlaybackRoom(room, state) ?: return@mapNotNull null
        if (!room.available || !player.available || room.tvAudio || player.tvAudio) return@mapNotNull null
        if (player.playing || player.entity.state == "paused" && now in player.entity.lastChanged until pauseEnds(player)) room to player
        else null
    }
    val playing = rooms.filter { it.second.playing }
    val eligible = playing.ifEmpty { rooms }
    return (eligible.find { it.first.id == prefs.preferredRoom } ?: eligible.firstOrNull())?.first
}

@Composable
fun rememberSonosWidgetRoom(prefs: SonosPrefs, state: SonosState): SonosRoom? {
    var expiredAt by remember { mutableLongStateOf(0L) }
    val room = sonosWidgetRoom(prefs, state, maxOf(expiredAt, System.currentTimeMillis()))
    val player = room?.let { sonosPlaybackRoom(it, state) }
    val deadline = player?.takeIf { it.entity.state == "paused" }?.let(::pauseEnds)
    LaunchedEffect(deadline) {
        if (deadline != null) {
            delay((deadline - System.currentTimeMillis()).coerceAtLeast(0L))
            expiredAt = deadline
        }
    }
    return room
}

/** One visible-Home subscription serves the widget and opt-in backdrop. */
@Composable
fun rememberSonosHome(container: AppContainer, profile: Profile, prefs: SonosPrefs, address: String?): SonosHomeSession {
    // The widget, the opt-in backdrop and a Sonos favourite card all share this one subscription.
    val enabled = sonosHomeWanted(prefs, profile, address, sonosCardFavorite(profile))
    val repository = remember(container, profile.id, address, prefs.musicAssistant, prefs.musicAssistantPlayers) { SonosRepository(container.haSocket,
        musicAssistantLinks = { container.household.current().sonos.let { if (it.musicAssistant) it.musicAssistantPlayers else emptyMap() } }) {
        val current = container.household.current()
        // Asked again at every (re)start: the active profile and its saved favourites decide, not a stale copy.
        container.session.active.value?.let { it.id == profile.id && sonosHomeWanted(current.sonos, it, current.haBaseUrl, sonosCardFavorite(it)) } == true
    } }
    var state by remember(repository) { mutableStateOf(SonosState(loading = false)) }
    var busy by remember(repository) { mutableStateOf(false) }
    var failed by remember(repository) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled, repository, lifecycle) {
        state = SonosState(loading = false)
        if (enabled) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try { repository.observe().collect { state = it } }
            finally { state = SonosState(loading = false) }
        }
    }
    return SonosHomeSession(if (enabled) state else SonosState(loading = false), busy, failed) { id ->
        if (enabled && !busy) { busy = true; failed = false
            scope.launch { try { failed = !repository.perform(id, SonosAction.PlayPause) } finally { busy = false } }
        }
    }
}

/** The ordinary film background remains underneath until a Sonos image has actually decoded. */
@Composable
fun SonosHomeBackdrop(room: SonosRoom?, images: HaImageSource?, onReady: (Boolean) -> Unit) {
    val path = room?.artwork
    val ink = MaterialTheme.colorScheme.background
    var ready by remember(path, images) { mutableStateOf(false) }
    val alpha = animateFloatAsState(if (ready) 1f else 0f, stovaMotion.fade(stovaMotion.ambient), label = "sonos-home-art")
    LaunchedEffect(ready, path) { onReady(path != null && ready) }
    if (path != null) Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) {
        // Opaque base only becomes visible after successful decoding; a failed URL keeps Home intact.
        Box(Modifier.fillMaxSize().background(ink))
        HaImage(path, images, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            onLoad = { ready = true }, onError = { ready = false })
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(ink.copy(alpha = .88f), ink.copy(alpha = .55f)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ink.copy(alpha = .1f), ink.copy(alpha = .85f)))))
    }
}

@Composable
fun SonosHomeHero(room: SonosRoom, modifier: Modifier) {
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    BoxWithConstraints(modifier.fillMaxWidth()) {
    val compact = largeText || maxHeight < 200.dp
    Column(Modifier.fillMaxWidth().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(StovaIcons.Sonos, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.sonos_home_playing, room.name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(room.title ?: stringResource(R.string.sonos_playing), style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
            modifier = Modifier.weight(1f, fill = false), maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
        room.artist?.let { Text(it, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (!compact) room.album?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    }
}
