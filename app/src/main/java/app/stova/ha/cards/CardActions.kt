package app.stova.ha.cards

import app.stova.ha.HaEntity
import app.stova.profiles.Profile
import kotlin.math.round
import kotlinx.serialization.json.*

data class ServiceCall(val domain: String, val service: String, val data: JsonObject = JsonObject(emptyMap()))
fun canControl(profile: Profile) = !profile.kid || profile.kidSmartHomeControl
fun primaryAction(type: CardType, entity: HaEntity): ServiceCall? = when (type) {
    CardType.LIGHT -> ServiceCall("light", "toggle")
    CardType.TOGGLE -> ServiceCall("homeassistant", "toggle")
    CardType.MEDIA_PLAYER -> ServiceCall("media_player", "media_play_pause")
    CardType.SCENE -> ServiceCall(entity.id.substringBefore('.'), "turn_on")
    else -> null
}
fun longAction(type: CardType, entity: HaEntity): ServiceCall? =
    if (type == CardType.MEDIA_PLAYER) ServiceCall("media_player", "media_next_track") else null
fun brightnessCall(percent: Int) = ServiceCall("light", "turn_on", buildJsonObject { put("brightness_pct", percent.coerceIn(1,100)) })
fun climateCall(entity: HaEntity, delta: Double): ServiceCall {
    val min = entity.attributes.number("min_temp") ?: 5.0
    val max = (entity.attributes.number("max_temp") ?: 35.0).coerceAtLeast(min)
    val current = entity.attributes.number("temperature") ?: entity.attributes.number("current_temperature") ?: min
    return ServiceCall("climate", "set_temperature", buildJsonObject { put("temperature", (round((current + delta) * 2) / 2).coerceIn(min,max)) })
}
internal fun JsonObject.number(key: String): Double? = (get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

fun directionAction(type: CardType, entity: HaEntity, direction: Int): ServiceCall? = when(type) {
    CardType.LIGHT -> {
        val modes = (entity.attributes["supported_color_modes"] as? JsonArray)?.map { it.jsonPrimitive.content }
        if (modes == listOf("onoff")) null else brightnessCall(((entity.attributes.number("brightness") ?: 0.0) / 255 * 100).toInt() + direction * 10)
    }
    CardType.CLIMATE -> climateCall(entity,direction * 0.5)
    CardType.MEDIA_PLAYER -> entity.attributes.number("volume_level")?.let { volume ->
        ServiceCall("media_player","volume_set",buildJsonObject { put("volume_level",(volume + direction * 0.05).coerceIn(0.0,1.0)) })
    }
    CardType.COVER -> if (((entity.attributes.number("supported_features") ?: 0.0).toInt() and 4) == 0) null else
        ServiceCall("cover","set_cover_position", buildJsonObject { put("position",((entity.attributes.number("current_position") ?: 0.0) + direction * 10).toInt().coerceIn(0,100)) })
    else -> null
}

/** Predict only state/attributes affected by this call. Server snapshots remain authoritative. */
fun optimisticEntity(entity: HaEntity, call: ServiceCall): HaEntity = when (call.service) {
    "toggle" -> entity.copy(state=if(entity.state=="on") "off" else "on")
    "turn_on", "turn_off" -> entity.copy(state=if(call.service=="turn_on") "on" else "off",
        attributes=if(call.data.number("brightness_pct")!=null) JsonObject(entity.attributes + ("brightness" to JsonPrimitive(call.data.number("brightness_pct")!! * 255 / 100))) else entity.attributes)
    "set_temperature", "volume_set" -> entity.copy(attributes=JsonObject(entity.attributes+call.data))
    else -> entity
}
