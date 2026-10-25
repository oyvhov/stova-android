package app.stova.ha.cards

import org.junit.Assert.*
import org.junit.Test

class CardFocusNavigationTest {
    private fun card(id:String,size:CardSize=CardSize.S)=CardConfig(id,CardType.SENSOR,"sensor.$id",size=size)
    private fun cells(cards:List<CardConfig>)=cardFocusCells(listOf(DashboardSection(null,false,cards)))

    @Test fun mixedLargeAndSmallCardsFollowTheVisibleRowsAndStopAtTheEdges() {
        val positions=cells(listOf(card("a",CardSize.L),card("b"),card("c"),card("d"),card("e"),card("f")))
        assertEquals("b",cardFocusNeighbor(positions,"a",CardFocusDirection.RIGHT))
        assertEquals("c",cardFocusNeighbor(positions,"b",CardFocusDirection.DOWN))
        assertEquals("f",cardFocusNeighbor(positions,"c",CardFocusDirection.DOWN))
        assertEquals("d",cardFocusNeighbor(positions,"a",CardFocusDirection.DOWN))
        assertEquals("a",cardFocusNeighbor(positions,"d",CardFocusDirection.UP))
        assertNull(cardFocusNeighbor(positions,"a",CardFocusDirection.LEFT))
        assertNull(cardFocusNeighbor(positions,"b",CardFocusDirection.RIGHT))
        assertNull(cardFocusNeighbor(positions,"f",CardFocusDirection.DOWN))
    }

    @Test fun nextVisibleBandWinsBeforeDistantCardsInTheSameColumn() {
        val positions=cells(listOf(card("a",CardSize.M),card("b"),card("c",CardSize.M),card("d",CardSize.L),card("e")))
        assertEquals("c",cardFocusNeighbor(positions,"b",CardFocusDirection.DOWN))
    }

    @Test fun roomHeadingsAreSkippedButRetainTheCorrectLazyListItemIndex() {
        val positions=cardFocusCells(listOf(DashboardSection("Stove",true,listOf(card("a"))),
            DashboardSection("Kontor",false,listOf(card("b")))))
        assertEquals("b",cardFocusNeighbor(positions,"a",CardFocusDirection.DOWN))
        assertEquals(1,positions.first().listIndex)
        assertEquals(3,positions.last().listIndex)
    }
}
