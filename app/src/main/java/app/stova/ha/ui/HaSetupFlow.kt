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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val profiles by container.profiles.file.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = setup) { setup = false }
    if (setup) {
        Dialog({ setup = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp)) {
                HaSetupFlow(container) { setup = false }
            }
        }
    }
    Text(stringResource(when {
        household?.haBaseUrl == null -> R.string.ha_not_connected
        auth == HaAuthState.SIGNED_OUT -> R.string.ha_signed_out
        else -> R.string.ha_connected
    }), color = MaterialTheme.colorScheme.onSurfaceVariant)
    household?.haBaseUrl?.let { Text(it) }
    StovaButton(stringResource(R.string.ha_connect), { setup = true })
    if (household?.haBaseUrl != null) {
        StovaButton(stringResource(R.string.ha_sign_out), { scope.launch { container.haAuth.signOut(); container.household.update { it.copy(haBaseUrl = null) }; container.haSocket.reconfigure() } })
    }
    profiles?.profiles.orEmpty().filter { it.kid }.forEach { kid ->
        SettingsSwitchRow(stringResource(R.string.ha_kid_control, kid.name), kid.kidSmartHomeControl, { allowed ->
            scope.launch { container.profiles.update(kid.id) { it.copy(kidSmartHomeControl = allowed) } }
        })
    }
}
