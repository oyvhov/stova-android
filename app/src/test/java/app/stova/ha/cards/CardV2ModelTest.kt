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
    @Test fun remoteAdjustmentsClampAndRespectSupportedFeatures() {
        assertEquals(brightnessCall(60),directionAction(CardType.LIGHT,e("light.a", "on", """{"brightness":128,"supported_color_modes":["brightness"]}"""),1))
        assertNull(directionAction(CardType.LIGHT,e("light.a","on","""{"supported_color_modes":["onoff"]}"""),1))
        assertEquals(1.0,directionAction(CardType.MEDIA_PLAYER,e("media_player.a","playing","""{"volume_level":0.98}"""),1)!!.data.number("volume_level")!!,0.01)
        assertEquals(100.0,directionAction(CardType.COVER,e("cover.a","open","""{"current_position":95,"supported_features":4}"""),1)!!.data.number("position")!!,0.01)
    }
    @Test fun roomSectionsPutFavouritesFirstAndPreserveOrder() {
        val cards = listOf(CardConfig("1",CardType.LIGHT,"light.a"),CardConfig("2",CardType.LIGHT,"light.b",favorite=true),CardConfig("3",CardType.SENSOR,"sensor.c"))
        val registry = HaRegistry(mapOf("light.a" to HaEntityInfo(room="Stove"),"light.b" to HaEntityInfo(room="Gang")))
        val sections = dashboardSections(cards,registry)
        assertTrue(sections[0].favorites); assertEquals("2",sections[0].cards.single().id)
        assertEquals("Stove",sections[1].room); assertNull(sections[2].room)
    }
    private fun e(id:String,state:String="on",attrs:String="{}")=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
}
