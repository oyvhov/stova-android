package app.stova.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.profiles.*
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class SeasonalDecorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun calmHasGreetingAndCountdownOffHidesBothAndReducedMotionKeepsCalm() {
        var decor by mutableStateOf(SeasonalDecor.CALM)
        compose.setContent { StovaTheme(appearance=SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs(seasonalDecor=decor,reduceMotion=true)),date=LocalDate.of(2026,10,25)) {
            HeroPanel(null)
        } }
        compose.onNodeWithText("God Halloween").assertIsDisplayed()
        compose.onNodeWithText("6 dagar til Halloween").assertIsDisplayed()
        compose.runOnIdle { decor=SeasonalDecor.OFF }
        compose.onNodeWithText("God Halloween").assertDoesNotExist()
        compose.onNodeWithText("6 dagar til Halloween").assertDoesNotExist()
    }
    @Test fun animationClockStopsForMotionAndLifecycleAndRestartsWithoutCatchup() {
        val owner=object:LifecycleOwner { override val lifecycle=LifecycleRegistry.createUnsafe(this) }
        owner.lifecycle.currentState=Lifecycle.State.RESUMED
        var reduce by mutableStateOf(false)
        var clock: State<Float>?=null
        compose.mainClock.autoAdvance=false
        compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            StovaTheme(appearance=SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs(reduceMotion=reduce))) {
                clock=rememberSeasonalTime()
                Text("Clock")
            }
        } }
        compose.mainClock.advanceTimeBy(1_000)
        val moving=clock!!.value; assertTrue(moving>0f)
        compose.runOnIdle { reduce=true }
        compose.mainClock.advanceTimeBy(100)
        val stopped = clock!!.value
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(stopped,clock!!.value)
        compose.runOnIdle { reduce=false; owner.lifecycle.currentState=Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(100)
        val hidden = clock!!.value
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(hidden,clock!!.value)
        compose.runOnIdle { owner.lifecycle.currentState=Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue(clock!!.value>moving)
        assertTrue(clock!!.value<moving+2f)
    }
    @Test fun levelsAndIdleSpiderHaveExplicitLimits() {
        for(season in Season.entries) for(decor in SeasonalDecor.entries) for(reduce in listOf(false,true)) for(started in listOf(false,true)) {
            assertEquals(season!=Season.NONE && decor==SeasonalDecor.FULL && !reduce && started, seasonalMotionEnabled(season,decor,reduce,started))
        }
        assertEquals(0f,spiderDrop(44.99f))
        assertEquals(0f,spiderDrop(45f))
        assertTrue(spiderDrop(47f) in 0f..1f)
        assertEquals(1f,spiderDrop(50f))
        assertEquals(seasonParticles(Season.HALLOWEEN),seasonParticles(Season.HALLOWEEN))
        assertTrue(seasonParticles(Season.CHRISTMAS).size<=18)
    }
}
