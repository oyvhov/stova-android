package app.stova.ha.cards

import app.stova.ha.HaEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PanelStateTest {
    private val light=HaEntity("light.a","on",buildJsonObject {put("brightness",127.5)},0,0)
    @Test fun debounceStartsAgainAfterLastKeyAndFailureRestoresServerValue()=runTest {
        val calls=mutableListOf<ServiceCall>()
        val state=PanelState(backgroundScope,light) {calls+=it;false}
        state.submit(brightnessCall(51),true)
        advanceTimeBy(250);runCurrent();assertTrue(calls.isEmpty())
        state.submit(brightnessCall(52),true)
        assertEquals(132.6,state.shown!!.attributes.number("brightness")!!,0.001)
        advanceTimeBy(299);runCurrent();assertTrue(calls.isEmpty())
        advanceTimeBy(1);runCurrent()
        assertEquals(listOf(brightnessCall(52)),calls)
        assertEquals(light,state.shown);assertTrue(state.failed)
    }
    @Test fun newAdjustmentDoesNotCancelInFlightCallAndLatestIsSentAfterIt()=runTest {
        val answer=CompletableDeferred<Boolean>();val calls=mutableListOf<ServiceCall>()
        val state=PanelState(backgroundScope,light) {calls+=it;if(calls.size==1) answer.await() else true}
        state.submit(brightnessCall(51),true);advanceTimeBy(300);runCurrent()
        state.submit(brightnessCall(60),true);advanceTimeBy(300);runCurrent()
        assertEquals(1,calls.size)
        answer.complete(true);runCurrent()
        assertEquals(listOf(brightnessCall(51),brightnessCall(60)),calls)
        assertFalse(state.pending)
    }
    @Test fun repeatAcceleratesOnlyPercentageSliders() {
        assertEquals(1,percentageStep(0));assertEquals(5,percentageStep(5));assertEquals(10,percentageStep(14))
    }
    @Test fun optimisticMediaAndModeAndPositionArePredicted() {
        val media=light.copy(id="media_player.a",state="playing")
        assertEquals("paused",optimisticEntity(media,ServiceCall("media_player","media_play_pause")).state)
        assertEquals("cool",optimisticEntity(light,ServiceCall("climate","set_hvac_mode",buildJsonObject {put("hvac_mode","cool")})).state)
        val cover=optimisticEntity(light.copy(id="cover.a"),ServiceCall("cover","set_cover_position",buildJsonObject {put("position",40)}))
        assertEquals(40.0,cover.attributes.number("current_position")!!,0.0)
    }
}
