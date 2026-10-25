package app.stova.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.core.theme.LocalAppearance
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.SettingsChoiceRow
import app.stova.core.ui.StovaButton
import app.stova.core.ui.focusOutline
import app.stova.profiles.IconColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class IconSearchResult(val pack: IconPack?, val query: String, val names: List<String>)

/** No eager bitmap list: search the disk index, then decode only visible grid cells. */
@Composable
fun IconPicker(art: AppArtCache, initialPack: String?, onSelect: (IconOverride) -> Unit, onDismiss: () -> Unit) {
    val packs by art.iconPacks.packs.collectAsStateWithLifecycle()
    LaunchedEffect(art) { art.iconPacks.refresh() }
    var chosen by remember(initialPack) { mutableStateOf(initialPack) }
    var query by remember { mutableStateOf("") }
    val pack = packs.firstOrNull { it.packageName == chosen } ?: packs.firstOrNull()
    val loaded by produceState<Pair<IconPack, IconPackIndex>?>(null, pack) {
        value = pack?.let { it to art.iconPacks.index(it) }
    }
    val index = loaded?.takeIf { it.first == pack }?.second
    val result by produceState<IconSearchResult?>(null, pack, index, query) {
        value = withContext(Dispatchers.Default) { IconSearchResult(pack, query, index?.drawables.orEmpty().filter { it.contains(query.trim(), ignoreCase = true) }) }
    }
    val names = result?.takeIf { it.pack == pack && it.query == query }?.names.orEmpty()
    val search = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize(0.9f).widthIn(max = 850.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.icon_from_pack), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (pack == null) {
                Text(stringResource(R.string.icon_no_packs), color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.icon_pack_recommended), color = MaterialTheme.colorScheme.onSurfaceVariant)
                StovaButton(stringResource(R.string.icon_close), onDismiss)
            } else {
                SettingsChoiceRow(stringResource(R.string.icon_pack), pack.label) { step ->
                    chosen = packs[Math.floorMod(packs.indexOf(pack) + step, packs.size)].packageName
                    query = ""
                }
                RemoteTextField(query, { query = it }, stringResource(R.string.icon_search), Modifier.fillMaxWidth().focusRequester(search))
                LaunchedEffect(pack.packageName) { search.requestFocus() }
                if (index == null) Text(stringResource(R.string.icon_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else if (names.isEmpty()) Text(stringResource(R.string.icon_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(names, key = { "${pack.cacheKey}:$it" }) { name ->
                        PickerIcon(art, pack, name) { onSelect(IconOverride(pack.packageName, name)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerIcon(art: AppArtCache, pack: IconPack, name: String, onClick: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, art, pack, name) { value = art.packIcon(pack, name) }
    val source = remember { MutableInteractionSource() }
    val prefs = LocalAppearance.current
    Column(Modifier.fillMaxWidth().focusOutline(source, MaterialTheme.shapes.medium).clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant).clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            image?.let { Image(it, null, colorFilter = if (pack.monochrome && prefs.iconColor == IconColor.ACCENT) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null) }
        }
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
