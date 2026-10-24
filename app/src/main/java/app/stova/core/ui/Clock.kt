package app.stova.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

fun clockText(epochMillis: Long, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zone).format(Instant.ofEpochMilli(epochMillis))

/** 24-hour clock that wakes once a minute, on the minute, and never otherwise recomposes. */
@Composable
fun Clock(modifier: Modifier = Modifier, clock: () -> Long = System::currentTimeMillis) {
    var now by remember { mutableLongStateOf(clock()) }
    LifecycleResumeEffect(Unit) {
        now = clock()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE - now % MINUTE)
            now = clock()
        }
    }
    Text(
        clockText(now, ZoneId.systemDefault(), LocalLocale.current.platformLocale),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

private const val MINUTE = 60_000L
