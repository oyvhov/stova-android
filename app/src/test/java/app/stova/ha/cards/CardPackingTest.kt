package app.stova.ha.cards

import org.junit.Assert.*
import org.junit.Test

class CardPackingTest {
    @Test fun everyDensityFitsEveryCardOnceWithoutOverlap() {
        val cards = List(100) { i -> CardConfig("$i", CardType.SENSOR, "sensor.$i", size=CardSize.entries[(i*7+i/3)%3]) }
        for(columns in 2..6) {
            val bands=packCards(cards,columns)
            assertEquals(cards.map {it.id}.toSet(),bands.flatten().map {it.card.id}.toSet())
            assertEquals(cards.size,bands.flatten().size)
            bands.forEach {band ->
                val occupied=mutableSetOf<Pair<Int,Int>>()
                band.forEach {p ->
                    assertTrue(p.column+p.card.size.columns<=columns)
                    assertTrue(p.row+p.card.size.rows<=2)
                    for(r in p.row until p.row+p.card.size.rows) for(c in p.column until p.column+p.card.size.columns)
                        assertTrue("overlap at $c/$r",occupied.add(c to r))
                }
            }
        }
    }
    @Test fun textSizeAndAvailableWidthDetermineTheColumnCount() {
        assertEquals(5,dashboardColumns(856f,DashboardDensity.COMPACT))
        assertEquals(4,dashboardColumns(856f,DashboardDensity.BALANCED))
        assertEquals(3,dashboardColumns(856f,DashboardDensity.AIRY))
        assertEquals(2,dashboardColumns(856f,DashboardDensity.BALANCED,2f))
        assertEquals(2,dashboardColumns(320f,DashboardDensity.COMPACT))
    }
    @Test fun laterSmallCardsFillTheSpaceBesideALargeCard() {
        val cards = listOf(CardSize.L, CardSize.M, CardSize.S, CardSize.S).mapIndexed { i, size ->
            CardConfig("$i", CardType.SENSOR, "sensor.$i", size = size)
        }
        val bands = packCards(cards)
        assertEquals(listOf("0", "2", "3"), bands.first().map { it.card.id })
        assertEquals("1", bands[1].single().card.id)
    }

    @Test fun defaultDashboardDoesNotSeparateRoomsOrReorderFavourites() {
        val cards = listOf(CardConfig("a", CardType.LIGHT, "light.a"), CardConfig("b", CardType.LIGHT, "light.b", favorite = true))
        val registry = app.stova.ha.HaRegistry(mapOf("light.a" to app.stova.ha.HaEntityInfo(room = "Stove"),
            "light.b" to app.stova.ha.HaEntityInfo(room = "Kjøkken")))
        val sections = dashboardSections(cards, registry)
        assertEquals(1, sections.size)
        assertEquals(cards, sections.single().cards)
    }
    @Test fun largeCardOccupiesTwoRowsWithoutOverlaps() {
        val cards=listOf(CardSize.L,CardSize.S,CardSize.S,CardSize.M).mapIndexed {i,size->CardConfig("$i",CardType.SENSOR,"sensor.$i",size=size)}
        val bands=packCards(cards)
        assertEquals(listOf("0","1","2"),bands[0].map {it.card.id})
        assertEquals(2,bands[0][0].card.size.rows)
        assertEquals(2,bands[0][1].column);assertEquals(0,bands[0][1].row)
        assertEquals(2,bands[0][2].column);assertEquals(1,bands[0][2].row)
        assertEquals("3",bands[1].single().card.id)
    }
}
