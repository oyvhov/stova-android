package app.stova.apps

import org.junit.Assert.*
import org.junit.Test

class FavoriteMoveStateTest {
    private val keys = listOf("a", "b", "c", "d", "e")
    @Test fun repeatedArrowsMoveTheSameKeyAndSaveOnlyTheFinalOrder() {
        val move = FavoriteMoveState()
        move.begin("b", keys)
        repeat(3) { move.move(1) }
        assertEquals(listOf("a", "c", "d", "e", "b"), move.ordered(keys))
        assertEquals("b", move.key)
        assertEquals(move.ordered(keys), move.finish())
        assertNull(move.key)
        // Keep the draft on screen until the stored profile catches up.
        assertEquals(listOf("a", "c", "d", "e", "b"), move.ordered(keys))
    }
    @Test fun cancelRestoresTheOrderAndKeepsTheFocusIdentity() {
        val move = FavoriteMoveState(); move.begin("b", keys); move.move(3); move.cancel()
        assertEquals(keys, move.ordered(keys)); assertEquals("b", move.focusKey); assertNull(move.finish())
    }
    @Test fun boundariesDoNotWrapAndRemovalEndsTheDraft() {
        val move = FavoriteMoveState(); move.begin("a", keys); move.move(-10)
        assertEquals(keys, move.ordered(keys))
        move.reconcile(keys - "a")
        assertNull(move.key); assertNull(move.focusKey); assertEquals(keys - "a", move.ordered(keys - "a"))
    }
    @Test fun savingPreservesUnshownSlotsAndNeverResurrectsARemovedFavorite() {
        assertEquals(listOf("b", "hidden", "a", "new"),
            mergeFavoriteOrder(listOf("a", "hidden", "b", "new"), listOf("b", "gone", "a", "b")))
    }
}
