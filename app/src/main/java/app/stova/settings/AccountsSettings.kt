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
import app.stova.media.model.MediaAccount
import app.stova.media.ui.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch
@Composable fun AccountsSettings(profile: Profile,container: AppContainer) {
 val accounts by remember(profile.id) { container.mediaAccounts.forProfile(profile.id) }.collectAsStateWithLifecycle(initialValue=emptyList())
 val scope=rememberCoroutineScope()
 key(profile.id) {
  AccountsContent(accounts,{ account -> scope.launch { container.mediaAccounts.remove(account.id) } }) { done -> AddAccountFlow(profile.id,container,done) }
 }
}
/** The profile's accounts and the way to add one; [addFlow] signs in and calls its argument when it is done. */
@Composable internal fun AccountsContent(accounts: List<MediaAccount>,onSignOut: (MediaAccount) -> Unit,addFlow: @Composable (onDone: () -> Unit) -> Unit) {
 var adding by remember { mutableStateOf(false) }
 // The pressed button leaves with the change, so focus first waits on the section's tab; left alone,
 // Compose hands it to the first tab and Settings opens «Heim og utsjånad».
 val tab=LocalSettingsSectionFocus.current; val enterPane=LocalSettingsPaneEntry.current
 val open={ tab?.requestFocus();adding=true }; val close={ enterPane();adding=false }
 BackHandler(adding,onBack=close)
 if(adding) { addFlow(close);return }
 SettingsGroup(stringResource(R.string.settings_accounts)) {
  accounts.forEach { account ->
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    ServiceLogo(account.kind)
    Column {
     Text(account.serverName,style=MaterialTheme.typography.titleMedium);Text(account.userName)
     if(account.needsSignIn) Text(stringResource(R.string.account_sign_in_again),color=StovaColors.Caution)
    }
    if(account.needsSignIn) StovaButton(stringResource(R.string.account_sign_in_again),open)
    StovaButton(stringResource(R.string.account_sign_out),{ enterPane();onSignOut(account) })
   }
  }
  StovaButton(stringResource(R.string.account_add),open,primary=true)
 }
}
