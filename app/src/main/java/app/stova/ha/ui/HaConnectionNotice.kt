package app.stova.ha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.ha.HaStatus

/** Cached readings remain useful, but never look like connected controls. */
@Composable
internal fun HaConnectionNotice(status: HaStatus, cached: Boolean, onRetry: () -> Unit,
    modifier: Modifier = Modifier, onSettings: (() -> Unit)? = null) {
    if (status == HaStatus.CONNECTED || status == HaStatus.NOT_CONFIGURED) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(when (status) {
                HaStatus.CONNECTING -> R.string.ha_connecting
                HaStatus.SIGNED_OUT -> R.string.ha_signed_out
                else -> R.string.ha_connection_unreachable
            }), style = MaterialTheme.typography.titleSmall)
            if (cached) Text(stringResource(R.string.ha_cached_values), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status != HaStatus.CONNECTING) {
            val signIn = status == HaStatus.SIGNED_OUT && onSettings != null
            StovaButton(stringResource(if (signIn) R.string.ha_signed_out else R.string.ha_v2_retry),
                if (signIn) onSettings else onRetry, compact = true)
        }
    }
}
