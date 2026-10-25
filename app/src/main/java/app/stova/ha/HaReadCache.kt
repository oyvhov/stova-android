package app.stova.ha

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by cards and panels. Each entity has its own lock; unrelated reads run in parallel. */
internal class HaReadCache<T>(private val clock: () -> Long = System::currentTimeMillis) {
    private class Entry<T> {
        val mutex = Mutex()
        var value: T? = null
        var loadedAt: Long? = null
    }
    private val entries = ConcurrentHashMap<String, Entry<T>>()

    suspend fun get(key: String, maxAgeMillis: Long, load: suspend () -> T): T {
        val entry = entries.getOrPut(key) { Entry() }
        return entry.mutex.withLock {
            val now = clock()
            val cached = entry.value
            if (cached != null && entry.loadedAt?.let { now - it in 0 until maxAgeMillis } == true) cached
            else load().also { entry.value = it; entry.loadedAt = now }
        }
    }

    fun clear() = entries.clear()
}
