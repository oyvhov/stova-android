package app.stova.ha

import app.stova.core.storage.JsonFileStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One-minute disk cadence, with a final flush when Stova leaves the foreground. */
internal class HaSnapshotWriter(private val store: JsonFileStore<HaSnapshot>?, scope: CoroutineScope) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val mutex = Mutex()
    @Volatile private var latest: HaSnapshot? = null

    init {
        if (store != null) scope.launch {
            for (ignored in requests) {
                flush()
                delay(60_000)
            }
        }
    }

    fun offer(snapshot: HaSnapshot) {
        latest = snapshot
        if (store != null) requests.trySend(Unit)
    }

    suspend fun flush() = mutex.withLock {
        val next = latest ?: return@withLock
        val target = store ?: return@withLock
        try {
            target.update { previous -> if (previous.displayData() == next.displayData()) previous else next }
        } catch (_: IOException) { /* The live screen remains usable; the next offer or stop retries. */ }
    }

    private fun HaSnapshot.displayData() = copy(entities = entities.mapValues { (_, entity) -> entity.copy(lastChanged = 0, lastUpdated = 0) })
}
