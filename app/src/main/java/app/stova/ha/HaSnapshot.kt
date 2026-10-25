package app.stova.ha

import app.stova.ha.cards.CardConfig
import app.stova.ha.cards.CardType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class HaSnapshot(val profileId: String? = null, val entities: Map<String, HaEntity> = emptyMap())

/** Persist display data only. In particular entity_picture can contain a camera bearer token. */
internal fun snapshotEntities(cards: List<CardConfig>, entities: Map<String, HaEntity>): Map<String, HaEntity> =
    cards.groupBy(CardConfig::entityId).mapNotNull { (id, configs) ->
        val entity = entities[id] ?: return@mapNotNull null
        val keys = setOf("friendly_name") + configs.flatMap { config ->
            when (config.type) {
                CardType.SENSOR, CardType.ENERGY_PRICE -> listOf("unit_of_measurement")
                CardType.WEATHER -> listOf("temperature", "temperature_unit")
                CardType.CLIMATE -> listOf("current_temperature", "temperature", "min_temp", "max_temp")
                CardType.LIGHT -> listOf("brightness")
                CardType.MEDIA_PLAYER -> listOf("media_title", "media_artist")
                else -> emptyList()
            }
        }
        val attrs = entity.attributes.filter { (key, value) -> key in keys && value is JsonPrimitive }
            .toMutableMap()
        if (configs.any { it.type == CardType.ENERGY_PRICE }) {
            // The graph only needs numbers, never arbitrary nested HA attributes.
            attrs["today"] = JsonArray(app.stova.ha.cards.priceCurve(entity).map(::JsonPrimitive))
        }
        id to entity.copy(attributes = JsonObject(attrs))
    }.toMap()
