package app.stova.ha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun HaSetupFlow(container: AppContainer, onDone: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var address by remember { mutableStateOf<HaAddress?>(null) }
    var web by remember { mutableStateOf(false) }
    var longLived by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun validate(): HaAddress? = when (val check = checkHaAddress(input)) {
        AddressCheck.Invalid -> { error = R.string.ha_invalid_address; null }
        AddressCheck.InsecurePublic -> { error = R.string.ha_insecure_address; null }
        is AddressCheck.Ok -> { error = null; check.address }
    }
    fun complete(code: String?) {
        if (busy) return
        val target = address ?: return
        busy = true
        scope.launch {
            try {
                if (code != null) container.haAuth.exchange(code, target) else container.haAuth.useLongLivedToken(token)
                container.household.update { it.copy(haBaseUrl = target.base) }
                container.haRepository.invalidateRegistry()
                container.haSocket.reconfigure()
                onDone()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = R.string.ha_login_failed; web = false }
            finally { busy = false }
        }
    }
    if (web && address != null) {
        HaLoginWebView(address!!, { complete(it) }, { web = false })
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RemoteTextField(input, { input = it }, stringResource(R.string.ha_address))
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (longLived) {
            RemoteTextField(token, { token = it }, stringResource(R.string.ha_token))
            StovaButton(stringResource(R.string.ha_save), { if (token.isNotBlank()) complete(null) }, enabled = !busy)
        } else {
            StovaButton(stringResource(R.string.ha_login), { address = validate(); web = address != null }, enabled = !busy)
            StovaButton(stringResource(R.string.ha_use_token), { address = validate(); longLived = address != null })
        }
        StovaButton(stringResource(R.string.ha_cancel), onDone)
    }
}

@Composable
fun HaSettings(container: AppContainer) {
    var setup by remember { mutableStateOf(false) }
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    val auth by container.haAuth.state.collectAsStateWithLifecycle()
    val connection by container.haSocket.status.collectAsStateWithLifecycle()
    val profiles by container.profiles.file.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = setup) { setup = false }
    val enterPane = app.stova.settings.LocalSettingsPaneEntry.current
    // Status is meaningful only while the configured connection is awake. Settings is a
    // lifecycle-scoped observer, just like the dashboard; it subscribes to no entities.
    val address = household?.haBaseUrl
    LifecycleResumeEffect(address, container.haSocket) {
        if (address != null) container.haSocket.hold()
        onPauseOrDispose { if (address != null) container.haSocket.release() }
    }
    if (setup) {
        StovaDialog({ setup = false }) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp)) {
                HaSetupFlow(container) { setup = false }
            }
        }
    }
    if (household == null) return
    HaSettingsContent(when {
        address == null -> HaStatus.NOT_CONFIGURED
        auth == HaAuthState.SIGNED_OUT -> HaStatus.SIGNED_OUT
        else -> connection
    }, address, profiles?.profiles.orEmpty().filter { it.kid }, { setup = true },
        onSignOut = { scope.launch { container.haAuth.signOut(); enterPane(); container.household.update { it.copy(haBaseUrl = null) }; container.haRepository.invalidateRegistry(); container.haSocket.reconfigure() } },
        onKidControl = { kid, allowed -> scope.launch { container.profiles.update(kid.id) { it.copy(kidSmartHomeControl = allowed) } } })
}

@Composable
internal fun HaSettingsContent(status: HaStatus, address: String?, kids: List<app.stova.profiles.Profile>,
    onConfigure: () -> Unit, onSignOut: () -> Unit, onKidControl: (app.stova.profiles.Profile, Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CategoryIcon(StovaIcons.Link)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.ha_settings_connection),Modifier.weight(1f,fill=false),style=MaterialTheme.typography.titleMedium)
                        HaStatusLabel(status)
                    }
                    address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text(stringResource(if (address == null) R.string.ha_settings_connect_hint else R.string.ha_settings_layout_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StovaButton(stringResource(if (address == null) R.string.ha_connect else R.string.ha_settings_change), onConfigure,
                icon = StovaIcons.Link, primary = address == null)
        }
        if (kids.isNotEmpty()) SettingsGroup(stringResource(R.string.ha_settings_permissions)) {
            kids.forEach { kid -> SettingsSwitchRow(stringResource(R.string.ha_kid_control, kid.name), kid.kidSmartHomeControl,
                { allowed -> onKidControl(kid, allowed) }) }
        }
        if (address != null) StovaButton(stringResource(R.string.ha_sign_out), onSignOut, quiet = true, compact = true)
    }
}
