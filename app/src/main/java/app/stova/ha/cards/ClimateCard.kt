package app.stova.ha.cards
import androidx.compose.runtime.Composable
import app.stova.core.theme.StovaColors
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Canvas
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
        val tone=when(entity.state) {"heat"->StovaColors.Ember;"cool"->StovaColors.Snow;else->MaterialTheme.colorScheme.primary}
        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.small).padding(horizontal=5.dp,vertical=3.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
            // A small gauge in the mode's own colour: filled to the room's temperature, a dot at the target.
            Box(Modifier.size(20.dp),contentAlignment=Alignment.Center) {
                ClimateArc(entity.attributes.number("current_temperature"),entity.attributes.number("temperature"),
                    entity.attributes.number("min_temp") ?: 10.0,entity.attributes.number("max_temp") ?: 30.0,tone,Modifier.matchParentSize())
                Icon(when(entity.state) {"heat"->StovaIcons.Sun;"cool"->StovaIcons.Snow;"auto","heat_cool"->StovaIcons.Home;else->StovaIcons.Toggle},null,Modifier.size(10.dp),tint=tone)
            }
            Text(entityStateLabel(entity),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    entity.attributes.number("temperature")?.let {
        CardDetail(stringResource(if(LocalCardFocused.current) R.string.ha_v2_target_adjust else R.string.ha_target_value,formatNumber(it,locale)))
    }
}

@Composable private fun ClimateArc(current:Double?,target:Double?,min:Double,max:Double,tone:androidx.compose.ui.graphics.Color,modifier:Modifier) {
    val track=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.25f)
    val dot=MaterialTheme.colorScheme.onSurface
    fun fraction(value:Double)=((value-min)/(max-min).coerceAtLeast(.1)).toFloat().coerceIn(0f,1f)
    Canvas(modifier.testTag("climate-arc")) {
        val stroke=2.dp.toPx()
        val inset=stroke/2
        val arcSize=androidx.compose.ui.geometry.Size(size.width-stroke,size.height-stroke)
        val topLeft=androidx.compose.ui.geometry.Offset(inset,inset)
        val style=androidx.compose.ui.graphics.drawscope.Stroke(stroke,cap=androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(track,135f,270f,false,topLeft,arcSize,style=style)
        if(current!=null) drawArc(tone,135f,270f*fraction(current),false,topLeft,arcSize,style=style)
        if(target!=null) {
            val angle=Math.toRadians(135.0+270.0*fraction(target))
            val r=arcSize.width/2
            drawCircle(dot,stroke*.9f,androidx.compose.ui.geometry.Offset(center.x+(r*kotlin.math.cos(angle)).toFloat(),center.y+(r*kotlin.math.sin(angle)).toFloat()))
        }
    }
}
