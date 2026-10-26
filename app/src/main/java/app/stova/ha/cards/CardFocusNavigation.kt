package app.stova.ha.cards

import kotlin.math.abs

/** Logical card rectangles share the exact band order used by SmartHomeContent's lazy list. */
internal data class CardFocusCell(
    val id: String,
    val column: Int,
    val row: Int,
    val columns: Int,
    val rows: Int,
    val listIndex: Int,
    val revealIndex: Int = listIndex,
)

internal enum class CardFocusDirection { UP, DOWN, LEFT, RIGHT }

internal fun cardFocusCells(sections: List<DashboardSection>, columns: Int = 3, headings: Boolean = true): List<CardFocusCell> = buildList {
    var row = 0
    var listIndex = 0
    sections.forEach { section ->
        if (headings) listIndex++ // A heading is a lazy-list item, never a focus target.
        packCards(section.cards, columns).forEachIndexed { bandIndex, band ->
            band.forEach { placed ->
                add(CardFocusCell(placed.card.id, placed.column, row + placed.row,
                    placed.card.size.columns, placed.card.size.rows, listIndex,
                    if (headings && bandIndex == 0) listIndex - 1 else listIndex))
            }
            row += band.maxOf { it.row + it.card.size.rows }
            listIndex++
        }
    }
}

/** Move to the nearest next row/column, keeping the other axis aligned where space permits. */
internal fun cardFocusNeighbor(cells: List<CardFocusCell>, id: String, direction: CardFocusDirection): String? {
    val from = cells.firstOrNull { it.id == id } ?: return null
    val vertical = direction == CardFocusDirection.UP || direction == CardFocusDirection.DOWN
    fun start(cell: CardFocusCell) = if (vertical) cell.row else cell.column
    fun end(cell: CardFocusCell) = start(cell) + if (vertical) cell.rows else cell.columns
    fun crossStart(cell: CardFocusCell) = if (vertical) cell.column else cell.row
    fun crossEnd(cell: CardFocusCell) = crossStart(cell) + if (vertical) cell.columns else cell.rows
    fun overlaps(cell: CardFocusCell) = crossStart(cell) < crossEnd(from) && crossEnd(cell) > crossStart(from)
    val forward = direction == CardFocusDirection.DOWN || direction == CardFocusDirection.RIGHT
    val candidates = cells.filter { candidate ->
        candidate.id != id && (if (forward) start(candidate) >= end(from) else end(candidate) <= start(from)) &&
            // Left/right must stay in the same visual row, rather than diagonally changing bands.
            (vertical || overlaps(candidate))
    }
    return candidates.minWithOrNull(compareBy<CardFocusCell>(
        { if (forward) start(it) - end(from) else start(from) - end(it) },
        { if (overlaps(it)) 0 else 1 },
        { abs(crossStart(it) + crossEnd(it) - crossStart(from) - crossEnd(from)) },
    ))?.id
}
