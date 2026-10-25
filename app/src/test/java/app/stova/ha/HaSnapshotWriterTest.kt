package app.stova.ha

import app.stova.core.storage.JsonFileStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HaSnapshotWriterTest {
    @get:Rule val dir = TemporaryFolder()
    @Test fun ignoresTimestampOnlyChangesAndFlushesLastValueOnStop() = runTest {
        val file = dir.root.resolve("cache.json")
        val store = JsonFileStore(file, HaSnapshot.serializer(), HaSnapshot(), UnconfinedTestDispatcher(testScheduler))
        val writer = HaSnapshotWriter(store, backgroundScope)
        fun snapshot(state: String, time: Long) = HaSnapshot("p", mapOf("light.a" to HaEntity("light.a", state, JsonObject(emptyMap()), time, time)))
        writer.offer(snapshot("on", 1)); runCurrent()
        file.writeText("untouched")
        writer.offer(snapshot("on", 2)); advanceTimeBy(60_000); runCurrent()
        assertEquals("untouched", file.readText())
        writer.offer(snapshot("off", 3)); runCurrent()
        assertEquals("on", store.current().entities.getValue("light.a").state)
        writer.flush()
        assertEquals("off", store.current().entities.getValue("light.a").state)
        assertTrue(file.readText().contains("off"))
    }
}
