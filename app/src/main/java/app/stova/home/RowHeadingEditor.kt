package app.stova.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaDialog

/** Both Home's long-OK menu and Settings use the same controlled editor. Blank restores locale defaults. */
@Composable
internal fun RowHeadingEditor(value: String?, defaultTitle: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(value.orEmpty()) }
    val field = remember { FocusRequester() }
    StovaDialog(onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.width(420.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.custom_edit_heading), style = MaterialTheme.typography.headlineSmall)
                RemoteTextField(draft, { draft = it.take(80) }, stringResource(R.string.custom_heading_name),
                    Modifier.fillMaxWidth().focusRequester(field),
                    supportingText = stringResource(R.string.custom_heading_default, defaultTitle))
                StovaButton(stringResource(R.string.profile_save), { onSave(draft) }, Modifier.fillMaxWidth(), primary = true)
                StovaButton(stringResource(R.string.custom_heading_reset), { onSave("") }, Modifier.fillMaxWidth())
                StovaButton(stringResource(R.string.profile_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        }
        LaunchedEffect(Unit) { field.requestFocus() }
    }
}
