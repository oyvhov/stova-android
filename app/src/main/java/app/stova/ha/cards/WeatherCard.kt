package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.stova.ha.ForecastDay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
fun weatherIcon(state:String) = when(state) {
    "sunny" -> StovaIcons.Sun; "clear-night" -> StovaIcons.Moon
    "cloudy","partlycloudy" -> StovaIcons.Cloud; "fog" -> StovaIcons.Fog
    "hail","snowy","snowy-rainy" -> StovaIcons.Snow
    "lightning","lightning-rainy","exceptional" -> StovaIcons.Lightning
    "rainy","pouring" -> StovaIcons.Rain
    "windy","windy-variant" -> StovaIcons.Wind
    else -> StovaIcons.Cloud
}
@Composable fun WeatherCard(entity:HaEntity,forecast:List<ForecastDay> = emptyList(),size:CardSize=CardSize.S,zone:ZoneId=ZoneId.systemDefault()) {
    if(size==CardSize.S) {
        CurrentWeather(entity)
        ForecastStrip(forecast,size,zone)
    } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1.3f),verticalArrangement=Arrangement.spacedBy(4.dp)) {CurrentWeather(entity)}
        Box(Modifier.weight(3f)) {ForecastStrip(forecast,size,zone)}
    }
}

@Composable private fun CurrentWeather(entity:HaEntity) {
    CardValue(entity.attributes.number("temperature")?.let { formatNumber(it,LocalConfiguration.current.locales[0]) } ?: stringResource(R.string.ha_dash), entity.attributes["temperature_unit"]?.jsonPrimitive?.contentOrNull)
    CardDetail(entityStateLabel(entity))
}

@Composable private fun ForecastStrip(forecast:List<ForecastDay>,size:CardSize,zone:ZoneId) {
    if(forecast.isEmpty()) CardDetail(stringResource(R.string.ha_v2_forecast_missing)) else {
        val locale=LocalConfiguration.current.locales[0]
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            forecast.take(5).forEach {day -> Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(Instant.ofEpochMilli(day.time).atZone(zone).format(DateTimeFormatter.ofPattern("EEE",locale)),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(weatherIcon(day.condition),null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatNumber(day.high,locale)+"°",style=MaterialTheme.typography.labelMedium)
                if(size!=CardSize.S) day.low?.let {Text(formatNumber(it,locale)+"°",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }}
        }
    }
}
