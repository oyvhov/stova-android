package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.color
import app.stova.core.ui.PageHeader
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.StovaIcons
import app.stova.profiles.Profile

/** Apps is a link to its own page, not a section: while its tab has focus the pane says what OK opens. */
@Composable
internal fun AppsPane(profile: Profile) {
    PageHeader(stringResource(R.string.nav_apps), StovaIcons.Apps, subtitle = stringResource(R.string.settings_rail_apps), tint = AvatarColor.ROSE.color)
    SettingsGroup(stringResource(R.string.settings_apps_pane_title)) {
        Text(stringResource(R.string.settings_apps_pane_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        val favorites = profile.apps.favorites.size
        val hidden = profile.apps.hidden.size
        Text(pluralStringResource(R.plurals.settings_apps_favorites, favorites, favorites) + " · " + pluralStringResource(R.plurals.settings_apps_hidden, hidden, hidden),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
