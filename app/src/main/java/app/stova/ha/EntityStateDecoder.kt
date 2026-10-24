package app.stova.ha

import kotlinx.serialization.json.*

data class HaEntity(val id: String, val state: String, val attributes: JsonObject, val lastChanged: Long, val lastUpdated: Long)

object EntityStateDecoder {
    fun apply(current: Map<String, HaEntity>, event: JsonObject): Map<String, HaEntity> {
        val next = current.toMutableMap()
        (event["a"] as? JsonObject)?.forEach { (id, raw) ->
            val value = raw.jsonObject
            val changed = millis(value["lc"]) ?: 0L
            next[id] = HaEntity(id, value["s"]?.jsonPrimitive?.content.orEmpty(), value["a"] as? JsonObject ?: JsonObject(emptyMap()), changed, millis(value["lu"]) ?: changed)
        }
        (event["c"] as? JsonObject)?.forEach { (id, raw) ->
            val old = next[id] ?: return@forEach
            val plus = raw.jsonObject["+"] as? JsonObject ?: JsonObject(emptyMap())
            val attrs = old.attributes.toMutableMap()
            (plus["a"] as? JsonObject)?.let { attrs.putAll(it) }
            ((raw.jsonObject["-"] as? JsonObject)?.get("a") as? JsonArray)?.forEach { attrs.remove(it.jsonPrimitive.content) }
            next[id] = old.copy(state = plus["s"]?.jsonPrimitive?.content ?: old.state,
                attributes = JsonObject(attrs), lastChanged = millis(plus["lc"]) ?: old.lastChanged,
                lastUpdated = millis(plus["lu"]) ?: millis(plus["lc"]) ?: old.lastUpdated)
        }
        (event["r"] as? JsonArray)?.forEach { next.remove(it.jsonPrimitive.content) }
        return next
    }
    private fun millis(value: JsonElement?): Long? = (value as? JsonPrimitive)?.doubleOrNull?.times(1000)?.toLong()
}
