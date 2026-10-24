package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import app.stova.AppContainer
import app.stova.BuildConfig
import app.stova.R
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsSwitchRow
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

@Composable
fun AppearanceSettings(container: AppContainer, profile: Profile) {
    val scope = rememberCoroutineScope()
    fun save(change: (AppearancePrefs) -> AppearancePrefs) {
        scope.launch { container.profiles.update(profile.id) { it.copy(appearance = change(it.appearance)) } }
    }
    SettingsGroup(stringResource(R.string.settings_appearance)) {
        SettingsSwitchRow(
            stringResource(R.string.appearance_reduce_motion),
            profile.appearance.reduceMotion,
            { on -> save { it.copy(reduceMotion = on) } },
            stringResource(R.string.appearance_reduce_motion_hint),
        )
        SettingsSwitchRow(
            stringResource(R.string.appearance_strong_focus),
            profile.appearance.strongFocus,
            { on -> save { it.copy(strongFocus = on) } },
            stringResource(R.string.appearance_strong_focus_hint),
        )
    }
}

@Composable
fun AboutSettings() {
    SettingsGroup(stringResource(R.string.settings_about)) {
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
