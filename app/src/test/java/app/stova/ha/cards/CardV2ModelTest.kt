package app.stova.ha.cards

import app.stova.core.storage.StovaJson
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CardV2ModelTest {
    @Test fun oldWideCardsMigrateAndExplicitSizeWins() {
        val old = """{"id":"a","type":"LIGHT","entityId":"light.a","wide":true}"""
        assertEquals(CardSize.M,StovaJson.decodeFromString<CardConfig>(old).size)
        assertEquals(CardSize.S,StovaJson.decodeFromString<CardConfig>(old.replace(",\"wide\":true"," ")).size)
        val large = StovaJson.decodeFromString<CardConfig>(old.dropLast(1)+",\"size\":\"L\",\"name\":\"Lampe\",\"icon\":\"sun\"}")
        assertEquals(CardSize.L,large.size); assertEquals("Lampe",large.name)
        assertEquals(large,StovaJson.decodeFromString<CardConfig>(StovaJson.encodeToString(large)))
    }
    @Test fun coverAndLockAreDiscoverable() {
        assertEquals(listOf(CardType.COVER),cardTypesFor(e("cover.curtain")))
        assertEquals(listOf(CardType.LOCK),cardTypesFor(e("lock.front")))
    }
    @Test fun riskyCardsHaveNoPrimaryService() {
        listOf(CardType.COVER,CardType.LOCK,CardType.CLIMATE,CardType.SENSOR).forEach {
            assertNull(primaryAction(it,e("cover.a")))
        }
    }
    @Test fun roomSectionsPutFavouritesFirstAndPreserveOrder() {
        val cards = listOf(CardConfig("1",CardType.LIGHT,"light.a"),CardConfig("2",CardType.LIGHT,"light.b",favorite=true),CardConfig("3",CardType.SENSOR,"sensor.c"))
        val registry = HaRegistry(mapOf("light.a" to HaEntityInfo(room="Stove"),"light.b" to HaEntityInfo(room="Gang")))
        val sections = dashboardSections(cards,registry)
        assertTrue(sections[0].favorites); assertEquals("2",sections[0].cards.single().id)
        assertEquals("Stove",sections[1].room); assertNull(sections[2].room)
    }
    @Test fun priceTimestampsPreserveBothRepeatedHoursOnDstDay() {
        val points=pricePoints(e("sensor.price","1","""{"raw_today":[{"start":"2026-10-25T02:00:00+02:00","end":"2026-10-25T02:00:00+01:00","value":-0.5},{"start":"2026-10-25T02:00:00+01:00","end":"2026-10-25T03:00:00+01:00","value":2}]}"""))
        assertEquals(2,points.size);assertEquals(points[0].end,points[1].start)
        assertEquals(3_600_000L,points[0].end-points[0].start)
        assertEquals(-0.5,points[0].value,0.001)
    }
    private fun e(id:String,state:String="on",attrs:String="{}")=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
}
