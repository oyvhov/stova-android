package app.stova.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.profiles.Profile

@Composable
fun AppearanceProfileActions(others: List<Profile>, onReset: () -> Unit, onCopy: (String) -> Unit) {
    var resetting by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    SettingsGroup(stringResource(R.string.settings_profile)) {
        Text(stringResource(R.string.custom_profile_hint), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsNavRow(stringResource(R.string.custom_reset)) { resetting = true }
        if (others.isNotEmpty()) SettingsNavRow(stringResource(R.string.custom_copy)) { copying = true }
    }
    if (resetting) StovaDialog({ resetting = false }) {
        val cancel = remember { FocusRequester() }
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.width(420.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.custom_reset_confirm), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.custom_reset_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                StovaButton(stringResource(R.string.profile_cancel), { resetting = false }, Modifier.fillMaxWidth().focusRequester(cancel))
                StovaButton(stringResource(R.string.custom_reset), { resetting = false; onReset() }, Modifier.fillMaxWidth())
            }
        }
        LaunchedEffect(Unit) { cancel.requestFocus() }
    }
    if (copying) StovaDialog({ copying = false }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.width(420.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.custom_copy), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.custom_copy_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                others.forEach { source -> StovaButton(source.name, { copying = false; onCopy(source.id) }, Modifier.fillMaxWidth()) }
                StovaButton(stringResource(R.string.profile_cancel), { copying = false })
            }
        }
    }
}
