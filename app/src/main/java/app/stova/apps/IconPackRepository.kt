package app.stova.apps

import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import app.stova.core.storage.StovaJson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Small versioned disk index; only visible grid icons are ever decoded by AppArtCache. */
class IconPackRepository(private val source: IconPackSource, private val directory: File) {
    private val mutablePacks = MutableStateFlow<List<IconPack>>(emptyList())
    val packs = mutablePacks.asStateFlow()
    private val mutex = Mutex()
    private val discoveryMutex = Mutex()
    private val memory = mutableMapOf<String, IconPackIndex>()
    @Volatile private var scanned = false

    /** Package callbacks invalidate the index; an unused integration does no discovery work. */
    suspend fun packagesChanged() {
        if (scanned) refresh()
    }

    suspend fun refresh() = discoveryMutex.withLock { discover() }

    private suspend fun discover() = withContext(Dispatchers.IO) {
        val installed = source.installed()
        mutablePacks.value = installed
        scanned = true
        mutex.withLock { memory.keys.retainAll(installed.map { it.cacheKey }.toSet()) }
    }

    suspend fun installed(): List<IconPack> = discoveryMutex.withLock {
        if (!scanned) discover()
        packs.value
    }

    suspend fun index(pack: IconPack): IconPackIndex = withContext(Dispatchers.IO) {
        mutex.withLock {
            memory[pack.cacheKey] ?: run {
                val file = File(directory, "${pack.packageName}-${pack.versionCode}-${pack.updatedAt}.json")
                val disk = try { StovaJson.decodeFromString(IconPackIndex.serializer(), file.readText()) }
                    catch (_: IOException) { null } catch (_: IllegalArgumentException) { null }
                val loaded = disk ?: run {
                    val components = AppFilter.parse(source.appFilter(pack).orEmpty())
                    val names = (components.values + AppFilter.drawables(source.drawableCatalog(pack).orEmpty())).distinct().sorted()
                    IconPackIndex(components, names).also { value ->
                        // Cache writes are best effort: a full disk must not break the launcher.
                        try {
                            directory.mkdirs()
                            val atomic = android.util.AtomicFile(file)
                            val stream = atomic.startWrite()
                            try {
                                stream.write(StovaJson.encodeToString(IconPackIndex.serializer(), value).toByteArray(Charsets.UTF_8))
                                atomic.finishWrite(stream)
                            } catch (e: IOException) { atomic.failWrite(stream) }
                        } catch (_: IOException) { }
                    }
                }
                val supplemented = if (pack.projectivy) loaded.copy(drawables = (loaded.drawables + ProjectivyIcons.SPOLE).distinct().sorted()) else loaded
                memory[pack.cacheKey] = supplemented
                supplemented
            }
        }
    }

    suspend fun drawable(pack: IconPack, name: String): Drawable? = withContext(Dispatchers.IO) {
        if (!AppFilter.validDrawable(name)) null else source.drawable(pack, name, DisplayMetrics.DENSITY_XXHIGH)
    }
}
