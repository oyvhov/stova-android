package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.stova.ha.text
@Composable fun LightCard(entity:HaEntity) {
    CardValue(entityStateLabel(entity))
    entity.attributes.number("brightness")?.takeIf {entity.state=="on"}?.let {brightness ->
        val percent=(brightness/255*100).toInt().coerceIn(0,100)
        CardDetail(stringResource(R.string.ha_v2_brightness,percent))
        LinearProgressIndicator(progress={percent/100f},modifier=Modifier.fillMaxWidth().height(3.dp),color=app.stova.core.theme.StovaColors.Caution,trackColor=MaterialTheme.colorScheme.surfaceVariant)
    }
    entity.attributes.number("color_temp_kelvin")?.takeIf {entity.state=="on"}?.let {CardDetail(stringResource(R.string.ha_v2_color_temp,it.toInt()))}
}
