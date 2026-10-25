package app.stova.profiles

import app.stova.core.theme.VisualTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

@Serializable enum class SeasonalDecor { OFF, CALM, FULL }
@Serializable enum class SeasonChoice { NONE, CALENDAR, CHRISTMAS, HALLOWEEN;
    fun applyTo(value: AppearancePrefs): AppearancePrefs {
        val plain = value.withoutCalendarSeason().withoutChosenSeason()
        return when (this) {
            NONE, CALENDAR -> plain.copy(seasonChoice = this)
            CHRISTMAS, HALLOWEEN -> (if (this == CHRISTMAS) Season.CHRISTMAS else Season.HALLOWEEN).applyTo(plain)
                .copy(seasonChoice = this, moodBeforeSeason = plain.visualTheme, accentBeforeSeason = plain.accent)
        }
    }
}

@Serializable enum class Season { NONE, CHRISTMAS, HALLOWEEN;
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
/** Ends a chosen Jul or Halloween: back to the mood before it (Skog/Lime for older profiles). */
fun AppearancePrefs.withoutChosenSeason(): AppearancePrefs =
    (if (season() == Season.NONE) this
    else copy(visualTheme = moodBeforeSeason ?: VisualTheme.FOREST, accent = accentBeforeSeason ?: Accent.LIME))
        .copy(moodBeforeSeason = null, accentBeforeSeason = null)
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

/** The moods a person picks as a base; Jul and Halloween come from the season row instead. */
val PLAIN_MOODS = VisualTheme.entries.filter { it != VisualTheme.NOEL && it != VisualTheme.HALLOWEEN }

/**
 * A new base mood. Browsing moods never touches the season: with the calendar on it stays on and
 * lays its season over the new mood; a fixed Jul or Halloween ends, since its mood was replaced.
 */
fun AppearancePrefs.withMood(theme: VisualTheme): AppearancePrefs {
    val stored = withoutCalendarSeason()
    return stored.withoutChosenSeason().copy(visualTheme = theme, seasonOverlay = null,
        seasonChoice = if (stored.seasonChoice == SeasonChoice.CALENDAR) SeasonChoice.CALENDAR else SeasonChoice.NONE)
}
