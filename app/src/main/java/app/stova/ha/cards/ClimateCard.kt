package app.stova.ha.cards
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
@Composable fun ClimateCard(entity:HaEntity) {
    val locale=LocalConfiguration.current.locales[0]
    CardValue(entity.attributes.number("current_temperature")?.let { formatNumber(it,locale) } ?: entity.state,stringResource(R.string.ha_celsius))
    entity.attributes.number("temperature")?.let { CardValue(stringResource(R.string.ha_target_value,formatNumber(it,locale))) }
}
