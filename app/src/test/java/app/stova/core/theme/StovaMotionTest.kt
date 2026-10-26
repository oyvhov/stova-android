package app.stova.core.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The motion tokens: three calm durations, and nothing at all under reduced motion. */
class StovaMotionTest {
    @Test fun reducedMotionCollapsesEveryDurationToZero() {
        val still = StovaMotion.tokens(reduceMotion = true)
        assertTrue(still.still)
        assertEquals(0, still.quick)
        assertEquals(0, still.standard)
        assertEquals(0, still.emphasized)
        assertEquals(0, still.ambient)
        assertEquals(0, still.focusRing)
        assertEquals(0, still.focusZoom)
        assertEquals(0, still.rowTravel)
        assertEquals(0, still.travel<Float>().durationMillis)
        // A delayed entry would still be a wait: delays collapse with the durations.
        assertEquals(0, still.enter<Float>(delayMillis = 250).delay)
        assertEquals(0, still.enter<Float>(delayMillis = 250).durationMillis)
        assertEquals(0, still.exit<Float>().durationMillis)
        assertEquals(0, still.fade<Float>().durationMillis)
    }

    @Test fun fullMotionIsQuickStandardEmphasized() {
        val full = StovaMotion.tokens(reduceMotion = false)
        assertFalse(full.still)
        assertEquals(150, full.quick)
        assertEquals(200, full.standard)
        assertEquals(300, full.emphasized)
        assertTrue(full.quick < full.standard && full.standard < full.emphasized && full.emphasized < full.ambient)
        assertEquals(200, full.enter<Float>().durationMillis)
        assertEquals(250, full.enter<Float>(delayMillis = 250).delay)
        assertEquals(150, full.exit<Float>().durationMillis)
        assertEquals(380, full.travel<Float>().durationMillis)
    }

    @Test fun entriesDecelerateAndExitsAccelerate() {
        val full = StovaMotion.Full
        // Halfway through its time an entry is well past halfway; an exit has barely begun.
        assertTrue(full.enter<Float>().easing.transform(0.5f) > 0.75f)
        assertTrue(full.exit<Float>().easing.transform(0.5f) < 0.35f)
        // The curves never overshoot: no spring, no bounce.
        for (i in 0..20) {
            val t = i / 20f
            assertTrue(StovaMotion.Enter.transform(t) in 0f..1.0001f)
            assertTrue(StovaMotion.Exit.transform(t) in 0f..1.0001f)
            assertTrue(StovaMotion.Travel.transform(t) in 0f..1.0001f)
        }
        assertTrue(StovaMotion.Travel.transform(0.1f) < 0.05f)
        assertTrue(StovaMotion.Travel.transform(0.9f) > 0.98f)
    }
}
