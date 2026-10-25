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
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
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
    Column(modifier, horizontalAlignment = Alignment.End) {
    if (mode != app.stova.profiles.ClockMode.HIDDEN) Text(
        clockText(now, ZoneId.systemDefault(), LocalLocale.current.platformLocale, mode),
        style = if (prefs.clockSize == app.stova.profiles.ClockSize.LARGE) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("top-clock"),
    )
    if (prefs.showDate) Text(
        DateTimeFormatter.ofPattern("EEE d. MMM", LocalLocale.current.platformLocale).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(now)),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("top-date"),
    )
    }
}

private const val MINUTE = 60_000L
