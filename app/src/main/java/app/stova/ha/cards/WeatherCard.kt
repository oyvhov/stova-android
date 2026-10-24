package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
fun weatherIcon(state:String) = when(state) {
    "sunny" -> StovaIcons.Sun; "clear-night" -> StovaIcons.Moon
    "cloudy","partlycloudy" -> StovaIcons.Cloud; "fog" -> StovaIcons.Fog
    "hail","snowy","snowy-rainy" -> StovaIcons.Snow
    "lightning","lightning-rainy","exceptional" -> StovaIcons.Lightning
    "rainy","pouring" -> StovaIcons.Rain
    "windy","windy-variant" -> StovaIcons.Wind
    else -> StovaIcons.Cloud
}
@Composable fun WeatherCard(entity:HaEntity) {
    CardValue(entity.attributes.number("temperature")?.let { formatNumber(it,LocalConfiguration.current.locales[0]) } ?: stringResource(R.string.ha_dash), entity.attributes["temperature_unit"]?.jsonPrimitive?.contentOrNull)
}
