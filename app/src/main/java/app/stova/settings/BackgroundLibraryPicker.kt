package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.media.BackgroundLibraryCatalog
import app.stova.profiles.BackgroundLibraryRef
import kotlinx.coroutines.CancellationException

@Composable
internal fun BackgroundLibraryPicker(selected:Set<BackgroundLibraryRef>?,load:suspend ()->BackgroundLibraryCatalog,
    onSave:(Set<BackgroundLibraryRef>?)->Unit,onDismiss:()->Unit) {
    var draft by remember { mutableStateOf(selected) }
    var catalog by remember { mutableStateOf<BackgroundLibraryCatalog?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val loader by rememberUpdatedState(load)
    val first=remember { FocusRequester() }
    val density=LocalDensity.current
    fun retryLoad() {first.requestFocus();retry++}
    LaunchedEffect(retry) {
        catalog=null;failed=false
        try { catalog=loader() } catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true}
    }
    StovaDialog(onDismiss) {
      CompositionLocalProvider(LocalDensity provides density) {
        Column(Modifier.widthIn(max=680.dp).fillMaxWidth().heightIn(max=440.dp)
            .background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PageHeader(stringResource(R.string.background_libraries),StovaIcons.Rows)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.background_libraries_all),{draft=null},Modifier.focusRequester(first),compact=true,quiet=true)
                StovaButton(stringResource(R.string.background_libraries_clear),{draft=emptySet()},compact=true,quiet=true)
            }
            LaunchedEffect(Unit) {first.requestFocus()}
            LazyColumn(Modifier.weight(1f,fill=false),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                item { Text(librarySelectionLabel(draft),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                val state=catalog
                when {
                    failed -> item {Text(stringResource(R.string.custom_library_failed));StovaButton(stringResource(R.string.custom_retry),::retryLoad,compact=true)}
                    state==null -> item {Text(stringResource(R.string.custom_library_loading))}
                    else -> {
                        if(!state.hasAccounts) item {Text(stringResource(R.string.background_no_accounts))}
                        else if(state.libraries.isEmpty() && state.failedAccounts.isEmpty()) item {Text(stringResource(R.string.background_no_libraries))}
                        if(state.failedAccounts.isNotEmpty()) item {
                            Text(stringResource(R.string.background_libraries_failed,state.failedAccounts.joinToString(", ")),color=MaterialTheme.colorScheme.onSurfaceVariant)
                            StovaButton(stringResource(R.string.custom_retry),::retryLoad,compact=true)
                        }
                        val unavailable=draft.orEmpty().count { ref->state.libraries.none { it.ref==ref } }
                        if(unavailable>0) item {Text(pluralStringResource(R.plurals.background_libraries_unavailable,unavailable,unavailable),style=MaterialTheme.typography.bodySmall)}
                        state.libraries.groupBy { it.accountName }.forEach { (account,libraries)->
                            item {Text(account,Modifier.padding(top=8.dp,start=16.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                            items(libraries,key={"${it.ref.accountId}:${it.ref.libraryId}"}) { library->
                                SettingsSwitchRow(library.name,draft==null || library.ref in draft.orEmpty(),{checked->
                                    val base=draft ?: state.libraries.mapTo(mutableSetOf()) {it.ref}
                                    draft=if(checked) base+library.ref else base-library.ref
                                })
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.profile_cancel),onDismiss,compact=true,quiet=true)
                StovaButton(stringResource(R.string.background_libraries_save),{onSave(draft)},compact=true)
            }
        }
      }
    }
}

@Composable internal fun librarySelectionLabel(selected:Set<BackgroundLibraryRef>?):String = when {
    selected==null->stringResource(R.string.background_libraries_all)
    selected.isEmpty()->stringResource(R.string.background_libraries_none)
    else->pluralStringResource(R.plurals.background_libraries_count,selected.size,selected.size)
}
