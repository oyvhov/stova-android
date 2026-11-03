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
import androidx.compose.animation.core.Animatable
import app.stova.core.theme.stovaMotion
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.stova.AppContainer
import app.stova.ha.cards.HaImage
import app.stova.ha.cards.HaImageSource
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

@Composable
fun SonosScreen(container: AppContainer, profile: Profile, onBack: () -> Unit) {
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    val allowed = household?.sonos?.enabled == true && household?.haBaseUrl != null && !profile.kid
    val repository = remember(container, profile.id, household?.sonos?.musicAssistant, household?.sonos?.musicAssistantPlayers) { SonosRepository(container.haSocket,
        musicAssistantLinks = { container.household.current().sonos.let { if (it.musicAssistant) it.musicAssistantPlayers else emptyMap() } }) {
        val settings = container.household.current()
        settings.sonos.enabled && settings.haBaseUrl != null && container.session.active.value?.let { it.id == profile.id && !it.kid } == true
    } }
    var state by remember(repository) { mutableStateOf(SonosState()) }
    var retry by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    var grouping by remember { mutableStateOf<String?>(null) }
    // Survives the library and coming back; per profile.
    var roomsOpen by rememberSaveable(profile.id) { mutableStateOf(true) }
    // Full screen is a way of looking, not a setting: it survives rotation and the library, not a new visit.
    var immersive by rememberSaveable(profile.id) { mutableStateOf(false) }
    var library by remember(repository) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(repository, allowed, owner, retry, household?.haBaseUrl) {
        if (!allowed) { state = SonosState(loading = false); return@LaunchedEffect }
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { repository.observe().collect { state = it } }
    }
    LaunchedEffect(allowed, household) { if (household != null && !allowed) onBack() }
    val room = state.rooms.find { it.id == (selected ?: household?.sonos?.preferredRoom) } ?: state.rooms.firstOrNull()
    val player = room?.let { coordinator(it, state.rooms) }
    val images = household?.haBaseUrl?.let { remember(it) { HaImageSource(it, container.haAuth, container.haImages) } }
    val loadArt: SonosArtLoader = { path, modifier, loaded, failed ->
        HaImage(path, images, modifier, contentScale = ContentScale.Crop, onLoad = loaded, onError = failed, artworkSize = 1280)
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
        loadArt(next.path, Modifier.size(1.dp).graphicsLayer { alpha = 0f }.clearAndSetSemantics {}, {
            warmed = (listOf(next) + warmed.filterNot { it.target == next.target }).take(2)
        }, {})
    } }
    val motion = stovaMotion
    val pageAlpha = remember(libraryRoom?.id) { Animatable(if (motion.still) 1f else 0f) }
    LaunchedEffect(pageAlpha) { pageAlpha.animateTo(1f, motion.enter()) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha.value }) {
    if (libraryRoom != null) SonosLibrary(repository, libraryRoom, busy, failed, { action ->
        if (!busy) { busy = true; failed = false
            scope.launch { try { failed = !repository.perform(libraryRoom.id, action) } finally { busy = false } }
        }
    }, { path, modifier, fit, onLoad ->
        HaImage(path, images, modifier, contentScale = if (fit) ContentScale.Fit else ContentScale.Crop, onLoad = onLoad)
    }) { library = null }
    else SonosContent(state, room?.id, { id ->
        selected = id; failed = false
        scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(preferredRoom = id)) } }
    }, act,
        { grouping = player?.id; failed = false }, { library = player?.id }, { retry++ }, busy, failed,
        backgroundArt = household?.sonos?.backgroundArt == true && profile.appearance.backgroundSource != app.stova.profiles.BackgroundSource.THEME,
        onBack = onBack, roomsOpen = roomsOpen, onRoomsOpen = { roomsOpen = it },
        preparedArtwork = warmed.firstOrNull { it.matches(queue) }?.path, loadArtwork = loadArt,
        immersive = immersive, onImmersive = { immersive = it },
        onBrowseMusicAssistant = state.musicAssistant[player?.id]?.takeIf { it.available && it.supports(131072) }?.let { ma -> { library = ma.id } })
    }
    grouping?.let { id -> state.rooms.find { it.id == id }?.let { anchor ->
        SonosGroupDialog(anchor, state.rooms, busy, failed, { chosen ->
            if (!busy) {
                busy = true; failed = false
                val expected = anchor.members.toSet()
                scope.launch { try {
                    val ok = repository.group(anchor.id, chosen, expected)
                    failed = !ok; if (ok) grouping = null
                } finally { busy = false } }
            }
        }, { grouping = null })
    } }
}
