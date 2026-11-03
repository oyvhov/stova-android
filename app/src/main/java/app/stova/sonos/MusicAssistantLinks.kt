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

/**
 * What a person recognises: the name given in HA, else the player's own friendly name, else the
 * registry's original name, else the id. Names that collide get the Sonos model, else the id, so
 * two players never look the same. Naming only; links stay explicit choices.
 */
fun musicAssistantNames(registry: HaRegistry, states: Map<String, HaEntity>, ids: Collection<String>): Map<String, String> {
    val base = ids.associateWith { id ->
        val info = registry.entities[id]
        info?.name?.takeIf(String::isNotBlank) ?: states[id]?.attributes?.text("friendly_name")?.takeIf(String::isNotBlank)
            ?: info?.originalName?.takeIf(String::isNotBlank) ?: id
    }
    val shared = base.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val withModel = base.mapValues { (id, name) ->
        if (name !in shared) name else registry.entities[id]?.model?.takeIf(String::isNotBlank)?.let { "$name · $it" } ?: name
    }
    val stillShared = withModel.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return withModel.mapValues { (id, name) -> if (name in stillShared) "${base[id]} · $id" else name }
}

/** Explicit links avoid guessing from duplicated friendly names. No service actions during setup. */
@Composable internal fun MusicAssistantLinks(container: AppContainer, links: Map<String, String>, onBack: () -> Unit) {
    var registry by remember { mutableStateOf<HaRegistry?>(null) }
    var states by remember { mutableStateOf<Map<String, HaEntity>>(emptyMap()) }
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
                // Friendly names live in the states, not the registry: watch only the Sonos and MA players,
                // only while this dialog is shown. A subscription, never a poll of every entity.
                val players = registry?.entities.orEmpty().filter { (id, info) -> id.startsWith("media_player.") && !info.disabled &&
                    info.platform in setOf("sonos", "music_assistant") }.keys
                if (players.isNotEmpty()) try { connection.observeEntities(players).collect { states = it } }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { }
            } } finally { connection.release() }
        }
    }
    val native = registry?.entities.orEmpty().filter { (id, info) -> id.startsWith("media_player.") && info.platform == "sonos" && !info.disabled }
    val assistants = registry?.entities.orEmpty().filter { (id, info) -> id.startsWith("media_player.") && info.platform == "music_assistant" && !info.disabled }
    val names = registry?.let { musicAssistantNames(it, states, native.keys + assistants.keys + links.values) }.orEmpty()
    fun name(id: String) = names[id] ?: id
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
