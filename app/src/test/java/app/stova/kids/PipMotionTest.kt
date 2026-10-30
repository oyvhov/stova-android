package app.stova.kids

import androidx.compose.ui.geometry.Offset
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipMotionTest {
    private var clock = 0L
    private fun PipMotion.run(seconds: Float) { repeat((seconds * 60).toInt()) { clock += 16_666_667L; step(clock) } }

    @Test fun heWalksThereTakingStepsAsHeGoes() {
        val pip = PipMotion(Random(1))
        pip.moveTo(Offset(100f, 500f))
        pip.moveTo(Offset(500f, 500f))
        pip.run(0.25f)
        assertTrue("on his way", pip.x in 120f..480f)
        assertTrue("walking, not gliding", pip.stride > 0.3f && pip.gait > 0.5f)
        pip.run(2f)
        assertEquals(500f, pip.x, 0.5f)
        assertEquals(0f, pip.stride, 0.01f)
    }

    @Test fun anotherRowIsAHopThatLandsOnIt() {
        val pip = PipMotion(Random(2))
        pip.moveTo(Offset(300f, 600f))
        pip.moveTo(Offset(300f, 300f))
        pip.run(0.2f)
        assertTrue("in the air", pip.air > 0.3f)
        pip.run(1.5f)
        assertEquals(300f, pip.y, 0.5f)
        assertEquals(0f, pip.air, 0.01f)
    }

    @Test fun leftAloneHeSitsDownAndStopsAskingForFrames() {
        val pip = PipMotion(Random(3)).apply { lively = false }
        pip.moveTo(Offset(0f, 0f))
        var restedAt = -1
        for (frame in 0 until 60 * 30) {
            clock += 16_666_667L
            if (!pip.step(clock) && pip.sit == 1f) { restedAt = frame; break }
        }
        assertTrue("he should settle within half a minute", restedAt >= 0)
        assertTrue(pip.nextWakeMillis() > 0)
    }

    @Test fun reducedMotionGoesStraightThere() {
        val pip = PipMotion(Random(4)).apply { still = true; lively = false }
        pip.moveTo(Offset(0f, 0f)); pip.moveTo(Offset(800f, 200f))
        pip.run(0.05f)
        assertEquals(800f, pip.x, 0f); assertEquals(200f, pip.y, 0f)
        assertFalse(pip.air > 0f)
    }

    @Test fun aCheerIsAHopWithArmsUp() {
        val pip = PipMotion(Random(5))
        pip.moveTo(Offset(0f, 400f))
        pip.celebrate()
        pip.run(0.2f)
        assertTrue(pip.cheer > 0.5f && pip.air > 0.2f && pip.y < 400f)
    }
}
