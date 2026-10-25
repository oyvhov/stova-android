package app.stova.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.StovaButton

/** What an adult can do with one app. Every choice closes the menu. */
@Composable
fun AppActionsMenu(
    app: InstalledApp,
    favorite: Boolean,
    hidden: Boolean,
    onToggleFavorite: () -> Unit,
    onMove: (Int) -> Unit,
    onToggleHidden: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
    onChangeIcon: () -> Unit = {},
    rowActions: @Composable () -> Unit = {},
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(360.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(StovaShapes.ExtraLarge))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(app.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            val full = Modifier.fillMaxWidth()
            StovaButton(
                stringResource(if (favorite) R.string.app_remove_favorite else R.string.app_add_favorite),
                { onToggleFavorite(); onDismiss() },
                full.focusRequester(first),
            )
            if (favorite) {
                StovaButton(stringResource(R.string.app_move_left), { onMove(-1); onDismiss() }, full)
                StovaButton(stringResource(R.string.app_move_right), { onMove(+1); onDismiss() }, full)
            }
            StovaButton(stringResource(if (hidden) R.string.app_show else R.string.app_hide), { onToggleHidden(); onDismiss() }, full)
            StovaButton(stringResource(R.string.app_info), { onAppInfo(); onDismiss() }, full)
            StovaButton(stringResource(R.string.icon_change), onChangeIcon, full)
            rowActions()
        }
    }
}
