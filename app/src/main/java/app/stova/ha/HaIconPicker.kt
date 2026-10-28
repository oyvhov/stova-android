package app.stova.ha

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.StovaDialog
import app.stova.core.ui.focusOutline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every Home Assistant icon, to pick one for a favourite scene. Searched by the English name Home
 * Assistant gives it; before a search, [current] and icons a house often uses. The whole list is
 * read only while the picker is open.
 */
@Composable
fun HaIconPicker(current: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val all by produceState<List<HaIconEntry>?>(null) {
        value = withContext(Dispatchers.IO) { runCatching { HaIconFont.all { context.assets.open("mdi/names.txt") } }.getOrDefault(emptyList()) }
    }
    var query by remember { mutableStateOf("") }
    val shown = remember(all, query, current) { all?.let { matching(it, query, current) } }
    val search = remember { FocusRequester() }
    StovaDialog(onDismiss) {
        Column(Modifier.fillMaxSize(0.9f).widthIn(max = 850.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.icon_change), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            RemoteTextField(query, { query = it }, stringResource(R.string.ha_icon_search), Modifier.fillMaxWidth().focusRequester(search))
            LaunchedEffect(Unit) { search.requestFocus() }
            if (shown == null) Text(stringResource(R.string.icon_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (shown.isEmpty()) Text(stringResource(R.string.icon_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown.orEmpty(), key = { it.name }) { entry -> PickerGlyph(entry) { onSelect(entry.icon) } }
            }
        }
    }
}

/** Before a search, [current] and then [SuggestedHaIcons]; a search puts the exact name first, then names that start with it. */
internal fun matching(all: List<HaIconEntry>, query: String, current: String?): List<HaIconEntry> {
    val wanted = query.trim().lowercase().replace(' ', '-')
    if (wanted.isEmpty()) {
        val byName = all.associateBy { it.name }
        val own = current?.takeIf { it.startsWith("mdi:") }?.removePrefix("mdi:")
        return (listOfNotNull(own) + SuggestedHaIcons).distinct().mapNotNull(byName::get)
    }
    return all.filter { wanted in it.name }.sortedBy { if (it.name == wanted) 0 else if (it.name.startsWith(wanted)) 1 else 2 }
}

/** What a home's scenes and scripts are often about: light, screens, rooms, pets, heat and the door. */
internal val SuggestedHaIcons = listOf(
    "lightbulb", "lightbulb-group", "lamp", "floor-lamp", "ceiling-light", "led-strip-variant", "television", "movie-open",
    "popcorn", "music", "speaker", "sofa", "bed", "sleep", "weather-night", "weather-sunny", "coffee", "food",
    "silverware-fork-knife", "cat", "dog", "paw", "fish", "fan", "radiator", "thermometer", "snowflake", "fire", "candle",
    "blinds", "curtains", "door", "lock", "garage", "robot-vacuum", "washing-machine", "power", "home", "shield-home", "car",
    "bell", "alarm", "gamepad-variant", "pumpkin", "palette", "script-text",
)

@Composable
private fun PickerGlyph(entry: HaIconEntry, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().focusOutline(source, MaterialTheme.shapes.medium).clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant).clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HaGlyph(entry.code, 48.dp, MaterialTheme.colorScheme.onSurface)
        Text(entry.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
