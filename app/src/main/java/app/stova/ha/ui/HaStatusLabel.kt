package app.stova.ha.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.theme.StovaColors
import app.stova.ha.HaStatus

@Composable fun HaStatusLabel(status:HaStatus) {
    androidx.compose.animation.Crossfade(status, animationSpec = app.stova.core.theme.stovaMotion.fade(), label = "ha-status") { shown ->
    Text(stringResource(when(shown) {
        HaStatus.CONNECTED -> R.string.ha_connected
        HaStatus.CONNECTING -> R.string.ha_connecting
        HaStatus.DISCONNECTED -> R.string.ha_disconnected
        HaStatus.SIGNED_OUT -> R.string.ha_signed_out
        HaStatus.NOT_CONFIGURED -> R.string.ha_not_connected
    }),style=MaterialTheme.typography.labelLarge,color=if(shown==HaStatus.SIGNED_OUT) StovaColors.Caution else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
