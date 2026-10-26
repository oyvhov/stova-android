package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaColors
import app.stova.ha.HaStatus

@Composable fun HaStatusLabel(status:HaStatus) {
    androidx.compose.animation.Crossfade(status, animationSpec = app.stova.core.theme.stovaMotion.fade(), label = "ha-status") { shown ->
    val label=stringResource(when(shown) {
        HaStatus.CONNECTED -> R.string.ha_connected
        HaStatus.CONNECTING -> R.string.ha_connecting
        HaStatus.DISCONNECTED -> R.string.ha_disconnected
        HaStatus.SIGNED_OUT -> R.string.ha_signed_out
        HaStatus.NOT_CONFIGURED -> R.string.ha_not_connected
    })
    val neutral=MaterialTheme.colorScheme.onSurfaceVariant
    if(shown==HaStatus.CONNECTED || shown==HaStatus.CONNECTING) {
        // Keep the state available to screen readers without another visible label or focus stop.
        Box(Modifier.size(14.dp).semantics {contentDescription=label},contentAlignment=Alignment.Center) {
            if(shown==HaStatus.CONNECTED) Box(Modifier.size(6.dp).background(neutral,CircleShape))
            else Box(Modifier.size(8.dp).border(1.dp,neutral,CircleShape))
        }
    } else Text(label,style=MaterialTheme.typography.labelSmall,
        color=if(shown==HaStatus.SIGNED_OUT) StovaColors.Caution else neutral)
    }
}
