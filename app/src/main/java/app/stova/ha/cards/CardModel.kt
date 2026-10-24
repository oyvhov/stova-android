package app.stova.ha.cards

import app.stova.ha.HaEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable enum class CardType { LIGHT, TOGGLE, CLIMATE, SENSOR, WEATHER, ENERGY_PRICE, MEDIA_PLAYER, SCENE, CAMERA }
@Serializable data class CardConfig(val id: String, val type: CardType, val entityId: String, val wide: Boolean = false, val onHome: Boolean = false)

fun cardTypesFor(entity: HaEntity): List<CardType> = when (entity.id.substringBefore('.')) {
    "light" -> listOf(CardType.LIGHT)
    "switch", "input_boolean", "fan" -> listOf(CardType.TOGGLE)
    "climate" -> listOf(CardType.CLIMATE)
    "sensor" -> if (entity.attributes["raw_today"] is JsonArray || entity.attributes["today"] is JsonArray) listOf(CardType.ENERGY_PRICE, CardType.SENSOR) else listOf(CardType.SENSOR)
    "binary_sensor" -> listOf(CardType.SENSOR)
    "weather" -> listOf(CardType.WEATHER)
    "media_player" -> listOf(CardType.MEDIA_PLAYER)
    "scene", "script" -> listOf(CardType.SCENE)
    "camera" -> listOf(CardType.CAMERA)
    else -> emptyList()
}
