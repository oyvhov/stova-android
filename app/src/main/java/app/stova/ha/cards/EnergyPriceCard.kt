package app.stova.ha.cards

import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.ha.HaEntity
import app.stova.ha.text
import app.stova.core.theme.StovaColors
import java.time.*

@Composable fun EnergyPriceCard(entity:HaEntity,zone:ZoneId=ZoneId.systemDefault()) {
    StateValue(entity)
    PriceChart(entity,modifier=Modifier.height(34.dp),detailed=false,zone=zone)
}

@Composable fun PriceChart(entity:HaEntity,tomorrow:Boolean=false,modifier:Modifier=Modifier,
    quarters:Boolean=false,detailed:Boolean=true,zone:ZoneId=ZoneId.systemDefault()) {
    val now by rememberResumedNow(60_000)
    val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val points=remember(entity,tomorrow,quarters,zone,date) {
        val raw=pricePoints(entity,tomorrow,date,zone)
        if(quarters) raw else hourlyPrices(raw,zone)
    }
    if(points.isEmpty()) {CardDetail(stringResource(if(tomorrow) R.string.ha_v2_no_tomorrow else R.string.ha_no_history));return}
    val locale=LocalConfiguration.current.locales[0]
    val unit=entity.attributes.text("unit_of_measurement").orEmpty()
    val accent=MaterialTheme.colorScheme.primary
    val lowest=points.minBy {it.value};val highest=points.maxBy {it.value}
    val day=if(tomorrow) date.plusDays(1) else date
    val begin=day.atStartOfDay(zone).toInstant().toEpochMilli()
    val duration=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-begin
    if(detailed) {
        points.find {now>=it.start && now<it.end}?.let {
            Text(stringResource(R.string.ha_v2_chart_now,formatNumber(it.value,locale),unit),style=MaterialTheme.typography.titleMedium,color=accent)
        }
    }
    Canvas(modifier.fillMaxWidth()) {
        val min=minOf(0.0,lowest.value);val max=maxOf(0.0,highest.value);val range=(max-min).coerceAtLeast(.001)
        val zero=(size.height*max/range).toFloat()
        points.forEach {p ->
            val x=size.width*(p.start-begin)/duration
            val width=size.width*(p.end-p.start)/duration
            val y=(size.height*(max-p.value)/range).toFloat()
            val current=now>=p.start && now<p.end
            val level=when(priceLevel(p.value,lowest.value,highest.value)) {
                PriceLevel.LOW->StovaColors.Success;PriceLevel.MEDIUM->StovaColors.Caution;PriceLevel.HIGH->StovaColors.Warning
            }
            drawRoundRect(if(current) accent else level.copy(alpha=.72f),Offset(x,minOf(y,zero)),
                Size((width-(if(quarters) 1.dp else 3.dp).toPx()).coerceAtLeast(1f),kotlin.math.abs(zero-y).coerceAtLeast(2.dp.toPx())),CornerRadius(4.dp.toPx()))
            if(current && detailed) drawLine(accent,Offset(x+width/2,0f),Offset(x+width/2,size.height),1.dp.toPx())
        }
    }
    if(detailed) {
        val ticks=remember(day,zone) {priceAxisFractions(day,zone)}
        Layout(modifier=Modifier.fillMaxWidth(),content={
            (0..24 step 6).forEach {Text(it.toString().padStart(2,'0'),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }) {measurables,constraints ->
            val labels=measurables.map {it.measure(constraints.copy(minWidth=0,minHeight=0))}
            layout(constraints.maxWidth,labels.maxOf {it.height}) {
                labels.forEachIndexed {index,label ->
                    label.placeRelative((ticks[index]*constraints.maxWidth-label.width/2).toInt().coerceIn(0,(constraints.maxWidth-label.width).coerceAtLeast(0)),0)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column {CardDetail(stringResource(R.string.ha_v2_cheapest,priceWindow(lowest,zone,quarters)));CardDetail("${formatNumber(lowest.value,locale)} $unit")}
            Column {CardDetail(stringResource(R.string.ha_v2_dearest,priceWindow(highest,zone,quarters)));CardDetail("${formatNumber(highest.value,locale)} $unit")}
        }
    }
}
