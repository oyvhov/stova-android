package app.stova.media.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.StovaButton
@Composable fun SpoleMissingDialog(onDismiss:()->Unit) {
 AlertDialog(onDismissRequest=onDismiss,text={Text(stringResource(R.string.spole_missing))},confirmButton={StovaButton(stringResource(R.string.spole_close),onDismiss)})
}
