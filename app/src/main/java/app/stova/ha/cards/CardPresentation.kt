package app.stova.ha.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
import app.stova.ha.text

@Composable fun entityStateLabel(entity:HaEntity?):String {
    if(entity==null || stateText(entity)==StateText.Unavailable) return stringResource(R.string.ha_unavailable)
    val resource=when(entity.state) {
        "on" -> if(entity.id.startsWith("binary_sensor.")) when(entity.attributes.text("device_class")) {
            "door","window","opening","garage_door" -> R.string.ha_v2_open
            "motion","occupancy","presence" -> R.string.ha_v2_motion
            "lock" -> R.string.ha_v2_unlocked; else -> R.string.ha_v2_detected
        } else R.string.ha_on
        "off" -> if(entity.id.startsWith("binary_sensor.")) when(entity.attributes.text("device_class")) {
            "door","window","opening","garage_door" -> R.string.ha_v2_closed
            "motion","occupancy","presence" -> R.string.ha_v2_clear
            "lock" -> R.string.ha_v2_locked; else -> R.string.ha_v2_not_detected
        } else R.string.ha_off
        "open","opening" -> R.string.ha_v2_open; "closed","closing" -> R.string.ha_v2_closed
        "locked" -> R.string.ha_v2_locked; "unlocked" -> R.string.ha_v2_unlocked
        "heat","heating" -> R.string.ha_v2_heat; "cool","cooling" -> R.string.ha_v2_cool
        "auto","heat_cool" -> R.string.ha_v2_auto; "idle","standby" -> R.string.ha_v2_idle
        "playing" -> R.string.ha_v2_playing; "paused" -> R.string.ha_v2_paused
        "sunny" -> R.string.ha_v2_condition_sunny; "cloudy" -> R.string.ha_v2_condition_cloudy
        "partlycloudy" -> R.string.ha_v2_condition_partlycloudy; "rainy","pouring" -> R.string.ha_v2_condition_rain
        "snowy","snowy-rainy","hail" -> R.string.ha_v2_condition_snow; "windy","windy-variant" -> R.string.ha_v2_condition_wind
        "fog" -> R.string.ha_v2_condition_fog; "lightning","lightning-rainy","exceptional" -> R.string.ha_v2_condition_lightning
        "clear-night" -> R.string.ha_v2_condition_night; else -> null
    }
    return resource?.let {stringResource(it)} ?: when(val text=stateText(entity)) {
        is StateText.Number -> listOfNotNull(formatNumber(text.value,LocalConfiguration.current.locales[0]),text.unit).joinToString(" ")
        else -> entity.state
    }
}
