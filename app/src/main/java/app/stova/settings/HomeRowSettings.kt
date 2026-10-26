package app.stova.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.stova.watchnext.WatchNextResult
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
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
import app.stova.profiles.*
import app.stova.home.*
import kotlinx.coroutines.launch

@Composable
fun HomeRowSettings(container: AppContainer, profile: Profile, home: HomeViewModel? = null, onEditAppearance: (() -> Unit)? = null,
    firstControl: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val accountFlow = remember(profile.id) { container.mediaAccounts.forProfile(profile.id) }
    val accounts by accountFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val stored = profile.home.rowOrder + profile.home.hiddenRows + profile.home.rowFormats.keys
    val known = (listOf(HomeRowKey.APPS) + (if (profile.kid) emptyList() else listOf(HomeRowKey.WATCH_NEXT)) + accounts.flatMap { account ->
        listOf(HomeRowKey.continueWatching(account.id), HomeRowKey.latestMovies(account.id), HomeRowKey.latestSeries(account.id)) +
            profile.appearance.season().takeUnless { it == Season.NONE }?.let { listOf(HomeRowKey.seasonal(account.id, it)) }.orEmpty()
    } + stored).distinct()
    val available = if (home != null) {
        val loaded by home.availableRows.collectAsStateWithLifecycle()
        (known + loaded).distinct()
    } else known
    val watchNext by container.watchNext.status.collectAsStateWithLifecycle()
    LaunchedEffect(container.watchNext, profile.kid) { if (!profile.kid) container.watchNext.read() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) container.watchNext.permissionDenied()
        scope.launch { container.watchNext.read(); container.watchNext.refresh() }
    }
    val prefs = profile.home
    fun save(change: (HomePrefs) -> HomePrefs) {
        scope.launch { container.profiles.update(profile.id) { it.copy(home = change(it.home)) } }
    }

    SettingsGroup(stringResource(R.string.settings_home)) {
        // Only a subpage may link onwards; sections never jump to each other.
        if (onEditAppearance != null) app.stova.core.ui.SettingsNavRow(stringResource(R.string.custom_home_appearance), firstControl, value = stringResource(R.string.custom_home_appearance_hint), onClick = onEditAppearance)
        SettingsSwitchRow(stringResource(R.string.home_backdrop_rotation), prefs.backdropRotation, { enabled -> save { it.copy(backdropRotation = enabled) } },
            modifier = if (onEditAppearance == null) firstControl else Modifier)
        SettingsSwitchRow(
            stringResource(R.string.home_start_on_apps),
            prefs.startFocus == StartFocus.FAVORITE_APPS,
            { onApps -> save { it.copy(startFocus = if (onApps) StartFocus.FAVORITE_APPS else StartFocus.FIRST_MEDIA_ROW) } },
            stringResource(R.string.home_start_on_apps_hint),
        )
    }
    if (!profile.kid) SettingsGroup(stringResource(R.string.row_watch_next)) {
        when (val state = watchNext) {
            is WatchNextResult.Available -> Text(stringResource(R.string.watch_next_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
            is WatchNextResult.Unavailable -> {
                Text(stringResource(if (state.reason == WatchNextResult.Reason.NO_PERMISSION) R.string.watch_next_needs_access else R.string.watch_next_blocked), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.reason == WatchNextResult.Reason.NO_PERMISSION) StovaButton(stringResource(R.string.watch_next_grant), {
                    permission.launch("android.permission.READ_TV_LISTINGS")
                })
            }
        }
    }
    // Every row the profile could have, hidden ones included, in the profile's own order.
    val ordered = arrangeRows(available, prefs.copy(hiddenRows = emptySet()))
    HomeRowOrderSettings(
        ordered, prefs.hiddenRows,
        onVisible = { key, visible -> save { it.copy(hiddenRows = if (visible) it.hiddenRows - key else it.hiddenRows + key) } },
        onMove = { key, delta -> save { it.copy(rowOrder = moveItem(ordered, key, delta)) } },
        rowFormats = prefs.rowFormats,
        appearance = profile.appearance,
        onStyle = { key, style -> save { it.copy(rowFormats = if (style == RowStyle()) it.rowFormats - key else it.rowFormats + (key to style)) } },
    )
}
@Composable
internal fun HomeRowOrderSettings(
    ordered: List<HomeRowKey>,
    hiddenRows: Set<HomeRowKey>,
    onVisible: (HomeRowKey, Boolean) -> Unit,
    onMove: (HomeRowKey, Int) -> Unit,
    rowFormats: Map<HomeRowKey, RowStyle> = emptyMap(),
    appearance: AppearancePrefs = AppearancePrefs(),
    onStyle: (HomeRowKey, RowStyle) -> Unit = { _, _ -> },
) {
    if (ordered.isNotEmpty()) {
        SettingsGroup(stringResource(R.string.home_rows)) {
            ordered.forEach { key ->
                key(key) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        SettingsSwitchRow((rowFormats[key] ?: RowStyle()).heading(rowLabel(key)), key !in hiddenRows, { visible -> onVisible(key, visible) })
                    }
                    StovaButton(stringResource(R.string.row_move_up), { onMove(key, -1) })
                    StovaButton(stringResource(R.string.row_move_down), { onMove(key, +1) })
                }
                RowStyleChoices(rowFormats[key] ?: RowStyle(), defaultRowFormat(key, appearance), appearance.cardSize, rowLabel(key),
                    hasSource = key != HomeRowKey.APPS && key != HomeRowKey.WATCH_NEXT, globalTitles = appearance.showTitles) { onStyle(key, it) }
                }
            }
        }
    }
}
