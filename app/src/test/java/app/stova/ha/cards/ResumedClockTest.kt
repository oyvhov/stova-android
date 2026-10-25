package app.stova.ha.cards

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class ResumedClockTest {
    @get:Rule val compose=createComposeRule()

    // Review finding 9: the media and price cards kept ticking with the screen off.
    @Test fun ticksOnlyWhileResumedAndCatchesUpOnResume() {
        val owner=object:LifecycleOwner { override val lifecycle=LifecycleRegistry.createUnsafe(this) }
        owner.lifecycle.currentState=Lifecycle.State.RESUMED
        var reads=0L
        var now: State<Long>?=null
        compose.mainClock.autoAdvance=false
        compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            now=rememberResumedNow(1_000) { ++reads }
            Text("Card")
        } }
        compose.mainClock.advanceTimeBy(3_500)
        val ticking=now!!.value
        assertTrue("ticks while resumed",ticking>=3)
        compose.runOnIdle { owner.lifecycle.currentState=Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(100)
        val stopped=now!!.value
        compose.mainClock.advanceTimeBy(10_000)
        assertEquals("no ticks while stopped",stopped,now!!.value)
        compose.runOnIdle { owner.lifecycle.currentState=Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(100)
        assertEquals("one fresh read on resume",stopped+1,now!!.value)
    }

    @Test fun anInactiveClockNeverTicks() {
        var reads=0L
        var now: State<Long>?=null
        compose.mainClock.autoAdvance=false
        compose.setContent { now=rememberResumedNow(1_000,active=false) { ++reads }; Text("Card") }
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(1L,now!!.value)
    }
}
