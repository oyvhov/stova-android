package app.stova.sonos

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.stova.core.ui.stepEntrance
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.stova.AppContainer
import app.stova.ha.cards.HaImage
import app.stova.ha.cards.HaImageSource
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

@Composable
internal fun SonosScreen(container: AppContainer, profile: Profile, session: SonosSession, prefs: SonosPrefs,
    images: HaImageSource?, allowed: Boolean, entryRoom: String?, onRetry: () -> Unit, onBack: () -> Unit, onConnectionSettings: (() -> Unit)? = null) {
    val repository = session.repository
    val state = session.state
    var selected by rememberSaveable(profile.id) { mutableStateOf(entryRoom) }
    // Survives the library and coming back; per profile.
    var roomsOpen by rememberSaveable(profile.id) { mutableStateOf(true) }
    // Full screen is a way of looking, not a setting: it survives rotation and the library, not a new visit.
    var immersive by rememberSaveable(profile.id) { mutableStateOf(false) }
    var library by remember(repository) { mutableStateOf<String?>(null) }
    var libraryStart by remember(repository) { mutableStateOf<SonosMedia?>(null) }
    var shelfReturn by remember(repository) { mutableStateOf<SonosMedia?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(allowed) { if (!allowed) onBack() }
    val room = state.rooms.find { it.id == (selected ?: prefs.preferredRoom) } ?: state.rooms.firstOrNull()
    val player = room?.let { coordinator(it, state.rooms) }
    val shelfPlayer = player?.id?.takeIf { allowed && player.available && player.supports(131072) }
    var shelf by remember(repository, shelfPlayer) { mutableStateOf(emptyList<SonosMedia>()) }
    LaunchedEffect(repository, owner, shelfPlayer, immersive) {
        if (shelfPlayer != null && !immersive && shelf.isEmpty()) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            shelf = repository.playerShelf(shelfPlayer)
        }
    }
    val thumbnail: SonosThumbnail = { path, modifier, fit, onLoad ->
        HaImage(path, images, modifier, contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
            onLoad = onLoad, artworkSize = 320)
    }
    fun openLibrary(id: String?, start: SonosMedia? = null) {
        libraryStart = start
        shelfReturn = start
        library = id
    }
    val loadArt: SonosArtLoader = { path, modifier, loaded, failed ->
        SonosCoverImage(path, images, modifier, loaded, failed)
    }
    val act: (SonosAction) -> Unit = { action ->
        if (!busy && room != null) {
            busy = true; failed = false
            scope.launch { try { failed = !repository.perform(room.id, action) } finally { busy = false } }
        }
    }
    val libraryRoom = (state.rooms + state.musicAssistant.values).find { it.id == library }
    val queuePlayer = room?.let { sonosPlaybackRoom(it, state) }
    val queue = room?.let { sonosQueueTarget(it, state, requirePlaying = false) }
    val target = queue?.takeIf { allowed && images != null && libraryRoom == null && queuePlayer?.playing == true }
    var candidate by remember(target) { mutableStateOf<SonosQueuedArtwork?>(null) }
    var warmed by remember(repository, images, room?.id, queue?.player, queue?.queue) { mutableStateOf(emptyList<SonosQueuedArtwork>()) }
    LaunchedEffect(repository, owner, target, room?.id, queuePlayer?.shuffle, queuePlayer?.repeat) {
        if (target == null) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            candidate = null
            candidate = repository.nextArtwork(room.id, target)
        }
    }
    candidate?.let { next -> key(next) {
        HaImage(next.path, images, Modifier.size(1.dp).graphicsLayer { alpha = 0f }.clearAndSetSemantics {},
            contentScale = ContentScale.Crop, artworkSize = 1280, onLoad = {
                warmed = (listOf(next) + warmed.filterNot { it.target == next.target }).take(2)
                android.util.Log.d("StovaSonosArt", "Next cover decoded")
            })
    } }
    val prepared = warmed.firstOrNull { it.matches(queue) }
    LaunchedEffect(prepared?.target) {
        if (prepared != null) android.util.Log.d("StovaSonosArt", "Prepared cover selected")
    }
    Box(Modifier.fillMaxSize().stepEntrance(libraryRoom?.id, if (libraryRoom == null) 0 else 1)) {
    if (libraryRoom != null) SonosLibrary(repository, libraryRoom, busy, failed, { action ->
        if (!busy) { busy = true; failed = false
            scope.launch { try { failed = !repository.perform(libraryRoom.id, action) } finally { busy = false } }
        }
    }, thumbnail, start = libraryStart) { library = null }
    else SonosContent(state, room?.id, { id ->
        selected = id; failed = false
        scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(preferredRoom = id)) } }
    }, act,
        {}, { openLibrary(player?.id) }, onRetry, busy || session.busy, failed || session.failed,
        backgroundArt = prefs.backgroundArt && profile.appearance.backgroundSource != app.stova.profiles.BackgroundSource.THEME,
        onBack = onBack, roomsOpen = roomsOpen, onRoomsOpen = { roomsOpen = it },onConnectionSettings = onConnectionSettings,
        preparedArtwork = prepared?.path, loadArtwork = loadArt,
        immersive = immersive, onImmersive = { immersive = it },
        onBrowseMusicAssistant = state.musicAssistant[player?.id]?.takeIf { it.available && it.supports(131072) }?.let { ma -> { openLibrary(ma.id) } },
        shelf = shelf, shelfThumbnail = thumbnail, shelfReturn = shelfReturn,
        onShelfRestored = { shelfReturn = null }, onBrowseMedia = { openLibrary(shelfPlayer, it) },
        onApplyGroup = { draft, complete ->
            if (!busy) {
                busy = true; failed = false
                scope.launch { try {
                    val ok = repository.group(draft.anchorId, draft.selected, draft.initial)
                    failed = !ok
                    complete(ok)
                } finally { busy = false } }
            }
        })
    }
}
