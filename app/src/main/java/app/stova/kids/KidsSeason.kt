package app.stova.kids

import app.stova.profiles.AppearancePrefs
import app.stova.profiles.SeasonChoice
import app.stova.profiles.SeasonalDecor
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import kotlinx.serialization.Serializable

/** The children's year: the four seasons, and the days a child counts towards. */
enum class KidsSeason { WINTER, EASTER, MAY_17, SPRING, SUMMER, AUTUMN, HALLOWEEN, CHRISTMAS;
    /** Easter and 17 May are spring, Halloween autumn and Christmas winter, when there is no party. */
    val nature: KidsSeason get() = when (this) {
        EASTER, MAY_17 -> SPRING
        HALLOWEEN -> AUTUMN
        CHRISTMAS -> WINTER
        else -> this
    }
    companion object {
        fun on(date: LocalDate): KidsSeason {
            val easter = easterSunday(date.year)
            val day = MonthDay.from(date)
            return when {
                date.monthValue == 12 || day <= MonthDay.of(1, 13) -> CHRISTMAS
                date.monthValue == 10 && date.dayOfMonth >= 20 || day == MonthDay.of(11, 1) -> HALLOWEEN
                !date.isBefore(easter.minusDays(7)) && !date.isAfter(easter.plusDays(1)) -> EASTER
                date.monthValue == 5 && date.dayOfMonth in 10..17 -> MAY_17
                date.monthValue in 1..2 -> WINTER
                date.monthValue in 3..5 -> SPRING
                date.monthValue in 6..8 -> SUMMER
                else -> AUTUMN
            }
        }
    }
}

/**
 * The season the profile asks for: Jul or Halloween when chosen, no parties when seasons are off
 * (the plain season still shows, as the world needs a sky), and the calendar otherwise.
 */
fun AppearancePrefs.kidsSeason(date: LocalDate): KidsSeason = when {
    seasonChoice == SeasonChoice.CHRISTMAS -> KidsSeason.CHRISTMAS
    seasonChoice == SeasonChoice.HALLOWEEN -> KidsSeason.HALLOWEEN
    seasonChoice == SeasonChoice.NONE || seasonalDecor == SeasonalDecor.OFF -> KidsSeason.on(date).nature
    else -> KidsSeason.on(date)
}

/** Easter Sunday in the Gregorian calendar (the anonymous algorithm). */
fun easterSunday(year: Int): LocalDate {
    val a = year % 19; val b = year / 100; val c = year % 100
    val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4; val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
}

/** The light outside, which the sky follows. */
enum class DayPart { MORNING, DAY, EVENING, NIGHT;
    companion object {
        fun at(time: LocalTime): DayPart = when (time.hour) {
            in 5..9 -> MORNING
            in 10..16 -> DAY
            in 17..19 -> EVENING
            else -> NIGHT
        }
    }
}

/** Behind the rows: Pip's drawn world, or the films' own artwork as on a grown-up's Home. */
@Serializable enum class KidsBackground { WORLD, ART }

/** What parents choose for Pip on a child's profile. */
@Serializable data class PipPrefs(
    /** Off: no robot, and the name of the card in focus sits over the card instead. */
    val shown: Boolean = true,
    /** Pip says the name of the card in focus, for the child who cannot read yet. */
    val readAloud: Boolean = true,
    /** Minutes after midnight; null leaves Pip awake all evening. */
    val bedtime: Int? = null,
    val background: KidsBackground = KidsBackground.WORLD,
)

/** Off, then every half hour from six to nine in the evening, and round again. */
fun PipPrefs.stepBedtime(step: Int): PipPrefs {
    val choices = listOf<Int?>(null) + (0..6).map { 18 * 60 + it * 30 }
    val at = choices.indexOf(bedtime).coerceAtLeast(0)
    return copy(bedtime = choices[Math.floorMod(at + step, choices.size)])
}

/** How close bedtime is: Pip grows sleepy half an hour before and sleeps until morning. */
enum class Bedtime { AWAKE, SOON, ASLEEP }
fun PipPrefs.bedtimeAt(time: LocalTime): Bedtime {
    val at = bedtime ?: return Bedtime.AWAKE
    val now = time.hour * 60 + time.minute
    return when {
        now < 5 * 60 -> Bedtime.ASLEEP
        now >= at -> Bedtime.ASLEEP
        now >= at - 30 -> Bedtime.SOON
        else -> Bedtime.AWAKE
    }
}
