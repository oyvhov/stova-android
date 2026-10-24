package app.stova.core.storage

import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@Serializable data class Sample(val n: Int = 0, val s: String = "")

@OptIn(ExperimentalCoroutinesApi::class)
class JsonFileStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun store(f: File = tmp.root.resolve("s.json")) =
        JsonFileStore(f, Sample.serializer(), Sample(), UnconfinedTestDispatcher())

    @Test fun unreadableFileFallsBackWithoutDestroyingOriginal() = runTest {
        val f = tmp.root.resolve("s.json").apply { mkdir() }
        assertEquals(Sample(), store(f).current())
        org.junit.Assert.assertTrue(f.isDirectory)
    }

    @Test fun corruptBackupFailureStillReturnsDefault() = runTest {
        val f = tmp.root.resolve("s.json").apply { writeText("{bad") }
        tmp.root.resolve("s.json.corrupt").apply { mkdir(); resolve("keep").writeText("keep") }
        assertEquals(Sample(), store(f).current())
        assertEquals("{bad", f.readText())
    }

    @Test fun missingFileGivesDefault() = runTest { assertEquals(Sample(), store().current()) }

    @Test fun updateSurvivesReopen() = runTest {
        val f = tmp.root.resolve("s.json"); store(f).update { it.copy(n = 7) }
        assertEquals(7, store(f).current().n)
    }

    @Test fun concurrentUpdatesAreNotLost() = runTest {
        val s = store(); (1..50).map { launch { s.update { it.copy(n = it.n + 1) } } }.joinAll()
        assertEquals(50, s.current().n)
    }

    @Test fun corruptFileFallsBackToDefaultAndKeepsCopy() = runTest {
        val f = tmp.root.resolve("s.json").apply { writeText("{not json") }
        assertEquals(Sample(), store(f).current())
        assertEquals("{not json", tmp.root.resolve("s.json.corrupt").readText())
    }

    @Test fun unknownKeysAreIgnored() = runTest {
        val f = tmp.root.resolve("s.json").apply { writeText("""{"n":3,"future":true}""") }
        assertEquals(3, store(f).current().n)
    }

    @Test fun dataEmitsEachUpdate() = runTest {
        val s = store(); val seen = mutableListOf<Int>()
        val job = launch { s.data.take(2).collect { seen += it.n } }
        testScheduler.runCurrent() // let the collector read the initial value before the update lands
        s.update { it.copy(n = 1) }; job.join()
        assertEquals(listOf(0, 1), seen)
    }

    @Test fun writeBlockingPersistsImmediately() {
        val f = tmp.root.resolve("s.json")
        store(f).writeBlocking { it.copy(s = "kræsj") }
        assertEquals("kræsj", kotlinx.coroutines.runBlocking { store(f).current().s })
    }
}
