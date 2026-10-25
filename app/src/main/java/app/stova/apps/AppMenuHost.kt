package app.stova.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

/** Shared by Home and Apps. Profile switches discard the pending menu and picker. */
@Composable
fun AppMenuHost(app: InstalledApp, apps: AppRepository, profile: Profile, art: AppArtCache, catalog: AppCatalog, rowActions: @Composable () -> Unit = {}, onDismiss: () -> Unit) {
    if (profile.kid) return
    key(profile.id, app.key) {
        val scope = rememberCoroutineScope()
        var changing by remember { mutableStateOf(false) }
        var picking by remember { mutableStateOf(false) }
        when {
            picking -> IconPicker(art, profile.apps.iconOverrides[app.key]?.packageName ?: profile.appearance.iconPack,
                { icon -> scope.launch { apps.setIcon(profile.id, app.key, icon); onDismiss() } }, { picking = false })
            changing -> {
                val first = remember { FocusRequester() }
                Dialog(onDismissRequest = { changing = false }) {
                    Column(Modifier.width(360.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.icon_change), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        StovaButton(stringResource(R.string.icon_from_pack), { picking = true }, Modifier.fillMaxWidth().focusRequester(first))
                        StovaButton(stringResource(R.string.icon_reset), { scope.launch { apps.setIcon(profile.id, app.key, null); onDismiss() } }, Modifier.fillMaxWidth())
                    }
                    LaunchedEffect(Unit) { first.requestFocus() }
                }
            }
            else -> AppActionsMenu(app, app.key in profile.apps.favorites, app.key in profile.apps.hidden,
                onToggleFavorite = { scope.launch { apps.toggleFavorite(profile.id, app.key) } },
                onMove = { delta -> scope.launch { apps.moveFavorite(profile.id, app.key, delta) } },
                onToggleHidden = { scope.launch { apps.setHidden(profile.id, app.key, app.key !in profile.apps.hidden) } },
                onAppInfo = { catalog.openAppInfo(app.packageName) }, onDismiss = onDismiss,
                onChangeIcon = { changing = true }, rowActions = rowActions)
        }
    }
}
