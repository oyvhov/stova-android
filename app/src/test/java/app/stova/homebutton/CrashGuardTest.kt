package app.stova.homebutton

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {
    @Test fun threeCrashesInFiveMinutesTrip() {
        val (t1, a) = CrashGuard.record(emptyList(), 0)
        val (t2, b) = CrashGuard.record(t1, 60_000)
        val (_, c) = CrashGuard.record(t2, 120_000)
        assertFalse(a); assertFalse(b); assertTrue(c)
    }

    @Test fun oldCrashesExpire() {
        val (t, tripped) = CrashGuard.record(listOf(0L, 1_000L), 301_001)
        assertFalse(tripped); assertEquals(listOf(301_001L), t)
    }
}
