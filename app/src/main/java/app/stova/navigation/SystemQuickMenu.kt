package app.stova.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline
import app.stova.core.ui.freshPressesOnly
import app.stova.homebutton.HomeButtonService
import app.stova.system.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The rows of the Settings icon's menu, in order. */
enum class SystemAction { NETWORK, BLUETOOTH, DISPLAY, STORAGE, RESTART, ALL_SETTINGS }

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
        SystemAction.DISPLAY -> open(Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS)
        SystemAction.STORAGE -> open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
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
    // Below the icon and the name it shows while focused (48 dp + one label line).
    val offset = with(LocalDensity.current) { IntOffset(0, 74.dp.roundToPx()) }
    Popup(alignment = Alignment.TopEnd, offset = offset, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        val motion = stovaMotion
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(shown, enter = fadeIn(motion.enter()) + scaleIn(motion.enter(), initialScale = .92f,
            transformOrigin = TransformOrigin(1f, 0f))) {
            Column(Modifier.width(332.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant)
                .onKeyEvent { event ->
                    if (event.key != Key.Back && event.key != Key.Escape) false
                    else { if (event.type == KeyEventType.KeyUp) onDismiss(); true }
                }
                // Holding OK opened the menu; that hold's repeats and release must not choose the first row.
                .freshPressesOnly()
                .padding(vertical = 10.dp)) {
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
                MenuRow(StovaIcons.Wifi, stringResource(R.string.system_network), now?.network?.let { network(it) },
                    Modifier.focusRequester(first)) { choose(SystemAction.NETWORK) }
                MenuRow(StovaIcons.Bluetooth, stringResource(R.string.system_bluetooth), now?.bluetooth?.let { bluetooth(it) }) {
                    choose(SystemAction.BLUETOOTH)
                }
                MenuRow(StovaIcons.Tv, stringResource(R.string.system_display), now?.display?.let {
                    stringResource(R.string.system_display_mode, it.width, it.height, it.hz)
                }) { choose(SystemAction.DISPLAY) }
                val context = LocalContext.current
                MenuRow(StovaIcons.Storage, stringResource(R.string.system_storage), now?.storage?.let {
                    stringResource(R.string.system_storage_free, Formatter.formatShortFileSize(context, it.free),
                        Formatter.formatShortFileSize(context, it.total))
                }) { choose(SystemAction.STORAGE) }
                MenuRow(StovaIcons.Power, stringResource(R.string.system_restart), now?.let {
                    stringResource(if (it.powerMenu) R.string.system_restart_hint else R.string.system_restart_needs)
                }) { choose(SystemAction.RESTART) }
                MenuRow(StovaIcons.Gear, stringResource(R.string.system_all_settings), null) { choose(SystemAction.ALL_SETTINGS) }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, detail: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    Row(modifier.padding(horizontal = 8.dp).fillMaxWidth().heightIn(min = 48.dp).focusOutline(source, shape).clip(shape)
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun network(status: NetworkStatus): String = when (status) {
    NetworkStatus.Offline -> stringResource(R.string.system_offline)
    is NetworkStatus.Ethernet -> listOfNotNull(stringResource(R.string.system_ethernet), status.ip).joinToString(" · ")
    is NetworkStatus.Wifi -> listOfNotNull(
        stringResource(R.string.system_wifi),
        stringResource(when { status.level >= 3 -> R.string.system_signal_strong; status.level == 2 -> R.string.system_signal_good
            else -> R.string.system_signal_weak }),
        status.band?.let { stringResource(when (it) { WifiBand.GHZ_24 -> R.string.system_band_24; WifiBand.GHZ_5 -> R.string.system_band_5
            WifiBand.GHZ_6 -> R.string.system_band_6 }) },
        status.ip,
    ).joinToString(" · ")
}

@Composable
private fun bluetooth(status: BluetoothStatus): String {
    val state = stringResource(if (status.on) R.string.system_on else R.string.system_off)
    val paired = status.paired?.takeIf { status.on }?.let { pluralStringResource(R.plurals.system_bluetooth_paired, it, it) }
    return listOfNotNull(state, paired).joinToString(" · ")
}
