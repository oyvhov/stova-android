package app.stova.homebutton

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaColors
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsSwitchRow
import app.stova.core.ui.StovaButton
import kotlinx.coroutines.launch

@Composable
fun HomeButtonSettings(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    val guard by container.crashGuard.data.collectAsStateWithLifecycle(initialValue = null)
    var serviceOn by remember { mutableStateOf(HomeButtonStatus.serviceEnabled(context)) }
    // The user turns the service on in system settings and comes back here.
    LifecycleResumeEffect(Unit) {
        serviceOn = HomeButtonStatus.serviceEnabled(context)
        onPauseOrDispose { }
    }

    SettingsGroup(stringResource(R.string.settings_home_button)) {
        Text(
            stringResource(if (serviceOn) R.string.hb_service_on else R.string.hb_service_off),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = RowInset,
        )
        // The Shield has no screen of its own for this: Android's settings open, and the path shows.
        var showPath by remember { mutableStateOf(false) }
        StovaButton(stringResource(R.string.hb_open_accessibility), modifier = RowInset, onClick = {
            showPath = !app.stova.navigation.openAccessibilitySettings(context)
        })
        if (showPath) Text(stringResource(R.string.hb_accessibility_path), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = RowInset)
        SettingsSwitchRow(
            stringResource(R.string.hb_override),
            household?.homeButtonOverride ?: true,
            { on -> scope.launch { container.household.update { it.copy(homeButtonOverride = on) } } },
            stringResource(R.string.hb_override_hint),
        )
    }
    if (guard?.tripped == true) {
        SettingsGroup(stringResource(R.string.hb_crash_title)) {
            Text(stringResource(R.string.hb_crash), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = RowInset)
            StovaButton(stringResource(R.string.hb_crash_reset), { scope.launch { CrashGuard.reset(container.crashGuard) } }, RowInset, primary = true)
        }
    }
    SettingsGroup(stringResource(R.string.hb_adb_title)) {
        Text(
            stringResource(R.string.hb_adb_body, context.packageName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = RowInset,
        )
        Text(stringResource(R.string.hb_warning), style = MaterialTheme.typography.bodyMedium, color = StovaColors.Caution, modifier = RowInset)
    }
}

/** Free text and buttons in a group line up with the 16 dp inset of the rows around them. */
private val RowInset = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
