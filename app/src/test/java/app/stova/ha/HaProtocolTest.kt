package app.stova.ha

import app.stova.core.storage.StovaJson
import java.time.Instant
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class HaProtocolTest {
    @Test fun configRequestReadsHomeTimeZone() {
        assertEquals("get_config",splitIncoming(getConfig(10)).single()["type"]!!.jsonPrimitive.content)
    }
    @Test fun coalescedArraySplit() = assertEquals(2, splitIncoming("""[{"id":2,"type":"event"},{"id":2,"type":"event"}]""").size)
    @Test fun subscribeSortsIds() = assertTrue(subscribeEntities(3, setOf("sensor.b", "light.a")).contains(""""entity_ids":["light.a","sensor.b"]"""))
    @Test fun historySkipsNonNumeric() = assertEquals(listOf(1_700_000_000_000L to 21.5), parseHistory(StovaJson.parseToJsonElement("""{"sensor.t":[{"s":"21.5","lu":1700000000.0},{"s":"unavailable","lu":1700000060.0}]}""").jsonObject, "sensor.t"))
    @Test fun serviceHasTargetAndData() {
        val j = splitIncoming(callService(4, "light", "turn_on", "light.a", buildJsonObject { put("brightness_pct", 50) })).single()
        assertEquals("light.a", j["target"]!!.jsonObject["entity_id"]!!.jsonPrimitive.content)
        assertEquals(50, j["service_data"]!!.jsonObject["brightness_pct"]!!.jsonPrimitive.int)
    }
    @Test fun historyRequestsAllChanges() {
        val j = splitIncoming(historyRequest(2,"sensor.a",Instant.EPOCH)).single()
        assertFalse(j["significant_changes_only"]!!.jsonPrimitive.boolean)
        assertTrue(j["minimal_response"]!!.jsonPrimitive.boolean)
    }
    @Test fun parsesFullStates() {
        val states = parseStates(StovaJson.parseToJsonElement("""[{"entity_id":"light.a","state":"on","attributes":{},"last_changed":"2023-11-14T22:13:20Z","last_updated":"2023-11-14T22:13:21Z"}]""").jsonArray)
        assertEquals(1_700_000_001_000L, states.single().lastUpdated)
    }
    @Test fun parsesHomeAssistantOffsetTimestamps() {
        val states = parseStates(StovaJson.parseToJsonElement("""[{"entity_id":"light.a","state":"on","attributes":{},"last_changed":"2023-11-14T22:13:20.123456+00:00","last_updated":"2023-11-15T00:13:21+02:00"}]""").jsonArray)
        assertEquals(1_700_000_000_123L, states.single().lastChanged)
        assertEquals(1_700_000_001_000L, states.single().lastUpdated)
    }
}
