package app.stova.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One pending focus change or idle rotation, never one image request per D-pad press. */
class BackdropController(
    private val scope: CoroutineScope,
    private val focusDelayMillis: Long = 250,
    private val rotationMillis: Long = 20_000,
) {
    private val state = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = state
    private var pending: Job? = null
    private var focused: String? = null
    private var pool: List<String> = emptyList()
    private var enabled = false
    private var index = 0

    fun focus(url: String?) {
        if (url == focused) return
        focused = url
        pending?.cancel()
        if (url == null) rotate()
        else pending = scope.launch { delay(focusDelayMillis); state.value = url }
    }

    fun rotation(urls: List<String>, enabled: Boolean) {
        val unique = urls.distinct()
        if (pool == unique && this.enabled == enabled) return
        pool = unique
        this.enabled = enabled
        index = 0
        if (focused == null) { pending?.cancel(); rotate() }
    }

    private fun rotate() {
        if (!enabled || pool.isEmpty()) return
        pending = scope.launch {
            while (true) {
                delay(rotationMillis)
                state.value = pool[index % pool.size]
                index++
            }
        }
    }

    fun clear() {
        pending?.cancel()
        focused = null
        state.value = null
        pool = emptyList()
        index = 0
    }
}
