package app.stova.homebutton

import android.content.Context
import app.stova.core.storage.JsonFileStore
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class CrashGuardState(val times: List<Long> = emptyList(), val tripped: Boolean = false)

/**
 * A launcher that crashes on start while it also owns the Home key would leave the TV without a
 * home screen. Three crashes within five minutes hand Home back to the stock launcher until the
 * user turns the override on again (Marquise does the same).
 */
object CrashGuard {
    const val WINDOW_MS = 300_000L
    const val THRESHOLD = 3

    fun record(times: List<Long>, now: Long): Pair<List<Long>, Boolean> {
        val kept = times.filter { now - it < WINDOW_MS } + now
        return kept to (kept.size >= THRESHOLD)
    }

    fun store(context: Context): JsonFileStore<CrashGuardState> =
        JsonFileStore(File(context.filesDir, "crash_guard.json"), CrashGuardState.serializer(), CrashGuardState())

    /** Records the crash synchronously (the process is about to die), then lets the old handler run. */
    fun install(store: JsonFileStore<CrashGuardState>, clock: () -> Long = System::currentTimeMillis) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                store.writeBlocking { state ->
                    val (times, trip) = record(state.times, clock())
                    state.copy(times = times, tripped = state.tripped || trip)
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    suspend fun reset(store: JsonFileStore<CrashGuardState>) {
        store.update { CrashGuardState() }
    }
}
