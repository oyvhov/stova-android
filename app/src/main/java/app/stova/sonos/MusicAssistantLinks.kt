package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Explicit links avoid guessing from duplicated friendly names. No service actions during setup. */
@Composable internal fun MusicAssistantLinks(container: AppContainer, links: Map<String, String>, onBack: () -> Unit) {
    var registry by remember { mutableStateOf<HaRegistry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(container, lifecycle, retry) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val connection = container.haSocket
            connection.hold()
            try { connection.status.collectLatest { status ->
                loading = status == HaStatus.CONNECTING
                registry = if (status == HaStatus.CONNECTED) try { connection.registry() }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null } else null
                failed = registry == null && !loading
            } } finally { connection.release() }
        }
    }
    val native = registry?.entities.orEmpty().filter { (id, info) -> id.startsWith("media_player.") && info.platform == "sonos" && !info.disabled }
    val assistants = registry?.entities.orEmpty().filter { (id, info) -> id.startsWith("media_player.") && info.platform == "music_assistant" && !info.disabled }
    fun name(id: String) = registry?.entities?.get(id)?.let { it.name ?: it.originalName } ?: id
    fun save(target: String?) {
        val room = selected ?: return
        scope.launch { container.household.update { current ->
            val previous = current.sonos.musicAssistantPlayers - room
            current.copy(sonos = current.sonos.copy(musicAssistantPlayers = if (target == null) previous else
                previous.filterValues { it != target } + (room to target)))
        }; selected = null }
    }
    StovaDialog(onBack, onBack = { if (selected != null) { selected = null; true } else false }) {
        Column(Modifier.widthIn(max = 620.dp).fillMaxHeight(.85f).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (selected == null) stringResource(R.string.sonos_ma_link) else stringResource(R.string.sonos_ma_choose, name(selected!!)), style = MaterialTheme.typography.headlineSmall)
            when {
                loading -> Text(stringResource(R.string.sonos_loading))
                failed -> { Text(stringResource(R.string.sonos_connection_failed)); StovaButton(stringResource(R.string.custom_retry), { retry++ }) }
                native.isEmpty() || assistants.isEmpty() -> Text(stringResource(R.string.sonos_ma_empty))
                selected == null -> native.keys.sortedBy(::name).forEach { id ->
                    SettingsNavRow(name(id), value = links[id]?.let(::name) ?: stringResource(R.string.sonos_ma_none), onClick = { selected = id })
                }
                else -> {
                    StovaButton(stringResource(R.string.sonos_ma_none), { save(null) })
                    assistants.keys.sortedBy(::name).forEach { id -> StovaButton(name(id), { save(id) }, primary = links[selected] == id) }
                }
            }
            StovaButton(stringResource(R.string.sonos_back), { if (selected == null) onBack() else selected = null })
        }
    }
}
