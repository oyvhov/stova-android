package app.stova.ha

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HaReadCacheTest {
    @Test fun cardReentryAndPanelShareFreshDataAndConcurrentRequests() = runTest {
        var now = 0L
        var reads = 0
        val cache = HaReadCache<List<Double>> { now }
        suspend fun read() = cache.get("sensor.a", 300_000) { reads++; delay(1); listOf(1.0, 2.0) }
        List(3) { async { read() } }.awaitAll()
        assertEquals(1, reads)
        now = 300_000
        read()
        assertEquals(2, reads)
        cache.clear()
        read()
        assertEquals(3, reads)
    }
}
