package app.stova.ha

import app.stova.ha.cards.cardTypesFor
import app.stova.ha.cards.friendlyName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class HaEntityInfo(val name: String? = null, val room: String? = null, val hidden: Boolean = false)
@Serializable data class HaRegistry(val entities: Map<String, HaEntityInfo> = emptyMap())
data class HaCatalogItem(val entity: HaEntity, val name: String, val room: String?, val hidden: Boolean)
internal fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull

fun parseRegistry(areas: JsonArray, devices: JsonArray, entities: JsonArray): HaRegistry {
    val rooms = areas.filterIsInstance<JsonObject>().mapNotNull { j -> j.text("area_id")?.let { it to j.text("name") } }.toMap()
    val deviceRooms = devices.filterIsInstance<JsonObject>().mapNotNull { j -> j.text("id")?.let { it to j.text("area_id") } }.toMap()
    return HaRegistry(entities.filterIsInstance<JsonObject>().mapNotNull { j ->
        val id = j.text("entity_id") ?: return@mapNotNull null
        val areaId = j.text("area_id") ?: deviceRooms[j.text("device_id")]
        id to HaEntityInfo(j.text("name"), rooms[areaId],
            j.text("hidden_by") != null || j.text("disabled_by") != null || j.text("entity_category") in setOf("config", "diagnostic"))
    }.toMap())
}

fun entityCatalog(states: List<HaEntity>, registry: HaRegistry): List<HaCatalogItem> = states
    .filter { cardTypesFor(it).isNotEmpty() }.map { entity ->
        val info = registry.entities[entity.id]
        HaCatalogItem(entity, info?.name?.takeIf(String::isNotBlank) ?: friendlyName(entity), info?.room, info?.hidden == true)
    }.sortedWith(compareBy<HaCatalogItem> { it.room == null }.thenBy { it.room?.lowercase().orEmpty() }.thenBy { it.name.lowercase() })

@Serializable data class ForecastDay(val time: Long, val condition: String, val high: Double, val low: Double? = null)
fun parseForecast(result: JsonObject, entityId: String): List<ForecastDay> {
    val response = result["response"] as? JsonObject ?: return emptyList()
    val forecast = (response[entityId] as? JsonObject)?.get("forecast") as? JsonArray ?: return emptyList()
    return forecast.mapNotNull { raw ->
        val j = raw as? JsonObject ?: return@mapNotNull null
        val time = j.text("datetime")?.let { runCatching { haMillis(it) }.getOrNull() } ?: return@mapNotNull null
        val high = (j["temperature"] as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite) ?: return@mapNotNull null
        ForecastDay(time, j.text("condition").orEmpty(), high, (j["templow"] as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite))
    }.sortedBy { it.time }.take(5)
}
