package app.stova.ha.cards

import org.junit.Assert.*
import org.junit.Test

class CardPackingTest {
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
