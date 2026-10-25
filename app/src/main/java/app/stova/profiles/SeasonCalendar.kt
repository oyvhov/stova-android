package app.stova.profiles

import app.stova.core.theme.VisualTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

@Serializable enum class SeasonalDecor { OFF, CALM, FULL }
@Serializable enum class SeasonChoice { NONE, CALENDAR, CHRISTMAS, HALLOWEEN;
    fun applyTo(value: AppearancePrefs): AppearancePrefs {
        val stored = value.withoutCalendarSeason()
        val plain = if (stored.season() == Season.NONE) stored else stored.copy(visualTheme = VisualTheme.FOREST, accent = Accent.LIME)
        return when (this) {
            NONE, CALENDAR -> plain.copy(seasonChoice = this)
            CHRISTMAS -> Season.CHRISTMAS.applyTo(stored).copy(seasonChoice = this)
            HALLOWEEN -> Season.HALLOWEEN.applyTo(stored).copy(seasonChoice = this)
        }
    }
}

enum class Season { NONE, CHRISTMAS, HALLOWEEN;
    fun applyTo(value: AppearancePrefs): AppearancePrefs = when (this) {
        NONE -> value
        CHRISTMAS -> value.copy(visualTheme = VisualTheme.NOEL, accent = Accent.HOLLY)
        HALLOWEEN -> value.copy(visualTheme = VisualTheme.HALLOWEEN, accent = Accent.PUMPKIN)
    }
    companion object {
        fun onCalendar(date: LocalDate): Season = when {
            date.monthValue == 10 && date.dayOfMonth >= 20 -> HALLOWEEN
            date.monthValue == 11 && date.dayOfMonth == 1 -> HALLOWEEN
            date.monthValue == 12 || (date.monthValue == 1 && date.dayOfMonth <= 13) -> CHRISTMAS
            else -> NONE
        }
    }
}

data class SeasonOverlay(val season: Season, val visualTheme: VisualTheme, val accent: Accent)
fun AppearancePrefs.season(): Season = when (visualTheme) {
    VisualTheme.NOEL -> Season.CHRISTMAS
    VisualTheme.HALLOWEEN -> Season.HALLOWEEN
    else -> Season.NONE
}
fun AppearancePrefs.activeSeason(date: LocalDate): Season = seasonal(date).season()
fun AppearancePrefs.seasonal(date: LocalDate): AppearancePrefs {
    if (seasonChoice != SeasonChoice.CALENDAR || seasonOverlay != null) return this
    val season = Season.onCalendar(date)
    return if (season == Season.NONE) this else season.applyTo(this).copy(seasonOverlay = SeasonOverlay(season, visualTheme, accent))
}
fun AppearancePrefs.withoutCalendarSeason(): AppearancePrefs {
    val overlay = seasonOverlay ?: return this
    val laid = overlay.season.applyTo(this)
    return copy(visualTheme = if (visualTheme == laid.visualTheme) overlay.visualTheme else visualTheme,
        accent = if (accent == laid.accent) overlay.accent else accent, seasonOverlay = null)
}
fun Season.daysUntilPeak(date: LocalDate): Int? {
    val peak = when (this) {
        Season.NONE -> return null
        Season.HALLOWEEN -> LocalDate.of(date.year, 10, 31)
        Season.CHRISTMAS -> LocalDate.of(date.year, 12, 24)
    }
    return ChronoUnit.DAYS.between(date, peak).toInt().takeIf { it in 0..31 }
}
