package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
@Composable fun ClimateCard(entity:HaEntity) {
    val locale=LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {CardValue(entity.attributes.number("current_temperature")?.let { formatNumber(it,locale) } ?: entityStateLabel(entity),stringResource(R.string.ha_celsius))}
        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.small).padding(6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(when(entity.state) {"heat"->StovaIcons.Sun;"cool"->StovaIcons.Snow;"auto","heat_cool"->StovaIcons.Home;else->StovaIcons.Toggle},null,Modifier.size(14.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entityStateLabel(entity),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    entity.attributes.number("temperature")?.let {
        CardDetail(stringResource(if(LocalCardFocused.current) R.string.ha_v2_target_adjust else R.string.ha_target_value,formatNumber(it,locale)))
    }
}
