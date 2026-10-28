package app.stova.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaDropdown
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.TopDropdown
import app.stova.core.ui.focusOutline
import app.stova.homebutton.HomeButtonService
import app.stova.system.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The rows of the Settings icon's menu, in order. */
enum class SystemAction { NETWORK, BLUETOOTH, RESTART, ALL_SETTINGS }

/** What a long press on the Settings icon needs: the device's own status and what each row does. */
class SystemMenu(val status: suspend () -> SystemStatus, val perform: (SystemAction) -> Unit)

/** The real menu: Android's status read off the main thread, rows that open Android's own pages. */
@Composable
fun rememberSystemMenu(): SystemMenu {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        SystemMenu({ withContext(Dispatchers.IO) { readSystemStatus(context) } }) { performSystemAction(context, it) }
    }
}

fun performSystemAction(context: Context, action: SystemAction) {
    fun open(vararg actions: String) {
        for (name in actions) {
            try { context.startActivity(Intent(name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
            catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
        }
        openSystemSettings(context)
    }
    when (action) {
        SystemAction.NETWORK -> open(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
        SystemAction.BLUETOOTH -> open(Settings.ACTION_BLUETOOTH_SETTINGS)
        // Android's own power menu (restart, power off) is reached through Stova's Home-button service.
        SystemAction.RESTART -> if (app.stova.homebutton.HomeButtonStatus.serviceEnabled(context))
            context.sendBroadcast(Intent(HomeButtonService.ACTION_POWER_MENU).setPackage(context.packageName))
            else open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        SystemAction.ALL_SETTINGS -> openSystemSettings(context)
    }
}

/** A small card that drops from the Settings icon. Back or a chosen row closes it. */
@Composable
internal fun SystemQuickMenu(menu: SystemMenu, onDismiss: () -> Unit) {
    val status by produceState<SystemStatus?>(null, menu) { value = runCatching { menu.status() }.getOrNull() }
    // Below the icon and the name it shows while focused (one label line under the 48 dp icon).
    TopDropdown(onDismiss, 332.dp, gap = 26.dp, alignEnd = true) {
        val now = status
        if (now != null) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Text(now.device, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.system_android_up, now.android, uptime(now.uptimeMinutes)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
        fun choose(action: SystemAction) { onDismiss(); menu.perform(action) }
        MenuRow({ NetworkIcon(now?.network) }, stringResource(R.string.system_network), null, Modifier.focusRequester(first)) {
            choose(SystemAction.NETWORK)
        }
        MenuRow(StovaIcons.Bluetooth, stringResource(R.string.system_bluetooth), now?.bluetooth?.let { bluetooth(it) }) {
            choose(SystemAction.BLUETOOTH)
        }
        MenuRow(StovaIcons.Power, stringResource(R.string.system_restart), now?.let {
            stringResource(if (it.powerMenu) R.string.system_restart_hint else R.string.system_restart_needs)
        }) { choose(SystemAction.RESTART) }
        MenuRow(StovaIcons.Gear, stringResource(R.string.system_all_settings), null) { choose(SystemAction.ALL_SETTINGS) }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, detail: String?, modifier: Modifier = Modifier, onClick: () -> Unit) =
    MenuRow({ Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }, label, detail, modifier, onClick)

@Composable
private fun MenuRow(icon: @Composable () -> Unit, label: String, detail: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    Row(modifier.padding(horizontal = 8.dp).fillMaxWidth().heightIn(min = 48.dp).focusOutline(source, shape).clip(shape)
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun uptime(minutes: Long): String =
    if (minutes >= 1440) stringResource(R.string.system_uptime_days, minutes / 1440, minutes % 1440 / 60)
    else stringResource(R.string.system_uptime_hours, minutes / 60, minutes % 60)

/** Connected or not, and how strongly, in the icon alone: lit Wi-Fi arcs, a cable, or Wi-Fi struck through. */
@Composable
private fun NetworkIcon(status: NetworkStatus?) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val description = status?.let {
        stringResource(when (it) { is NetworkStatus.Wifi -> R.string.system_wifi; NetworkStatus.Ethernet -> R.string.system_ethernet
            NetworkStatus.Offline -> R.string.system_offline })
    }
    Box(Modifier.size(22.dp).then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })) {
        val fill = Modifier.matchParentSize()
        when (status) {
            is NetworkStatus.Wifi -> {
                Icon(StovaIcons.Wifi, null, fill, tint = tint.copy(alpha = StovaDropdown.UnlitAlpha))
                Icon(StovaIcons.wifiArcs(wifiArcs(status.level)), null, fill, tint = tint)
            }
            NetworkStatus.Ethernet -> Icon(StovaIcons.Ethernet, null, fill, tint = tint)
            NetworkStatus.Offline -> Icon(StovaIcons.WifiOff, null, fill, tint = tint)
            null -> Icon(StovaIcons.Wifi, null, fill, tint = tint)
        }
    }
}

@Composable
private fun bluetooth(status: BluetoothStatus): String {
    val state = stringResource(if (status.on) R.string.system_on else R.string.system_off)
    val paired = status.paired?.takeIf { status.on }?.let { pluralStringResource(R.plurals.system_bluetooth_paired, it, it) }
    return listOfNotNull(state, paired).joinToString(" · ")
}
