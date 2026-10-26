package app.stova.ha.cards

data class PlacedCard(val card: CardConfig, val column: Int, val row: Int)

/** Stable first-fit packing. Later small cards fill holes without changing the saved order. */
fun packCards(cards: List<CardConfig>, columns: Int = 3): List<List<PlacedCard>> {
    require(columns >= 2)
    val bands = mutableListOf<MutableList<PlacedCard>>()
    val occupancy = mutableListOf<Array<BooleanArray>>()
    for (card in cards) {
        fun slot(cells: Array<BooleanArray>): Pair<Int, Int>? {
            for (row in 0..2 - card.size.rows) for (column in 0..columns - card.size.columns) {
                if ((row until row + card.size.rows).all { r ->
                    (column until column + card.size.columns).all { c -> !cells[r][c] }
                }) return column to row
            }
            return null
        }
        var index = occupancy.indexOfFirst { slot(it) != null }
        if (index < 0) {
            index = bands.size
            bands.add(mutableListOf())
            occupancy.add(Array(2) { BooleanArray(columns) })
        }
        val (column, row) = checkNotNull(slot(occupancy[index]))
        bands[index] += PlacedCard(card, column, row)
        for (r in row until row + card.size.rows) for (c in column until column + card.size.columns) occupancy[index][r][c] = true
    }
    return bands
}
