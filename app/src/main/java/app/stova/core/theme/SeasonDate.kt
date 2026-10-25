package app.stova.core.theme

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import kotlinx.coroutines.delay

/** Rechecks midnight/time-zone changes while visible and immediately on returning to the launcher. */
@Composable
fun currentSeasonDate(): LocalDate {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var date by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { date = LocalDate.now(); delay(60_000) }
        }
    }
    return date
}
