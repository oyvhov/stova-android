package app.stova.ha

import app.stova.core.storage.StovaJson
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.*

fun authMessage(token: String) = buildJsonObject { put("type", "auth"); put("access_token", token) }.toString()
private fun command(id: Int, type: String, extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject { put("id", id); put("type", type); extra() }.toString()
fun supportedFeatures(id: Int) = command(id, "supported_features") { putJsonObject("features") { put("coalesce_messages", 1) } }
fun subscribeEntities(id: Int, ids: Set<String>) = command(id, "subscribe_entities") { putJsonArray("entity_ids") { ids.sorted().forEach { add(it) } } }
fun unsubscribe(id: Int, subscription: Int) = command(id, "unsubscribe_events") { put("subscription", subscription) }
fun callService(id: Int, domain: String, service: String, entityId: String, data: JsonObject, returnResponse: Boolean = false) = command(id, "call_service") {
    put("domain", domain); put("service", service); put("service_data", data); putJsonObject("target") { put("entity_id", entityId) }
    if (returnResponse) put("return_response", true)
}
enum class HaRegistryKind(val endpoint: String) { AREA("area"), DEVICE("device"), ENTITY("entity") }
fun registryRequest(id: Int, kind: HaRegistryKind) = command(id, "config/${kind.endpoint}_registry/list")
fun forecastRequest(id: Int, entityId: String) = callService(id, "weather", "get_forecasts", entityId,
    buildJsonObject { put("type", "daily") }, returnResponse = true)
fun historyRequest(id: Int, entityId: String, start: Instant) = command(id, "history/history_during_period") {
    put("start_time", start.toString()); putJsonArray("entity_ids") { add(entityId) }
    put("minimal_response", true); put("no_attributes", true); put("significant_changes_only", false)
}
fun getStates(id: Int) = command(id, "get_states")
fun splitIncoming(text: String): List<JsonObject> = when (val json = StovaJson.parseToJsonElement(text)) {
    is JsonObject -> listOf(json)
    is JsonArray -> json.filterIsInstance<JsonObject>()
    else -> emptyList()
}
fun parseHistory(result: JsonObject, entityId: String): List<Pair<Long, Double>> =
    (result[entityId] as? JsonArray).orEmpty().mapNotNull { raw ->
        val j = raw as? JsonObject ?: return@mapNotNull null
        val value = (j["s"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() } ?: return@mapNotNull null
        val time = ((j["lu"] ?: j["lc"]) as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        (time * 1000).toLong() to value
    }
/** HA sends "+00:00"; Android 11's Instant.parse accepts only "Z", so parse the offset explicitly. */
internal fun haMillis(text: String): Long = OffsetDateTime.parse(text).toInstant().toEpochMilli()
fun parseStates(result: JsonArray): List<HaEntity> = result.mapNotNull { raw -> runCatching {
    val j = raw.jsonObject
    val changed = haMillis(j.getValue("last_changed").jsonPrimitive.content)
    HaEntity(j.getValue("entity_id").jsonPrimitive.content, j.getValue("state").jsonPrimitive.content,
        j["attributes"] as? JsonObject ?: JsonObject(emptyMap()), changed,
        j["last_updated"]?.jsonPrimitive?.content?.let(::haMillis) ?: changed)
}.getOrNull() }
