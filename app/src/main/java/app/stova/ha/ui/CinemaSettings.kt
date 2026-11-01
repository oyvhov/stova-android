package app.stova.ha.ui

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Cinema mode under Smarthus: on or off, which lights, and how low they go while Spole plays. */
@Composable
internal fun CinemaSettings(prefs: CinemaPrefs, onChange: (CinemaPrefs) -> Unit, onPickLights: () -> Unit) {
    SettingsGroup(stringResource(R.string.ha_cinema)) {
        SettingsSwitchRow(stringResource(R.string.ha_cinema_switch), prefs.enabled, { onChange(prefs.copy(enabled = it)) },
            subtitle = stringResource(R.string.ha_cinema_note))
        if (prefs.enabled) {
            SettingsNavRow(stringResource(R.string.ha_cinema_lights), value = if (prefs.lights.isEmpty()) stringResource(R.string.ha_cinema_lights_none)
                else pluralStringResource(R.plurals.ha_cinema_lights_count, prefs.lights.size, prefs.lights.size), onClick = onPickLights)
            SettingsChoiceRow(stringResource(R.string.ha_cinema_level),
                if (prefs.level == 0) stringResource(R.string.ha_cinema_level_off) else stringResource(R.string.ha_cinema_level_value, prefs.level)) { step ->
                onChange(prefs.copy(level = nextCinemaLevel(prefs.level, step)))
            }
        }
    }
}

internal fun nextCinemaLevel(level: Int, step: Int): Int {
    val levels = CinemaPrefs.LEVELS
    val at = levels.indexOf(level).takeIf { it >= 0 } ?: levels.indexOfFirst { it >= level }.takeIf { it >= 0 } ?: levels.lastIndex
    return levels[(at + step).mod(levels.size)]
}

/** The house's lights by room, as Home Assistant names them, to pick for cinema mode. */
@Composable
internal fun CinemaLightPicker(connection: HaConnection, registry: suspend () -> HaRegistry?, selected: List<String>,
    onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(selected.toSet()) }
    var lights by remember { mutableStateOf<List<HaCatalogItem>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    androidx.lifecycle.compose.LifecycleResumeEffect(connection) {
        connection.hold()
        onPauseOrDispose { connection.release() }
    }
    LaunchedEffect(retry) {
        lights = null; failed = false
        try {
            withTimeoutOrNull(10_000) { connection.status.first { it == HaStatus.CONNECTED } } ?: throw java.io.IOException("not connected")
            val states = connection.states().filter { it.id.substringBefore('.') == "light" }
            lights = entityCatalog(states, registry() ?: HaRegistry()).filter { it.entity.id.substringBefore('.') == "light" && !it.hidden }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { failed = true }
    }
    StovaDialog(onDismiss) {
        Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(max = 440.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageHeader(stringResource(R.string.ha_cinema_pick_title), StovaIcons.Sun)
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val found = lights
                when {
                    failed -> item {
                        Text(stringResource(R.string.ha_cinema_failed))
                        StovaButton(stringResource(R.string.custom_retry), { retry++ }, Modifier.focusRequester(first), compact = true)
                    }
                    found == null -> item { Text(stringResource(R.string.ha_cinema_loading)) }
                    found.isEmpty() -> item { Text(stringResource(R.string.ha_cinema_no_lights)) }
                    else -> found.groupBy { it.room }.forEach { (room, inRoom) ->
                        item { Text(room ?: stringResource(R.string.ha_cinema_no_room), Modifier.padding(top = 8.dp, start = 16.dp),
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(inRoom, key = { it.entity.id }) { light ->
                            SettingsSwitchRow(light.name, light.entity.id in draft, { on -> draft = if (on) draft + light.entity.id else draft - light.entity.id },
                                modifier = if (light == found.first()) Modifier.focusRequester(first) else Modifier)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.ha_cancel), onDismiss, compact = true, quiet = true)
                // Lights not found now (unavailable for a while) stay chosen.
                StovaButton(stringResource(R.string.ha_save), { onSave(selected.filter { it in draft } + (draft - selected.toSet()).sorted()) }, compact = true)
            }
        }
    }
    LaunchedEffect(lights, failed) { if (lights?.isNotEmpty() == true || failed) runCatching { first.requestFocus() } }
}
