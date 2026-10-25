package app.stova.ha.cards

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Wall-clock millis for a card: one fresh read on resume, then one per period, nothing while stopped. */
@Composable
internal fun rememberResumedNow(periodMillis: Long, active: Boolean = true, clock: () -> Long = System::currentTimeMillis): State<Long> {
    val now = remember { mutableLongStateOf(clock()) }
    val owner = LocalLifecycleOwner.current
    val read by rememberUpdatedState(clock)
    LaunchedEffect(owner, periodMillis, active) {
        if (active) owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now.longValue = read(); delay(periodMillis) }
        }
    }
    return now
}
