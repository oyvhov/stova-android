package app.stova.profiles

import app.stova.core.theme.VisualTheme
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SeasonCalendarTest {
    @Test fun calendarBoundariesIncludingYearRollover() {
        val dates = mapOf("2026-10-19" to Season.NONE, "2026-10-20" to Season.HALLOWEEN,
            "2026-11-01" to Season.HALLOWEEN, "2026-11-02" to Season.NONE,
            "2026-11-30" to Season.NONE, "2026-12-01" to Season.CHRISTMAS,
            "2027-01-13" to Season.CHRISTMAS, "2027-01-14" to Season.NONE)
        dates.forEach { (date, season) -> assertEquals(date, season, Season.onCalendar(LocalDate.parse(date))) }
    }
    @Test fun calendarCoversAndRestoresTheStoredMoodWithoutSerializingOverlay() {
        val base = AppearancePrefs(visualTheme = VisualTheme.MIDNIGHT, accent = Accent.OCEAN)
        val shown = base.seasonal(LocalDate.of(2026, 10, 25))
        assertEquals(VisualTheme.HALLOWEEN, shown.visualTheme)
        assertEquals(Accent.PUMPKIN, shown.accent)
        assertEquals(shown, shown.seasonal(LocalDate.of(2026, 10, 25)))
        assertEquals(base, shown.withoutCalendarSeason())
        val encoded = Json.encodeToString(shown.withoutCalendarSeason())
        assertFalse(encoded.contains("Overlay"))
        assertEquals(base, Json.decodeFromString<AppearancePrefs>(encoded))
        assertEquals(base, base.seasonal(LocalDate.of(2026, 11, 2)))
        assertEquals(base.copy(showTitles = true), shown.copy(showTitles = true).withoutCalendarSeason())
        assertEquals(Accent.GOLD, shown.copy(accent = Accent.GOLD).withoutCalendarSeason().accent)
    }
    @Test fun explicitSeasonsAndNoneRespectUserChoice() {
        val base = AppearancePrefs(visualTheme = VisualTheme.MIDNIGHT, accent = Accent.OCEAN)
        assertEquals(Season.NONE, base.copy(seasonChoice = SeasonChoice.NONE).seasonal(LocalDate.of(2026,10,25)).season())
        val fixed = SeasonChoice.CHRISTMAS.applyTo(base)
        assertEquals(Season.CHRISTMAS, fixed.seasonal(LocalDate.of(2026,6,1)).season())
        assertEquals(Accent.HOLLY, fixed.accent)
        assertEquals(base.copy(seasonChoice = SeasonChoice.NONE), SeasonChoice.NONE.applyTo(base))
        // Review finding 14: leaving a chosen season brings back the mood you had, not Skog/Lime.
        assertEquals(VisualTheme.MIDNIGHT, SeasonChoice.CALENDAR.applyTo(fixed).visualTheme)
    }
    @Test fun leavingAChosenSeasonRestoresTheMoodYouHad() {
        val mine = AppearancePrefs(visualTheme = VisualTheme.MIDNIGHT, accent = Accent.OCEAN, seasonChoice = SeasonChoice.NONE)
        val back = SeasonChoice.NONE.applyTo(SeasonChoice.CHRISTMAS.applyTo(mine))
        assertEquals(VisualTheme.MIDNIGHT to Accent.OCEAN, back.visualTheme to back.accent)
        val viaHalloween = SeasonChoice.NONE.applyTo(SeasonChoice.HALLOWEEN.applyTo(SeasonChoice.CHRISTMAS.applyTo(mine)))
        assertEquals("switching seasons keeps the first mood", mine, viaHalloween)
        val picked = SeasonChoice.CHRISTMAS.applyTo(mine).withMood(VisualTheme.CINEMA)
        assertEquals(VisualTheme.CINEMA to Accent.OCEAN, picked.visualTheme to picked.accent)
        assertEquals(mine.copy(visualTheme = VisualTheme.CINEMA), picked)
        val saved = Json.encodeToString(AppearancePrefs.serializer(), SeasonChoice.HALLOWEEN.applyTo(mine))
        assertEquals(mine, SeasonChoice.NONE.applyTo(Json.decodeFromString<AppearancePrefs>(saved)))
    }
    @Test fun countdownStopsAfterPeakAndAtThirtyOneDays() {
        assertEquals(6, Season.HALLOWEEN.daysUntilPeak(LocalDate.of(2026,10,25)))
        assertEquals(0, Season.HALLOWEEN.daysUntilPeak(LocalDate.of(2026,10,31)))
        assertNull(Season.HALLOWEEN.daysUntilPeak(LocalDate.of(2026,11,1)))
        assertEquals(23, Season.CHRISTMAS.daysUntilPeak(LocalDate.of(2026,12,1)))
        assertNull(Season.CHRISTMAS.daysUntilPeak(LocalDate.of(2027,1,1)))
        assertNull(Season.CHRISTMAS.daysUntilPeak(LocalDate.of(2026,6,1)))
    }

    // Review finding 4, and the Shield freeze: browsing moods during Halloween turned the calendar
    // off and flipped the season on every press.
    @org.junit.Test fun browsingMoodsKeepsTheCalendarSeason() {
        val calendar = AppearancePrefs(seasonChoice = SeasonChoice.CALENDAR)
        val midnight = calendar.seasonal(java.time.LocalDate.of(2026, 10, 25)).withMood(app.stova.core.theme.VisualTheme.MIDNIGHT)
        org.junit.Assert.assertEquals(SeasonChoice.CALENDAR, midnight.seasonChoice)
        org.junit.Assert.assertEquals(app.stova.core.theme.VisualTheme.MIDNIGHT, midnight.visualTheme)
        org.junit.Assert.assertEquals(null, midnight.seasonOverlay)
        org.junit.Assert.assertEquals(Season.HALLOWEEN, midnight.seasonal(java.time.LocalDate.of(2026, 10, 25)).season())
        val fixed = SeasonChoice.CHRISTMAS.applyTo(AppearancePrefs(seasonChoice = SeasonChoice.NONE))
        org.junit.Assert.assertEquals(SeasonChoice.NONE, fixed.withMood(app.stova.core.theme.VisualTheme.PLUM).seasonChoice)
    }
}
