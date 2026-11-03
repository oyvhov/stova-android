package app.stova.sonos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.*
import app.stova.home.SeasonalScene
import app.stova.home.rememberSeasonalClock

/** Loads one library picture. [fit] keeps logos and icons whole; [onLoad] removes the fallback mark under it. */
internal typealias SonosThumbnail = @Composable (path: String, modifier: Modifier, fit: Boolean, onLoad: () -> Unit) -> Unit

@Composable
internal fun SonosLibrary(repository: SonosRepository, room: SonosRoom, busy: Boolean, failed: Boolean,
    play: (SonosAction) -> Unit, thumbnail: SonosThumbnail, onBack: () -> Unit) {
    var stack by remember(room.id) { mutableStateOf<List<SonosMedia>>(emptyList()) }
    var target by remember(room.id) { mutableStateOf<SonosMedia?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var choosing by remember { mutableStateOf<SonosMedia?>(null) }
    val first = remember { FocusRequester() }
    val current = stack.lastOrNull()
    fun back() {
        if ((loading || error) && stack.isNotEmpty() && target !== stack.last()) {
            target = stack.last(); loading = false; error = false; retry = 0
        } else if (stack.size > 1) { stack = stack.dropLast(1); target = stack.last(); loading = false; error = false; retry = 0 }
        else onBack()
    }
    BackHandler(onBack = ::back)
    LaunchedEffect(target?.id, target?.type, retry) {
        if (target != null && target === stack.lastOrNull() && retry == 0) { loading = false; error = false; return@LaunchedEffect }
        loading = true; error = false
        val found = repository.browse(room.id, target)
        if (found == null) error = true else {
            stack = if (target == null) listOf(found) else if (stack.lastOrNull()?.id == found.id && stack.lastOrNull()?.type == found.type)
                stack.dropLast(1) + found else stack + found
        }
        loading = false
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    val seasonClock = rememberSeasonalClock()
    SeasonalScene(Modifier.matchParentSize(), clock = seasonClock)
    Column(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StovaButton(stringResource(R.string.sonos_back), ::back, Modifier.focusRequester(first), icon = StovaIcons.ChevronLeft)
            SonosSeasonMark()
            Column(Modifier.weight(1f)) {
                Text(current?.title ?: stringResource(R.string.sonos_library), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.sonos_play_in, room.name), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (current?.playable == true) StovaButton(stringResource(R.string.ha_play), { choosing = current }, enabled = !busy && !loading)
        }
        if (failed) Text(stringResource(R.string.sonos_action_failed), color = MaterialTheme.colorScheme.error)
        when {
            loading -> Text(stringResource(R.string.sonos_loading))
            error -> { Text(stringResource(R.string.sonos_library_failed)); StovaButton(stringResource(R.string.custom_retry), { retry++ }) }
            current?.children.isNullOrEmpty() -> Text(stringResource(R.string.sonos_library_empty))
            else -> LazyVerticalGrid(GridCells.Adaptive(150.dp), contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                itemsIndexed(current!!.children, key = { index, item -> "${item.type}/${item.id}/$index" }) { index, item ->
                    val source = remember { MutableInteractionSource() }
                    Column(Modifier.focusOutline(source, MaterialTheme.shapes.large)
                        .clickable(source, null, enabled = item.expandable || item.playable, role = Role.Button) {
                            if (item.expandable) { target = item; retry = 0 } else choosing = item
                        }.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SonosLibraryTile(item, index, thumbnail)
                        Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
    SeasonalScene(Modifier.matchParentSize(), front = true, clock = seasonClock)
    }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    choosing?.let { item ->
        StovaDialog({ choosing = null }) {
            Column(Modifier.widthIn(max = 520.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.sonos_replace_queue), color = MaterialTheme.colorScheme.onSurfaceVariant)
                StovaButton(stringResource(R.string.ha_play), { play(SonosAction.PlayMedia(item)); choosing = null }, primary = true, enabled = !busy)
                if (room.supports(2097152)) StovaButton(stringResource(R.string.sonos_enqueue), { play(SonosAction.PlayMedia(item, enqueue = true)); choosing = null }, enabled = !busy)
                StovaButton(stringResource(R.string.sonos_cancel), { choosing = null })
            }
        }
    }
}

/**
 * Album and playlist art fills the square; logos and folder icons sit whole on a quiet plate.
 * The neutral mark shows only until a picture has loaded, never through a transparent one.
 */
@Composable private fun SonosLibraryTile(item: SonosMedia, index: Int, thumbnail: SonosThumbnail) {
    val path = item.artwork
    var loaded by remember(path) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        if (!loaded) Icon(StovaIcons.Sonos, null, Modifier.size(56.dp).testTag("sonos-library-fallback-$index"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (path != null) thumbnail(path, if (item.logoArtwork) Modifier.fillMaxSize().padding(24.dp) else Modifier.fillMaxSize(),
            item.logoArtwork) { loaded = true }
    }
}
