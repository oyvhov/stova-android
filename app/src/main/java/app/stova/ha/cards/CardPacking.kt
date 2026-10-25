package app.stova.ha.cards

data class PlacedCard(val card:CardConfig,val column:Int,val row:Int)

/** Two-row bands keep L cards beside two S cards, without an empty full-width grid row. */
fun packCards(cards:List<CardConfig>,columns:Int=3):List<List<PlacedCard>> {
    require(columns>=2)
    val bands=mutableListOf<List<PlacedCard>>()
    var band=mutableListOf<PlacedCard>();var occupied=Array(2) {BooleanArray(columns)}
    for(card in cards) {
        fun slot():Pair<Int,Int>? {
            for(row in 0..2-card.size.rows) for(column in 0..columns-card.size.columns) {
                if((row until row+card.size.rows).all {r->(column until column+card.size.columns).all {c->!occupied[r][c]}}) return column to row
            }
            return null
        }
        var position=slot()
        if(position==null) {bands+=band;band=mutableListOf();occupied=Array(2) {BooleanArray(columns)};position=slot()!!}
        val (column,row)=position
        band+=PlacedCard(card,column,row)
        for(r in row until row+card.size.rows) for(c in column until column+card.size.columns) occupied[r][c]=true
    }
    if(band.isNotEmpty()) bands+=band
    return bands
}
