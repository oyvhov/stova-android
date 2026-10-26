package app.stova.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

fun clockText(epochMillis: Long, zone: ZoneId, locale: Locale, mode: app.stova.profiles.ClockMode = app.stova.profiles.ClockMode.H24): String =
    if (mode == app.stova.profiles.ClockMode.HIDDEN) "" else
    DateTimeFormatter.ofPattern(if (mode == app.stova.profiles.ClockMode.H12) "h:mm a" else "HH:mm", locale).withZone(zone).format(Instant.ofEpochMilli(epochMillis))

/** 24-hour clock that wakes once a minute, on the minute, and never otherwise recomposes. */
@Composable
fun Clock(modifier: Modifier = Modifier, clock: () -> Long = System::currentTimeMillis) {
    val prefs = app.stova.core.theme.LocalAppearance.current
    val mode = prefs.clock
    if (mode == app.stova.profiles.ClockMode.HIDDEN && !prefs.showDate) return
    var now by remember { mutableLongStateOf(clock()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentClock by androidx.compose.runtime.rememberUpdatedState(clock)
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            now = currentClock()
            while (true) {
                delay(MINUTE - Math.floorMod(now, MINUTE))
                now = currentClock()
            }
        }
    }
    val locale = LocalLocale.current.platformLocale
    val large = prefs.clockSize == app.stova.profiles.ClockSize.LARGE
    val time = if (mode == app.stova.profiles.ClockMode.HIDDEN) null else clockText(now, ZoneId.systemDefault(), locale, mode)
    fun date(pattern: String) = DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(now))
    val type = MaterialTheme.typography
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    when (prefs.clockStyle) {
        // Light, wide-set digits with the weekday and date in small letters below.
        app.stova.profiles.ClockStyle.THIN -> Column(modifier, horizontalAlignment = Alignment.End) {
            if (time != null) Text(time, Modifier.testTag("top-clock"), color = ink,
                style = (if (large) type.displaySmall else type.headlineLarge).copy(fontWeight = FontWeight.Thin, letterSpacing = 1.sp))
            Text(date("EEEE d. MMMM").uppercase(locale), Modifier.testTag("top-date"), color = quiet,
                style = type.labelSmall.copy(letterSpacing = 1.5.sp))
        }
        // The weekday and date stacked to the left, the time beside them like a masthead.
        app.stova.profiles.ClockStyle.SIDE -> Row(modifier, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.testTag("top-date"), horizontalAlignment = Alignment.End) {
                Text(date("EEEE"), color = ink, style = type.labelLarge)
                Text(date("d. MMMM"), color = quiet, style = type.labelMedium)
            }
            if (time != null) Text(time, Modifier.testTag("top-clock"), color = ink,
                style = (if (large) type.headlineLarge else type.headlineMedium).copy(fontWeight = FontWeight.Light))
        }
        app.stova.profiles.ClockStyle.STANDARD -> Column(modifier, horizontalAlignment = Alignment.End) {
            if (time != null) Text(time, Modifier.testTag("top-clock"), color = ink,
                style = if (large) type.headlineMedium else type.titleLarge)
            if (prefs.showDate) Text(date("EEE d. MMM"), Modifier.testTag("top-date"), color = ink, style = type.bodySmall)
        }
    }
}

private const val MINUTE = 60_000L
