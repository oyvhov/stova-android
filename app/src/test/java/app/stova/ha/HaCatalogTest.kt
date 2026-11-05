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
    @Test fun roomIconsFollowTheResolvedAreaAndSurviveOldSnapshots() {
        val registry=parseRegistry(
            array("""[{"area_id":"k","name":"Kjøkken","icon":"mdi:stove"},{"area_id":"s","name":"Stove","icon":"mdi:sofa"}]"""),
            array("""[{"id":"device","area_id":"k"}]"""),
            array("""[{"entity_id":"light.a","device_id":"device","icon":"mdi:lamp"},{"entity_id":"light.b","device_id":"device","area_id":"s"},{"entity_id":"sensor.none"}]"""))
        assertEquals("mdi:stove",registry.entities["light.a"]?.roomIcon)
        assertEquals("mdi:lamp",registry.entities["light.a"]?.icon)
        assertEquals("mdi:sofa",registry.entities["light.b"]?.roomIcon)
        assertNull(registry.entities["sensor.none"]?.roomIcon)
        assertEquals(registry,StovaJson.decodeFromString<HaRegistry>(StovaJson.encodeToString(HaRegistry.serializer(),registry)))
        assertNull(StovaJson.decodeFromString<HaRegistry>("""{"entities":{"light.old":{"room":"Stove"}}}""").entities["light.old"]?.roomIcon)
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
    @Test fun sonosEntitiesCarryTheirDeviceModelOthersStayLean() {
        val registry = parseRegistry(array("[]"),
            array("""[{"id":"s","model":"Era 100","manufacturer":"Sonos"},{"id":"c","model":"Chromecast","manufacturer":"Google"},{"id":"m","model":42}]"""),
            array("""[{"entity_id":"media_player.kitchen","platform":"sonos","device_id":"s"},{"entity_id":"media_player.cast","platform":"cast","device_id":"c"},
                {"entity_id":"media_player.odd","platform":"sonos","device_id":"m"},{"entity_id":"media_player.lone","platform":"sonos"}]"""))
        assertEquals("Era 100", registry.entities["media_player.kitchen"]?.model)
        assertEquals("Sonos", registry.entities["media_player.kitchen"]?.manufacturer)
        assertNull(registry.entities["media_player.cast"]?.model)
        assertNull(registry.entities["media_player.lone"]?.model)
        // A malformed registry value is not trusted as a name.
        assertNull(registry.entities["media_player.odd"]?.model)
    }
}
