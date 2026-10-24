package app.stova.core.storage

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Forgiving on read: files written by a newer version still load, and absent fields take defaults. */
val StovaJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * One JSON document on disk, held in memory after the first read.
 *
 * A launcher that cannot read its own settings must still start, so a file that does not parse is
 * set aside as `<name>.corrupt` and the default takes its place. Writes go to `<name>.tmp` and are
 * moved over the original atomically, so a crash mid-write leaves the previous version intact.
 */
class JsonFileStore<T : Any>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<T?>(null)

    /** The current value, then every value that differs from the one before it. */
    val data: Flow<T> = flow {
        current()
        emitAll(state.filterNotNull())
    }

    suspend fun current(): T = state.value ?: mutex.withLock { loadedLocked() }

    suspend fun update(transform: (T) -> T): T = mutex.withLock {
        val next = transform(loadedLocked())
        withContext(io) { write(next) }
        state.value = next
        next
    }

    /** For the uncaught-exception handler, where nothing may suspend: same lock, on the caller's thread. */
    fun writeBlocking(transform: (T) -> T): T = runBlocking { update(transform) }

    private suspend fun loadedLocked(): T =
        state.value ?: withContext(io) { read() }.also { state.value = it }

    private fun read(): T {
        if (!file.exists()) return default
        return try {
            StovaJson.decodeFromString(serializer, file.readText())
        } catch (_: SerializationException) {
            setAside()
        } catch (_: IllegalArgumentException) {
            setAside()
        }
    }

    private fun setAside(): T {
        file.copyTo(File(file.path + ".corrupt"), overwrite = true)
        return default
    }

    private fun write(value: T) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use { out ->
            out.write(StovaJson.encodeToString(serializer, value).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        moveOver(tmp, file)
    }

    /**
     * Windows (where the unit tests run) refuses to replace a file that a scanner or indexer has
     * open for a moment after it was written. Android never does; there the first attempt wins.
     */
    private fun moveOver(source: File, target: File, attempts: Int = 5) {
        repeat(attempts - 1) {
            try {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                return
            } catch (_: java.nio.file.AccessDeniedException) {
                Thread.sleep(10)
            }
        }
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
