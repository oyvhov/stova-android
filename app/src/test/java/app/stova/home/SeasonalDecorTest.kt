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
    @Test fun halloweenNeverShowsACountdownInEitherHeaderEvenOnTheDay() {
        var choice by mutableStateOf(SeasonChoice.HALLOWEEN)
        var date by mutableStateOf(LocalDate.of(2026, 10, 25))
        compose.setContent { StovaTheme(appearance = choice.applyTo(AppearancePrefs(showCountdown = true)), date = date) {
            Column {
                app.stova.navigation.TopBar(Profile("a", "A", app.stova.core.theme.AvatarColor.LIME), {}, {})
                HeroPanel(null)
            }
        } }
        for (mode in listOf(SeasonChoice.HALLOWEEN, SeasonChoice.CALENDAR)) for (day in listOf(25, 31)) {
            compose.runOnIdle { choice = mode; date = LocalDate.of(2026, 10, day) }
            compose.onAllNodesWithText("6 dagar til Halloween").assertCountEquals(0)
            compose.onAllNodesWithText("Halloween i dag").assertCountEquals(0)
            compose.onNodeWithText("God Halloween").assertExists()
        }
    }

    @Test fun seasonMarkSitsAtTheLeftEdgeOfTheTopBar() {
        compose.setContent { StovaTheme(appearance = SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs()), date = LocalDate.of(2026, 10, 25)) {
            app.stova.navigation.TopBar(Profile("a", "A", app.stova.core.theme.AvatarColor.LIME), {}, {})
        } }
        val mark = compose.onNodeWithTag("season-mark", useUnmergedTree = true).getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue("Mark at the left safe edge: $mark", mark.left.value in 40f..60f)
    }

    @Test fun christmasCountdownRespectsThePreferenceInBothHeaders() {
        var enabled by mutableStateOf(true)
        compose.setContent { StovaTheme(appearance = SeasonChoice.CHRISTMAS.applyTo(AppearancePrefs(showCountdown = enabled)), date = LocalDate.of(2026, 12, 20)) {
            Column {
                app.stova.navigation.TopBar(Profile("a", "A", app.stova.core.theme.AvatarColor.LIME), {}, {})
                HeroPanel(null)
            }
        } }
        compose.onAllNodesWithText("4 dagar til julaftan").assertCountEquals(2)
        compose.runOnIdle { enabled = false }
        compose.onAllNodesWithText("4 dagar til julaftan").assertCountEquals(0)
    }
    @Test fun halloweenKeepsItsGreetingWithoutAnyCountdown() {
        var decor by mutableStateOf(SeasonalDecor.CALM)
        compose.setContent { StovaTheme(appearance=SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs(seasonalDecor=decor,reduceMotion=true)),date=LocalDate.of(2026,10,25)) {
            HeroPanel(null)
        } }
        compose.onNodeWithText("God Halloween").assertIsDisplayed()
        compose.onNodeWithText("6 dagar til Halloween").assertDoesNotExist()
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
    // Review finding 8: with the defaults Home redrew 20 times a second all day from 20 Oct to 13 Jan.
    @Test fun idleHomeFadesTheParticlesAndTheClockRestsUntilTheNextKey() {
        var clock: SeasonalClock?=null
        compose.mainClock.autoAdvance=false
        compose.setContent { StovaTheme(appearance=SeasonChoice.CHRISTMAS.applyTo(AppearancePrefs())) {
            clock=rememberSeasonalClock()
            Text("Clock")
        } }
        compose.mainClock.advanceTimeBy(((SEASON_REST_SECONDS+SEASON_FADE_SECONDS)*1_000).toLong()+500)
        val resting=clock!!.time.value
        assertEquals(0f,clock!!.presence)
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals("no frames while resting",resting,clock!!.time.value)
        compose.runOnIdle { SeasonalInput.report() }
        compose.mainClock.advanceTimeBy(500)
        assertTrue("a key wakes the clock",clock!!.time.value>resting)
        assertTrue("and the particles fade back in, never pop",clock!!.presence in 0.01f..0.99f)
        compose.mainClock.advanceTimeBy(3_000)
        assertEquals(1f,clock!!.presence)
    }
    @Test fun presenceFadesOutOnlyAfterTheRest() {
        assertEquals(1f,seasonalPresence(idleSeconds=0f,awakeSeconds=10f))
        assertEquals(1f,seasonalPresence(idleSeconds=SEASON_REST_SECONDS,awakeSeconds=500f))
        assertTrue(seasonalPresence(SEASON_REST_SECONDS+SEASON_FADE_SECONDS/2,500f) in 0.4f..0.6f)
        assertEquals(0f,seasonalPresence(SEASON_REST_SECONDS+SEASON_FADE_SECONDS,500f))
        assertEquals(0f,seasonalPresence(idleSeconds=0f,awakeSeconds=0f))
        assertTrue("the spider has dropped before the rest",SEASON_REST_SECONDS>50f)
    }
    @Test fun aKeyWindsTheSpiderUpInsteadOfMakingItVanish() {
        assertEquals(1f,spiderShown(idleSeconds=0f,retractFrom=1f,sinceKey=0f))
        assertTrue(spiderShown(0f,1f,SPIDER_RETRACT_SECONDS/2) in 0.4f..0.6f)
        assertEquals(0f,spiderShown(0f,1f,SPIDER_RETRACT_SECONDS))
        assertEquals(spiderDrop(48f),spiderShown(48f,0f,48f))
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
        // Light snow across the whole screen: enough to fill it, few enough to stay calm.
        for (season in listOf(Season.CHRISTMAS, Season.HALLOWEEN)) {
            val particles = seasonParticles(season)
            assertTrue(particles.size in 30..64)
            assertTrue(particles.minOf { it.x } < .15f && particles.maxOf { it.x } > .85f)
        }
        assertEquals(3, seasonBats(Season.HALLOWEEN).size)
        assertTrue(seasonBats(Season.CHRISTMAS).isEmpty())
    }
}
