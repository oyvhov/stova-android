package app.stova.ha

import app.stova.core.storage.StovaJson
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class EntityStateDecoderTest {
    private fun j(s: String) = StovaJson.parseToJsonElement(s).jsonObject
    private val added = EntityStateDecoder.apply(emptyMap(), j("""{"a":{"light.stove":{"s":"on","a":{"brightness":200,"friendly_name":"Stove"},"c":"x","lc":1700000000.5}}}"""))
    @Test fun add() { val e = added.getValue("light.stove"); assertEquals("on", e.state); assertEquals(1_700_000_000_500, e.lastChanged); assertEquals(e.lastChanged, e.lastUpdated) }
    @Test fun changeStateAndMergeAttributes() {
        val e = EntityStateDecoder.apply(added, j("""{"c":{"light.stove":{"+":{"s":"off","lc":1700000100.0,"a":{"brightness":0}}}}}""")).getValue("light.stove")
        assertEquals("off", e.state); assertEquals(0, e.attributes["brightness"]!!.jsonPrimitive.int); assertEquals("Stove", e.attributes["friendly_name"]!!.jsonPrimitive.content)
        assertEquals(1_700_000_100_000L, e.lastUpdated)
        assertEquals("on", added.getValue("light.stove").state)
    }
    @Test fun removeAttribute() = assertNull(EntityStateDecoder.apply(added, j("""{"c":{"light.stove":{"-":{"a":["brightness"]}}}}""")).getValue("light.stove").attributes["brightness"])
    @Test fun removeEntity() = assertFalse(EntityStateDecoder.apply(added, j("""{"r":["light.stove"]}""")).containsKey("light.stove"))
    @Test fun changeForUnknownIgnored() = assertEquals(added, EntityStateDecoder.apply(added, j("""{"c":{"switch.x":{"+":{"s":"on"}}}}""")))
    @Test fun updatedDoesNotChangeLastChanged() {
        val e = EntityStateDecoder.apply(added, j("""{"c":{"light.stove":{"+":{"lu":1700000001.25}}}}""")).getValue("light.stove")
        assertEquals(1_700_000_001_250L, e.lastUpdated); assertEquals(added.getValue(e.id).lastChanged, e.lastChanged)
    }
}
