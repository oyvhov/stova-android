package app.stova.ha.cards
import androidx.compose.runtime.*
import app.stova.ha.HaEntity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import kotlinx.coroutines.delay

@Composable fun EnergyPriceCard(entity:HaEntity) {StateValue(entity);PriceChart(entity,modifier=Modifier.height(42.dp))}

@Composable fun PriceChart(entity:HaEntity,tomorrow:Boolean=false,modifier:Modifier=Modifier) {
    val points=remember(entity,tomorrow) {pricePoints(entity,tomorrow)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {delay(60_000);now=System.currentTimeMillis()}}
    if(points.isEmpty()) {CardDetail(stringResource(if(tomorrow) R.string.ha_v2_no_tomorrow else R.string.ha_no_history));return}
    val text=MaterialTheme.colorScheme.onSurface;val muted=MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.fillMaxWidth()) {
        val min=minOf(0.0,points.minOf {it.value});val max=maxOf(0.0,points.maxOf {it.value});val range=(max-min).coerceAtLeast(.001)
        val zero=(size.height*max/range).toFloat();val width=size.width/points.size
        points.forEachIndexed {i,p ->
            val y=(size.height*(max-p.value)/range).toFloat()
            drawRect(if(now>=p.start && now<p.end) text else muted.copy(alpha=.55f),Offset(i*width,minOf(y,zero)),Size((width-2.dp.toPx()).coerceAtLeast(1f),(kotlin.math.abs(zero-y)).coerceAtLeast(1f)))
        }
    }
    val locale=LocalConfiguration.current.locales[0]
    CardDetail(stringResource(R.string.ha_v2_minmax,formatNumber(points.minOf {it.value},locale),formatNumber(points.maxOf {it.value},locale)))
}
