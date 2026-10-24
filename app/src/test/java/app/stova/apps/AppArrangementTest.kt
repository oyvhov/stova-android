package app.stova.apps

import app.stova.profiles.AppPrefs
import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppArrangementTest {
    private fun app(p: String, l: String) = InstalledApp("$p/.Main", p, ".Main", l, true)
    private val a = app("a", "Ærlig"); private val b = app("b", "Banan"); private val c = app("c", "Østen"); private val d = app("d", "Apple")
    private val nn = Collator.getInstance(Locale.forLanguageTag("nn"))

    @Test fun favoritesInOrderAllSortedNorwegian() {
        val r = arrangeApps(listOf(a, b, c, d), AppPrefs(favorites = listOf(c.key, "gone/.X", a.key)), kid = false, nn)
        assertEquals(listOf(c, a), r.favorites); assertEquals(listOf(d, b, a, c), r.all)
    }

    @Test fun hiddenAreSeparated() {
        val r = arrangeApps(listOf(a, b), AppPrefs(hidden = setOf(b.key)), false, nn)
        assertEquals(listOf(a), r.all); assertEquals(listOf(b), r.hidden)
    }

    @Test fun kidSeesOnlyAllowed() {
        val r = arrangeApps(listOf(a, b, c), AppPrefs(favorites = listOf(a.key, b.key), kidAllowed = setOf(b.key)), true, nn)
        assertEquals(listOf(b), r.favorites); assertEquals(listOf(b), r.all); assertEquals(emptyList<InstalledApp>(), r.hidden)
    }

    @Test fun moveFavoriteClamps() {
        assertEquals(listOf("x", "z", "y"), moveFavorite(listOf("x", "y", "z"), "y", +1))
        assertEquals(listOf("y", "x", "z"), moveFavorite(listOf("x", "y", "z"), "y", -5))
    }
}
