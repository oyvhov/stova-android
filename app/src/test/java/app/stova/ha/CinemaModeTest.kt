package app.stova.ha

import app.stova.core.storage.JsonFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The chosen lights dim while Spole plays and come back as they were; nothing happens without the setup. */
class CinemaModeTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun light(id: String, state: String, brightness: Int? = null, modes: List<String> = listOf("brightness"), fade: Boolean = false) =
        HaEntity(id, state, buildJsonObject {
            brightness?.let { put("brightness", it) }
            putJsonArray("supported_color_modes") { modes.forEach { add(it) } }
            put("supported_features", if (fade) 32 else 0)
        }, 0, 0)

    @Test fun onlyLightsThatAreOnAndBrighterThanTheLevelAreDimmed() {
        val (calls, was) = cinemaDim(listOf(
            light("light.sofa", "on", 200, fade = true),
            light("light.low", "on", 20),
            light("light.off", "off"),
            light("light.plain", "on", modes = listOf("onoff")),
        ), levels = mapOf("light.sofa" to 10, "light.low" to 10, "light.off" to 10, "light.plain" to 10))
        assertEquals(listOf(
            LightCall("light.sofa", "turn_on", buildJsonObject { put("brightness_pct", 10); put("transition", 2) }),
            // A lamp that cannot dim goes off for the film.
            LightCall("light.plain", "turn_off"),
        ), calls)
        assertEquals(mapOf("light.sofa" to DimmedLight(200), "light.plain" to DimmedLight(null, turnedOff = true)), was)
        // Each light has its own level: the ceiling goes off, the low one too at 0 %, the window to 10 %.
        assertEquals(listOf(LightCall("light.ceiling", "turn_off"), LightCall("light.low", "turn_off"),
            LightCall("light.window", "turn_on", buildJsonObject { put("brightness_pct", 10) })),
            cinemaDim(listOf(light("light.ceiling", "on", 255), light("light.low", "on", 20), light("light.window", "on", 200), light("light.other", "on", 255)),
                mapOf("light.ceiling" to 0, "light.low" to 0, "light.window" to 10)).first)
    }

    @Test fun lightsComeBackAsTheyWereUnlessSomeoneSwitchedThemByHand() {
        val was = mapOf("light.sofa" to DimmedLight(200), "light.plain" to DimmedLight(null, turnedOff = true),
            "light.hand" to DimmedLight(180), "light.gone" to DimmedLight(90))
        val calls = cinemaRestore(listOf(light("light.sofa", "on", 26, fade = true), light("light.plain", "off"), light("light.hand", "off")), was)
        assertEquals(listOf(
            LightCall("light.sofa", "turn_on", buildJsonObject { put("brightness", 200); put("transition", 2) }),
            LightCall("light.plain", "turn_on"),
        ), calls)
    }

    private val store by lazy { JsonFileStore(tmp.root.resolve("cinema.json"), CinemaState.serializer(), CinemaState(), Dispatchers.Unconfined) }

    private fun mode(ha: FakeHaConnection, prefs: CinemaPrefs?, connected: Boolean = true): Pair<CinemaMode, JsonFileStore<CinemaState>> {
        return CinemaMode({ prefs }, store) { block -> if (connected) { block(ha); true } else false } to store
    }

    @Test fun playingDimsAndStoppingPutsBack() = runBlocking {
        val ha = FakeHaConnection().apply { allStates = listOf(light("light.sofa", "on", 200), light("light.hall", "on", 255)) }
        val sent = mutableListOf<Pair<String, String>>()
        ha.service = { domain, service, id, _ -> sent += "$domain.$service" to id; true }
        val (cinema, store) = mode(ha, CinemaPrefs(enabled = true, levels = mapOf("light.sofa" to 10)))
        cinema.onSpole("playing")
        assertEquals(listOf("light.turn_on" to "light.sofa"), sent)
        assertEquals(mapOf("light.sofa" to DimmedLight(200)), store.current().lights)
        // Playing again after a pause that was missed keeps what the light was before the film, not the dim level.
        ha.allStates = listOf(light("light.sofa", "on", 26))
        cinema.playing()
        assertEquals(mapOf("light.sofa" to DimmedLight(200)), store.current().lights)
        sent.clear()
        cinema.onSpole("paused")
        assertEquals(listOf("light.turn_on" to "light.sofa"), sent)
        assertEquals(CinemaState(), store.current())
    }

    @Test fun nothingIsAskedOfHomeAssistantWithoutTheSetupOrWithNothingToPutBack() = runBlocking {
        val ha = FakeHaConnection().apply { allStates = listOf(light("light.sofa", "on", 200)) }
        mode(ha, CinemaPrefs(enabled = false, levels = mapOf("light.sofa" to 10))).first.onSpole("playing")
        mode(ha, CinemaPrefs(enabled = true)).first.onSpole("playing")
        mode(ha, null).first.onSpole("playing")
        mode(ha, CinemaPrefs(enabled = true, levels = mapOf("light.sofa" to 10))).first.onSpole("stopped")
        assertEquals(0, ha.stateRequests)
        assertEquals(0, ha.calls)
    }

    @Test fun whatWasDimmedIsKeptUntilItCanBePutBack() = runBlocking {
        val ha = FakeHaConnection().apply { allStates = listOf(light("light.sofa", "on", 200)) }
        val prefs = CinemaPrefs(enabled = true, levels = mapOf("light.sofa" to 10))
        mode(ha, prefs).first.playing()
        // Home Assistant cannot be reached: the room is remembered for the next try (Stova coming to the front).
        mode(ha, prefs, connected = false).first.restore()
        assertEquals(mapOf("light.sofa" to DimmedLight(200)), store.current().lights)
        // Home Assistant was disconnected from Stova since: there is nothing left to put back through.
        mode(ha, null).first.restore()
        assertEquals(CinemaState(), store.current())
    }
}
