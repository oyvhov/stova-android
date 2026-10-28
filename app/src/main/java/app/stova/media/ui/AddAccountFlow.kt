package app.stova.media.ui
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import app.stova.media.*
import app.stova.media.net.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

private enum class SignInStep { DISCOVER, ADDRESS, PASSWORD, QUICK, DONE }
@Composable fun AddAccountFlow(profileId: String,container: AppContainer,onDone: () -> Unit) {
 val discovery=remember(container) { container.lanDiscovery.discover() }
 AddAccountContent(profileId,container.mediaAccounts,container.serverProbe,container.mediaAuth,discovery,onDone)
}
/** Dependency seam also used by the flow tests; no application graph is needed to sign in. */
@Composable internal fun AddAccountContent(profileId: String,accounts: MediaAccountRepository,probe: ServerProbe,auth: MediaAuth,discovery: Flow<DiscoveredServer>,onDone: () -> Unit) {
 var step by remember { mutableStateOf(SignInStep.DISCOVER) }
 var address by remember { mutableStateOf("") };var user by remember { mutableStateOf("") };var password by remember { mutableStateOf("") }
 var server by remember { mutableStateOf<ServerInfo?>(null) };var code by remember { mutableStateOf<QuickConnectCode?>(null) }
 var busy by remember { mutableStateOf(false) };var error by remember { mutableStateOf<Int?>(null) }
 val found=remember { mutableStateListOf<DiscoveredServer>() }; val scope=rememberCoroutineScope()
 // Each step replaces the control that was pressed. Focus waits on the section's tab meanwhile and then
 // enters the new step; left alone, Compose hands it to the first tab and Settings leaves Smarthus.
 val tab=app.stova.settings.LocalSettingsSectionFocus.current; val first=remember { FocusRequester() }
 var settle by remember { mutableIntStateOf(0) }
 fun park() { tab?.requestFocus();settle++ }
 fun go(next: SignInStep) { park();step=next }
 LaunchedEffect(settle) { repeat(8) { withFrameNanos { }; if(runCatching { first.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return@LaunchedEffect } }
 LaunchedEffect(discovery) { try { discovery.collect { found.add(it) } } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Manual entry remains available. */ } }
 fun connect(input: String) { scope.launch {
  busy=true;error=null
  try {
   val info=probe.probe(input);server=info
   if(auth.quickConnectEnabled(info)) { code=auth.quickConnectStart(info);go(SignInStep.QUICK) } else go(SignInStep.PASSWORD)
  } catch(e: CancellationException) { throw e } catch(_: Exception) { error=R.string.server_not_found }
  finally { busy=false }
 } }
 LaunchedEffect(step,code) {
  val info=server;val quick=code
  if(step==SignInStep.QUICK && info!=null && quick!=null) try {
   accounts.add(profileId,info,auth.quickConnectAwait(info,quick.secret));step=SignInStep.DONE;onDone()
  } catch(_: TimeoutCancellationException) { error=R.string.sign_in_timeout }
    catch(e: CancellationException) { throw e }
    catch(_: Exception) { error=R.string.sign_in_failed }
 }
 // Busy buttons only ignore OK: a disabled button cannot hold focus, and focus would leave Smarthus.
 Column(Modifier.focusRequester(first).focusGroup(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  when(step) {
   SignInStep.DISCOVER -> {
    found.forEach { item -> StovaButton(item.name,{ if(!busy) connect(item.address) }) }
    StovaButton(stringResource(R.string.enter_address),{ if(!busy) go(SignInStep.ADDRESS) })
   }
   SignInStep.ADDRESS -> {
    RemoteTextField(address,{ address=it },stringResource(R.string.server_address),keyboardType=KeyboardType.Uri)
    StovaButton(stringResource(R.string.account_next),{ if(!busy) connect(address) })
   }
   SignInStep.PASSWORD -> {
    RemoteTextField(user,{ user=it },stringResource(R.string.account_username))
    RemoteTextField(password,{ password=it },stringResource(R.string.account_password),keyboardType=KeyboardType.Password)
    StovaButton(stringResource(R.string.account_sign_in),{
     val info=server ?: return@StovaButton
     if(busy) return@StovaButton
     scope.launch { busy=true;error=null
      try { accounts.add(profileId,info,auth.signIn(info,user,password));password="";step=SignInStep.DONE;onDone() }
      catch(e: CancellationException) { throw e }
      catch(_: MediaError.Unauthorized) { error=R.string.wrong_credentials }
      catch(_: Exception) { error=R.string.sign_in_failed }
      finally { busy=false }
     }
    },primary=true)
   }
   SignInStep.QUICK -> {
    Text(code?.code.orEmpty(),style=MaterialTheme.typography.displaySmall)
    Text(stringResource(R.string.quick_connect_approve))
    StovaButton(stringResource(R.string.use_password),{ go(SignInStep.PASSWORD);code=null;error=null })
    // Retrying clears the error and with it this button.
    if(error!=null) StovaButton(stringResource(R.string.account_retry),{ server?.let { park();connect(it.baseUrl) } })
   }
   SignInStep.DONE -> Unit
  }
  if(busy) CircularProgressIndicator()
  error?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error) }
 }
}
