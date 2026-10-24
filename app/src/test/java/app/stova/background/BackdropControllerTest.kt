@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.background
import app.stova.home.*
import app.stova.media.model.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
class BackdropControllerTest {
    @Test fun focusSettlesAfterDelayAndSkipsFastScroll() = runTest {
        val c = BackdropController(backgroundScope)
        c.focus("a"); advanceTimeBy(100); c.focus("b"); advanceTimeBy(100); c.focus("c"); advanceTimeBy(251)
        assertEquals("c", c.current.value)
    }
    @Test fun rotationStartsAfterFocusLeaves() = runTest {
        val c = BackdropController(backgroundScope); c.rotation(listOf("r1", "r2"), enabled = true)
        c.focus("a"); advanceTimeBy(251); c.focus(null); advanceTimeBy(19_999); assertEquals("a", c.current.value)
        advanceTimeBy(2); assertEquals("r1", c.current.value); advanceTimeBy(20_000); assertEquals("r2", c.current.value)
    }
    @Test fun rotationDisabledHolds() = runTest {
        val c = BackdropController(backgroundScope); c.rotation(listOf("r1"), enabled = false)
        c.focus("a"); advanceTimeBy(251); c.focus(null); advanceTimeBy(60_000); assertEquals("a", c.current.value)
    }
}
