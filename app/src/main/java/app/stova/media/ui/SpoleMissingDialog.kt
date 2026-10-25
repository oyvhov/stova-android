package app.stova.media.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaDialog
@Composable fun SpoleMissingDialog(onDismiss:()->Unit) {
 StovaDialog(onDismiss) {
  Column(Modifier.widthIn(max=480.dp).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(stringResource(R.string.spole_missing),style=MaterialTheme.typography.bodyLarge)
   StovaButton(stringResource(R.string.spole_close),onDismiss)
  }
 }
}
