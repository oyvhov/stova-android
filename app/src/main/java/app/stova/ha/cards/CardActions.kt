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
fun brightnessCall(percent: Int) = ServiceCall("light", "turn_on", buildJsonObject { put("brightness_pct", percent.coerceIn(1,100)) })
fun climateCall(entity: HaEntity, delta: Double): ServiceCall {
    val min = entity.attributes.number("min_temp") ?: 5.0
    val max = (entity.attributes.number("max_temp") ?: 35.0).coerceAtLeast(min)
    val current = entity.attributes.number("temperature") ?: entity.attributes.number("current_temperature") ?: min
    return ServiceCall("climate", "set_temperature", buildJsonObject { put("temperature", (round((current + delta) * 2) / 2).coerceIn(min,max)) })
}
internal fun JsonObject.number(key: String): Double? = (get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

/** Predict only state/attributes affected by this call. Server snapshots remain authoritative. */
fun optimisticEntity(entity: HaEntity, call: ServiceCall): HaEntity = when (call.service) {
    "toggle" -> entity.copy(state=if(entity.state=="on") "off" else "on")
    "turn_on", "turn_off" -> if(entity.id.substringBefore('.') in setOf("scene","script")) entity else entity.copy(state=if(call.service=="turn_on") "on" else "off",
        attributes=if(call.data.number("brightness_pct")!=null) JsonObject(entity.attributes + ("brightness" to JsonPrimitive(call.data.number("brightness_pct")!! * 255 / 100))) else entity.attributes)
    "media_play_pause" -> entity.copy(state=if(entity.state=="playing") "paused" else "playing")
    "media_play" -> entity.copy(state="playing")
    "media_pause" -> entity.copy(state="paused")
    "set_hvac_mode" -> entity.copy(state=call.data["hvac_mode"]?.jsonPrimitive?.content ?: entity.state)
    "set_cover_position" -> entity.copy(attributes=JsonObject(entity.attributes+("current_position" to call.data.getValue("position"))))
    "set_temperature", "volume_set", "set_percentage", "set_direction", "set_preset_mode", "select_source" -> entity.copy(attributes=JsonObject(entity.attributes+call.data))
    "lock" -> entity.copy(state="locked")
    "unlock" -> entity.copy(state="unlocked")
    else -> entity
}
