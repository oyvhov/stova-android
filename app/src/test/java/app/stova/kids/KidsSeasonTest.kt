package app.stova.kids

import app.stova.profiles.AppearancePrefs
import app.stova.profiles.SeasonChoice
import app.stova.profiles.SeasonalDecor
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class KidsSeasonTest {
    private fun on(y: Int, m: Int, d: Int) = KidsSeason.on(LocalDate.of(y, m, d))

    @Test fun easterFollowsTheMoon() {
        assertEquals(LocalDate.of(2026, 4, 5), easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), easterSunday(2027))
        assertEquals(LocalDate.of(2025, 4, 20), easterSunday(2025))
    }

    @Test fun theYearAsAChildCountsIt() {
        assertEquals(KidsSeason.CHRISTMAS, on(2026, 12, 1)); assertEquals(KidsSeason.CHRISTMAS, on(2027, 1, 13))
        assertEquals(KidsSeason.WINTER, on(2027, 1, 14)); assertEquals(KidsSeason.WINTER, on(2027, 2, 28))
        assertEquals(KidsSeason.SPRING, on(2027, 3, 1))
        assertEquals(KidsSeason.EASTER, on(2027, 3, 21)); assertEquals(KidsSeason.EASTER, on(2027, 3, 29))
        assertEquals(KidsSeason.SPRING, on(2027, 3, 30))
        assertEquals(KidsSeason.MAY_17, on(2027, 5, 17)); assertEquals(KidsSeason.SPRING, on(2027, 5, 18))
        assertEquals(KidsSeason.SUMMER, on(2027, 7, 1))
        assertEquals(KidsSeason.AUTUMN, on(2026, 10, 19)); assertEquals(KidsSeason.HALLOWEEN, on(2026, 10, 29))
        assertEquals(KidsSeason.HALLOWEEN, on(2026, 11, 1)); assertEquals(KidsSeason.AUTUMN, on(2026, 11, 2))
    }

    @Test fun theProfilesSeasonChoiceIsKept() {
        val halloween = LocalDate.of(2026, 10, 29)
        assertEquals(KidsSeason.HALLOWEEN, AppearancePrefs().kidsSeason(halloween))
        assertEquals(KidsSeason.AUTUMN, AppearancePrefs(seasonChoice = SeasonChoice.NONE).kidsSeason(halloween))
        assertEquals(KidsSeason.AUTUMN, AppearancePrefs(seasonalDecor = SeasonalDecor.OFF).kidsSeason(halloween))
        assertEquals(KidsSeason.CHRISTMAS, AppearancePrefs(seasonChoice = SeasonChoice.CHRISTMAS).kidsSeason(halloween))
    }

    @Test fun theSkyFollowsTheHour() {
        assertEquals(DayPart.NIGHT, DayPart.at(LocalTime.of(4, 59)))
        assertEquals(DayPart.MORNING, DayPart.at(LocalTime.of(7, 0)))
        assertEquals(DayPart.DAY, DayPart.at(LocalTime.of(13, 0)))
        assertEquals(DayPart.EVENING, DayPart.at(LocalTime.of(18, 30)))
        assertEquals(DayPart.NIGHT, DayPart.at(LocalTime.of(21, 0)))
    }

    @Test fun bedtimeStepsThroughTheEveningAndRoundToOff() {
        var prefs = PipPrefs()
        prefs = prefs.stepBedtime(1); assertEquals(18 * 60, prefs.bedtime)
        prefs = prefs.stepBedtime(1); assertEquals(18 * 60 + 30, prefs.bedtime)
        prefs = prefs.stepBedtime(-2); assertEquals(null, prefs.bedtime)
        prefs = prefs.stepBedtime(-1); assertEquals(21 * 60, prefs.bedtime)
        prefs = prefs.stepBedtime(1); assertEquals(null, prefs.bedtime)
    }

    @Test fun pipGrowsSleepyHalfAnHourBeforeBedtimeAndSleepsUntilMorning() {
        val prefs = PipPrefs(bedtime = 19 * 60)
        assertEquals(Bedtime.AWAKE, prefs.bedtimeAt(LocalTime.of(18, 29)))
        assertEquals(Bedtime.SOON, prefs.bedtimeAt(LocalTime.of(18, 30)))
        assertEquals(Bedtime.ASLEEP, prefs.bedtimeAt(LocalTime.of(19, 0)))
        assertEquals(Bedtime.ASLEEP, prefs.bedtimeAt(LocalTime.of(4, 0)))
        assertEquals(Bedtime.AWAKE, prefs.bedtimeAt(LocalTime.of(7, 0)))
        assertEquals(Bedtime.AWAKE, PipPrefs().bedtimeAt(LocalTime.of(23, 0)))
    }
}
