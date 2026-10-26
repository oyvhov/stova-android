package app.stova.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Main-thread editor. Coalesces waiting values, never cancels an in-flight atomic write.
 * Its save scope outlives the screen; leaving flushes the last previewed value. */
internal class AppearanceEdits(initial: AppearancePrefs, private val scope: CoroutineScope,
    private val write: suspend (AppearancePrefs) -> Unit) {
    var draft by mutableStateOf(initial)
        private set
    var pending by mutableStateOf(false)
        private set
    private var queued: AppearancePrefs? = null
    private var timer: Job? = null
    private var writing = false
    private var settled = CompletableDeferred<Unit>().apply { complete(Unit) }

    fun sync(saved: AppearancePrefs) { if (!pending) draft = saved }

    fun change(value: AppearancePrefs, delayed: Boolean = true) {
        if (value == draft) return
        if (!pending) settled = CompletableDeferred()
        draft = value; queued = value; pending = true
        timer?.cancel()
        if (!delayed) flush()
        else if (!writing) timer = scope.launch { delay(180); timer = null; drain() }
    }

    fun flush() {
        timer?.cancel(); timer = null
        if (!writing && queued != null) scope.launch { drain() }
    }

    suspend fun awaitSaved() { flush(); settled.await() }

    private suspend fun drain() {
        if (writing) return
        writing = true
        try {
            while (true) {
                val next = queued ?: break
                queued = null
                write(next)
            }
        } finally {
            writing = false; pending = queued != null
            if (!pending) settled.complete(Unit)
        }
    }
}

/** Process-owned editors keep pending values available when a profile page is reopened. */
internal class AppearanceEditsStore(private val scope: CoroutineScope,
    private val write: suspend (String, AppearancePrefs) -> Unit) {
    private val editors = mutableMapOf<String, AppearanceEdits>()
    fun open(profile: Profile): AppearanceEdits = editors.getOrPut(profile.id) {
        AppearanceEdits(profile.appearance, scope) { write(profile.id, it) }
    }.also { it.sync(profile.appearance) }
}
