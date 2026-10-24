package app.stova.settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaColors
import app.stova.core.ui.*
import app.stova.media.ui.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch
@Composable fun AccountsSettings(profile: Profile,container: AppContainer) {
 val accounts by remember(profile.id) { container.mediaAccounts.forProfile(profile.id) }.collectAsStateWithLifecycle(initialValue=emptyList())
 var adding by remember(profile.id) { mutableStateOf(false) };val scope=rememberCoroutineScope()
 BackHandler(adding) { adding=false }
 if(adding) { AddAccountFlow(profile.id,container) { adding=false };return }
 SettingsGroup(stringResource(R.string.settings_accounts)) {
  accounts.forEach { account ->
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    ServiceLogo(account.kind)
    Column {
     Text(account.serverName,style=MaterialTheme.typography.titleMedium);Text(account.userName)
     if(account.needsSignIn) Text(stringResource(R.string.account_sign_in_again),color=StovaColors.Caution)
    }
    if(account.needsSignIn) StovaButton(stringResource(R.string.account_sign_in_again),{ adding=true })
    StovaButton(stringResource(R.string.account_sign_out),{ scope.launch { container.mediaAccounts.remove(account.id) } })
   }
  }
  StovaButton(stringResource(R.string.account_add),{ adding=true },primary=true)
 }
}
