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

/** Cinema mode under Smarthus: on or off, and the lights with how low each goes while Spole plays. */
@Composable
internal fun CinemaSettings(prefs: CinemaPrefs, onChange: (CinemaPrefs) -> Unit, onPickLights: () -> Unit) {
    SettingsGroup(stringResource(R.string.ha_cinema)) {
        SettingsSwitchRow(stringResource(R.string.ha_cinema_switch), prefs.enabled, { onChange(prefs.copy(enabled = it)) },
            subtitle = stringResource(R.string.ha_cinema_note))
        if (prefs.enabled) {
            SettingsNavRow(stringResource(R.string.ha_cinema_lights), value = if (prefs.levels.isEmpty()) stringResource(R.string.ha_cinema_lights_none)
                else pluralStringResource(R.plurals.ha_cinema_lights_count, prefs.levels.size, prefs.levels.size), onClick = onPickLights)
        }
    }
}

/** The next choice for one light: not in cinema mode, then off, then up through the levels, and round. */
internal fun nextCinemaLevel(level: Int?, step: Int): Int? {
    val choices = listOf<Int?>(null) + CinemaPrefs.LEVELS
    val at = choices.indexOf(level).takeIf { it >= 0 } ?: 0
    return choices[(at + step).mod(choices.size)]
}

@Composable
private fun cinemaLevelLabel(level: Int?): String = when (level) {
    null -> stringResource(R.string.ha_cinema_light_unused)
    0 -> stringResource(R.string.ha_cinema_level_off)
    else -> stringResource(R.string.ha_cinema_level_value, level)
}

/** The house's lights by room, as Home Assistant names them, each set to its level in cinema mode or left out. */
@Composable
internal fun CinemaLightPicker(connection: HaConnection, registry: suspend () -> HaRegistry?, selected: Map<String, Int>,
    onSave: (Map<String, Int>) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(selected) }
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
                            val id = light.entity.id
                            SettingsChoiceRow(light.name, cinemaLevelLabel(draft[id]),
                                modifier = if (light == found.first()) Modifier.focusRequester(first) else Modifier) { step ->
                                draft = nextCinemaLevel(draft[id], step)?.let { draft + (id to it) } ?: (draft - id)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.ha_cancel), onDismiss, compact = true, quiet = true)
                // Lights not found now (unavailable for a while) keep their level.
                StovaButton(stringResource(R.string.ha_save), { onSave(draft) }, compact = true)
            }
        }
    }
    LaunchedEffect(lights, failed) { if (lights?.isNotEmpty() == true || failed) runCatching { first.requestFocus() } }
}
