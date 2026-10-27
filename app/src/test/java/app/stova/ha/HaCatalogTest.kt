package app.stova.ha

import app.stova.core.storage.StovaJson
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class HaCatalogTest {
    private fun array(text: String) = StovaJson.parseToJsonElement(text).jsonArray
    @Test fun registryCommandsUseHomeAssistantEndpoints() {
        assertEquals("config/area_registry/list", splitIncoming(registryRequest(4, HaRegistryKind.AREA)).single()["type"]!!.jsonPrimitive.content)
        assertEquals("config/device_registry/list", splitIncoming(registryRequest(5, HaRegistryKind.DEVICE)).single()["type"]!!.jsonPrimitive.content)
        assertEquals("config/entity_registry/list", splitIncoming(registryRequest(6, HaRegistryKind.ENTITY)).single()["type"]!!.jsonPrimitive.content)
    }
    @Test fun registryResolvesOverridesAndDeviceRoomsAndHiddenEntities() {
        val registry = parseRegistry(
            array("""[{"area_id":"living","name":"Stove"},{"area_id":"hall","name":"Gang"}]"""),
            array("""[{"id":"dev1","area_id":"living","name_by_user":"Lampe"}]"""),
            array("""[{"entity_id":"light.a","device_id":"dev1","area_id":null,"name":"Leselys","icon":"mdi:lamp","original_icon":"mdi:lightbulb","hidden_by":null,"disabled_by":null,"entity_category":null},{"entity_id":"sensor.b","device_id":"dev1","area_id":"hall","name":null,"original_name":"Batteri","entity_category":"diagnostic"},{"entity_id":"switch.c","hidden_by":"user"},{"entity_id":"sensor.d","disabled_by":"integration"}]"""))
        val entities = listOf("light.a", "sensor.b", "switch.c", "sensor.d").map { HaEntity(it,"on",JsonObject(emptyMap()),0,0) }
        val items = entityCatalog(entities, registry)
        assertEquals(listOf("sensor.b","light.a","sensor.d","switch.c"),items.map { it.entity.id })
        assertEquals("Gang",items.first().room)
        assertEquals("Leselys",items[1].name)
        assertEquals(listOf("mdi:lamp","mdi:lightbulb"),registry.entities["light.a"]!!.let { listOf(it.icon,it.originalIcon) })
        assertEquals("Stove",items[1].room)
        assertEquals(listOf("light.a"),items.filterNot {it.hidden}.map {it.entity.id})
    }
    @Test fun unknownRegistryEntityRemainsDiscoverable() {
        val item = entityCatalog(listOf(HaEntity("weather.home","cloudy",buildJsonObject { put("friendly_name","Heime") },0,0)),HaRegistry()).single()
        assertEquals("Heime",item.name); assertNull(item.room); assertFalse(item.hidden)
    }
    @Test fun forecastUsesResponseAndParsesModernServiceEnvelope() {
        val request = splitIncoming(forecastRequest(12,"weather.home")).single()
        assertTrue(request["return_response"]!!.jsonPrimitive.boolean)
        assertEquals("get_forecasts",request["service"]!!.jsonPrimitive.content)
        assertEquals("daily",request["service_data"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val result = StovaJson.parseToJsonElement("""{"context":{"id":"abc"},"response":{"weather.home":{"forecast":[{"datetime":"2026-10-25T12:00:00+00:00","condition":"rainy","temperature":8.2,"templow":3.1,"precipitation":4.2},{"datetime":"bad","temperature":null}]}}}""").jsonObject
        val day = parseForecast(result,"weather.home").single()
        assertEquals("rainy",day.condition); assertEquals(8.2,day.high,0.001); assertEquals(3.1,day.low!!,0.001)
    }
    @Test fun normalServiceDoesNotRequestAResponse() {
        assertNull(splitIncoming(callService(1,"light","toggle","light.a",JsonObject(emptyMap()))).single()["return_response"])
    }
    @Test fun disabledRegistryEntitiesRemainAvailableUnderShowAll() {
        val registry=parseRegistry(array("[]"),array("[]"),array("""[{"entity_id":"sensor.battery","disabled_by":"integration","original_name":"Batteri"}]"""))
        val item=entityCatalog(emptyList(),registry).single()
        assertTrue(item.hidden);assertEquals("Batteri",item.name);assertEquals("unavailable",item.entity.state)
    }
}
