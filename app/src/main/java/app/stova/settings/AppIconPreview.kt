package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.*
import app.stova.core.theme.StovaTheme
import app.stova.home.LocalArtworkScale
import app.stova.profiles.AppearancePrefs

/** The same artwork and background path as Home, with the unsaved/current control values. */
@Composable
internal fun AppIconPreview(prefs: AppearancePrefs, apps: List<InstalledApp>, art: AppArtCache,
    overrides: Map<String, IconOverride> = emptyMap()) {
    StovaTheme(appearance = prefs) {
        Column(Modifier.fillMaxWidth().testTag("app-icon-preview")
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.icon_preview), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Keep previews legible and inside the pane even at the largest card setting.
                val count = apps.take(3).size.coerceAtLeast(1)
                val format = if (prefs.appStyle == app.stova.profiles.AppStyle.ICON) app.stova.profiles.RowFormat.ICON else app.stova.profiles.RowFormat.WIDE
                val width = app.stova.home.artworkDimensions(format, prefs.cardSize).width
                val scale = minOf(0.5f, (maxWidth.value - 12 * (count - 1)) / (width * count)).coerceAtLeast(0.1f)
                CompositionLocalProvider(LocalArtworkScale provides scale) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        apps.take(3).forEach { app ->
                            AppTile(app, art, {}, {}, Modifier.focusProperties { canFocus = false }, overrides[app.key])
                        }
                    }
                }
            }
        }
    }
}
