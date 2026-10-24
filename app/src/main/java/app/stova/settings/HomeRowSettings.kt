package app.stova.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsSwitchRow
import app.stova.core.ui.StovaButton
import app.stova.home.HomeRowKey
import app.stova.home.HomeViewModel
import app.stova.home.arrangeRows
import app.stova.home.moveItem
import app.stova.profiles.HomePrefs
import app.stova.profiles.Profile
import app.stova.profiles.StartFocus
import kotlinx.coroutines.launch

@Composable
fun HomeRowSettings(container: AppContainer, profile: Profile, home: HomeViewModel) {
    val scope = rememberCoroutineScope()
    val available by home.availableRows.collectAsStateWithLifecycle()
    val prefs = profile.home
    fun save(change: (HomePrefs) -> HomePrefs) {
        scope.launch { container.profiles.update(profile.id) { it.copy(home = change(it.home)) } }
    }

    SettingsGroup(stringResource(R.string.settings_home)) {
        SettingsSwitchRow(
            stringResource(R.string.home_start_on_apps),
            prefs.startFocus == StartFocus.FAVORITE_APPS,
            { onApps -> save { it.copy(startFocus = if (onApps) StartFocus.FAVORITE_APPS else StartFocus.FIRST_MEDIA_ROW) } },
            stringResource(R.string.home_start_on_apps_hint),
        )
    }
    // Every row the profile could have, hidden ones included, in the profile's own order.
    val ordered = arrangeRows(available, prefs.copy(hiddenRows = emptySet()))
    if (ordered.isNotEmpty()) {
        SettingsGroup(stringResource(R.string.home_rows)) {
            ordered.forEach { key ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        SettingsSwitchRow(rowLabel(key), key !in prefs.hiddenRows, { visible ->
                            save { it.copy(hiddenRows = if (visible) it.hiddenRows - key else it.hiddenRows + key) }
                        })
                    }
                    StovaButton(stringResource(R.string.row_move_up), { save { it.copy(rowOrder = moveItem(ordered, key, -1)) } })
                    StovaButton(stringResource(R.string.row_move_down), { save { it.copy(rowOrder = moveItem(ordered, key, +1)) } })
                }
            }
        }
    }
}

/** Human name for a row key. Plan 2 adds the media rows. */
@Composable
fun rowLabel(key: HomeRowKey): String = when (key) {
    HomeRowKey.APPS -> stringResource(R.string.row_favorite_apps)
    HomeRowKey.WATCH_NEXT -> stringResource(R.string.row_watch_next)
    else -> key.value
}
