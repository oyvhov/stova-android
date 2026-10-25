package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.stova.ha.text
@Composable fun LightCard(entity:HaEntity) {
    val brightness=entity.attributes.number("brightness")?.takeIf {entity.state=="on"}
    val percent=brightness?.let {(it/255*100).toInt().coerceIn(0,100)}
    CardValue(percent?.let {stringResource(R.string.ha_percent,it)} ?: entityStateLabel(entity))
    if(entity.state=="on") {
        val glow=lightGlow(entity)
        val kelvin=entity.attributes.number("color_temp_kelvin")
            ?: entity.attributes.number("color_temp")?.takeIf {it>0}?.let {1_000_000/it}
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            if(glow.colored) Box(Modifier.size(10.dp).background(glow.color,CircleShape))
            else if(kelvin!=null) CardDetail(stringResource(temperatureWord(kelvin.toInt())))
        }
        if(percent!=null) LinearProgressIndicator(progress={percent/100f},modifier=Modifier.fillMaxWidth().height(3.dp),color=glow.color,trackColor=MaterialTheme.colorScheme.surfaceVariant,drawStopIndicator={})
    }
}
