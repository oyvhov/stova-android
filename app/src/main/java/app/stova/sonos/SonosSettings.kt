package app.stova.sonos

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import kotlinx.coroutines.launch

@Composable
fun SonosSettings(container: AppContainer, onOpen: () -> Unit) {
    val settings by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val scope = rememberCoroutineScope()
    var linking by remember { mutableStateOf(false) }
    SettingsGroup(stringResource(R.string.sonos_title)) {
        if (current.haBaseUrl == null) Text(stringResource(R.string.sonos_needs_ha))
        else {
            SettingsSwitchRow(stringResource(R.string.sonos_enable), current.sonos.enabled, { enabled ->
                scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(enabled = enabled)) } }
            }, subtitle = stringResource(R.string.sonos_settings_hint))
            if (current.sonos.enabled) {
                SettingsSwitchRow(stringResource(R.string.sonos_widget), current.sonos.showNowPlaying, { value ->
                    scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(showNowPlaying = value)) } }
                })
                SettingsSwitchRow(stringResource(R.string.sonos_ma_enable), current.sonos.musicAssistant, { value ->
                    scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(musicAssistant = value)) } }
                }, subtitle = stringResource(R.string.sonos_ma_hint))
                if (current.sonos.musicAssistant) SettingsNavRow(stringResource(R.string.sonos_ma_link), onClick = { linking = true })
                SettingsSwitchRow(stringResource(R.string.sonos_home_icon), current.sonos.showHomeButton, { value ->
                    scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(showHomeButton = value)) } }
                })
                SettingsSwitchRow(stringResource(R.string.sonos_background), current.sonos.backgroundArt, { value ->
                    scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(backgroundArt = value)) } }
                })
                SettingsSwitchRow(stringResource(R.string.sonos_home_art), current.sonos.homeArtwork, { value ->
                    scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(homeArtwork = value)) } }
                }, subtitle = stringResource(R.string.sonos_home_art_hint))
                SettingsNavRow(stringResource(R.string.sonos_open), onClick = onOpen)
            }
        }
    }
    if (linking && current.sonos.enabled && current.sonos.musicAssistant && current.haBaseUrl != null)
        MusicAssistantLinks(container, current.sonos.musicAssistantPlayers) { linking = false }
}
