package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import app.stova.AppContainer
import app.stova.R
import app.stova.apps.InstalledApp
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsSwitchRow
import app.stova.profiles.Profile
import java.text.Collator
import kotlinx.coroutines.launch

/** An adult chooses, app by app, what a kid profile may open. */
@Composable
fun KidAppsSettings(container: AppContainer, kid: Profile) {
    val scope = rememberCoroutineScope()
    val locale = LocalLocale.current.platformLocale
    val installed by produceState<List<InstalledApp>?>(null) {
        val collator = Collator.getInstance(locale)
        value = container.appCatalog.installed().sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
    Text(stringResource(R.string.kid_apps_title, kid.name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    val apps = installed ?: return
    SettingsGroup(stringResource(R.string.kid_apps)) {
        apps.forEach { app ->
            SettingsSwitchRow(app.label, app.key in kid.apps.kidAllowed, { allowed ->
                scope.launch { container.apps.setKidAllowed(kid.id, app.key, allowed) }
            })
        }
    }
}
