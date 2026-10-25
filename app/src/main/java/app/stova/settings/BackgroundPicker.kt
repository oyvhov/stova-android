package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.SettingsNavRow
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaDialog
import app.stova.media.SpotlightEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Searches Movie/Series across the profile's libraries. No account or artwork leaks across profiles. */
@Composable
fun BackgroundPicker(search: suspend (String) -> List<SpotlightEntry>, onSelect: (SpotlightEntry) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SpotlightEntry>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val currentSearch by rememberUpdatedState(search)
    val field = remember { FocusRequester() }
    LaunchedEffect(query, retry) {
        results = null; failed = false
        delay(250)
        try { results = currentSearch(query) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    StovaDialog(onDismiss) {
        Column(Modifier.fillMaxSize(0.9f).widthIn(max = 760.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.custom_pick_background), style = MaterialTheme.typography.headlineSmall)
            RemoteTextField(query, { query = it }, stringResource(R.string.custom_search_titles), Modifier.fillMaxWidth().focusRequester(field))
            LaunchedEffect(Unit) { field.requestFocus() }
            when {
                failed -> {
                    Text(stringResource(R.string.custom_library_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StovaButton(stringResource(R.string.custom_retry), { retry++ })
                }
                results == null -> Text(stringResource(R.string.custom_library_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                results!!.isEmpty() -> Text(stringResource(R.string.custom_library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results!!, key = { "${it.account.id}:${it.item.id}" }) { entry ->
                        SettingsNavRow(entry.item.title, value = entry.account.serverName) { onSelect(entry) }
                    }
                }
            }
            StovaButton(stringResource(R.string.profile_cancel), onDismiss)
        }
    }
}
