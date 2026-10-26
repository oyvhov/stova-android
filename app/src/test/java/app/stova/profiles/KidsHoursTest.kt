package app.stova.profiles

import org.junit.Assert.*
import org.junit.Test

class KidsHoursTest {
    private val kidsLib = BackgroundLibraryRef("a", "kids")
    private val films = BackgroundLibraryRef("a", "films")
    private val series = BackgroundLibraryRef("a", "series")
    private val kids = KidsHours(enabled = true, libraries = setOf(kidsLib))

    @Test fun coversSixToEightEveryDayWithTheEndHourExcluded() {
        assertFalse(kids.covers(5)); assertTrue(kids.covers(6)); assertTrue(kids.covers(19)); assertFalse(kids.covers(20))
        assertFalse(KidsHours().covers(12))
    }

    @Test fun anOvernightWindowWrapsAroundMidnight() {
        val night = KidsHours(true, 22, 2)
        assertTrue(night.covers(23)); assertTrue(night.covers(1)); assertFalse(night.covers(2)); assertFalse(night.covers(12))
    }

    @Test fun daytimeShowsOnlyKidsLibrariesAndEveningsLeaveThemOut() {
        val chosen = BackgroundFilter(setOf(kidsLib, films, series), maxAge = 12)
        assertEquals(BackgroundFilter(setOf(kidsLib), maxAge = 12), chosen.atHour(kids, 8))
        assertEquals(BackgroundFilter(setOf(films, series), maxAge = 12), chosen.atHour(kids, 21))
    }

    @Test fun offOrWithoutKidsLibrariesChangesNothing() {
        val chosen = BackgroundFilter(setOf(kidsLib, films))
        assertEquals(chosen, chosen.atHour(kids.copy(enabled = false), 8))
        assertEquals(chosen, chosen.atHour(kids.copy(libraries = emptySet()), 8))
        assertEquals(BackgroundFilter(), BackgroundFilter().atHour(kids, 21))
    }
}
