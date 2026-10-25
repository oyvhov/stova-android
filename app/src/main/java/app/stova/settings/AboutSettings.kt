package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.BuildConfig
import app.stova.R
import app.stova.core.ui.SettingsGroup

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
