package app.stova.media.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaDialog
import app.stova.media.model.MediaItem
@Composable fun ItemActionsMenu(item:MediaItem,onPlay:()->Unit,onDetails:()->Unit,onDismiss:()->Unit,rowActions: @Composable () -> Unit = {}) {
 StovaDialog(onDismiss) {
  Surface(shape=MaterialTheme.shapes.large) {
   Column(Modifier.width(440.dp).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text(item.title,style=MaterialTheme.typography.headlineSmall)
    StovaButton(stringResource(R.string.item_play),onPlay,primary=true)
    StovaButton(stringResource(R.string.item_details),onDetails)
    rowActions()
   }
  }
 }
}
