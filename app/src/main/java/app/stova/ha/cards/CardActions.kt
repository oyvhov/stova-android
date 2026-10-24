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
