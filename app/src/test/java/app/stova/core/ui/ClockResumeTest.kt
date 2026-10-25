package app.stova.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ClockResumeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stoppedClockDoesNotWakeAndResumingReadsImmediately() {
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry.createUnsafe(this) }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        var reads = 0
        var now = 1_000_000L
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { StovaTheme { Clock(clock = { reads++; now }) } }
        }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(100)
        val stopped = reads
        compose.mainClock.advanceTimeBy(180_000)
        org.junit.Assert.assertEquals(stopped, reads)
        compose.runOnIdle { now += 3_600_000; owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(100)
        org.junit.Assert.assertEquals(stopped + 1, reads)
    }

    @Test fun resumeRefreshesClockBeforeNextMinuteTick() {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        var now = 1_000_000L
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { StovaTheme { Clock(clock = { now }) } }
        }
        compose.onNodeWithText(clockText(now, ZoneId.systemDefault(), Locale.getDefault())).assertExists()
        compose.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            now += 3_600_000
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.onNodeWithText(clockText(now, ZoneId.systemDefault(), Locale.getDefault())).assertExists()
    }
}
