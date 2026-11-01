package app.stova.ha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.ui.CinemaLightPicker
import app.stova.ha.ui.HaSettingsContent
import app.stova.ha.ui.nextCinemaLevel
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Cinema mode is set up under Smarthus, only once Home Assistant is: on, lights, level. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class CinemaSettingsTest {
    @get:Rule val compose = createComposeRule()

    private fun settings(address: String?, start: CinemaPrefs = CinemaPrefs()): () -> CinemaPrefs {
        var prefs by mutableStateOf(start)
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HaSettingsContent(if (address == null) HaStatus.NOT_CONFIGURED else HaStatus.CONNECTED, address, emptyList(), {}, {}, { _, _ -> },
                        cinema = prefs, onCinema = { prefs = it })
                }
            }
        }
        return { prefs }
    }

    @Test fun withoutHomeAssistantThereIsNoCinemaMode() {
        settings(null)
        compose.onNodeWithText("Kino-modus").assertDoesNotExist()
    }

    @Test fun turnedOnItOffersTheLightsAndTheLevel() {
        val prefs = settings("http://ha.local:8123")
        compose.onNodeWithText("Lys").assertDoesNotExist()
        compose.onNodeWithText("Demp lysa når Spole spelar").performScrollTo().performClick()
        assertEquals(true, prefs().enabled)
        compose.onNodeWithText("Ingen valde", substring = true).assertExists()
    }

    @Test fun eachLightStepsFromLeftOutThroughOffAndTheLevelsAndRound() {
        assertEquals(listOf<Int?>(0, 5, 10, 20, 30, 50, null), (listOf<Int?>(null) + CinemaPrefs.LEVELS).map { nextCinemaLevel(it, 1) })
        assertEquals(50, nextCinemaLevel(null, -1))
    }

    @Test fun eachLightGetsItsOwnLevelAndOnesNotFoundKeepTheirs() {
        val ha = FakeHaConnection().apply {
            allStates = listOf(HaEntity("light.sofa", "on", JsonObject(emptyMap()), 0, 0), HaEntity("light.hall", "off", JsonObject(emptyMap()), 0, 0),
                HaEntity("switch.tv", "on", JsonObject(emptyMap()), 0, 0))
            registryValue = HaRegistry(mapOf("light.sofa" to HaEntityInfo(name = "Sofa", room = "Stova"), "light.hall" to HaEntityInfo(name = "Gangtak", room = "Gang")))
        }
        var saved: Map<String, Int>? = null
        compose.setContent {
            StovaTheme(reduceMotion = true) { CinemaLightPicker(ha, { ha.registry() }, mapOf("light.gone" to 20), { saved = it }, {}) }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sofa").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Stova").assertExists()
        compose.onNodeWithText("tv", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("Ikkje med", substring = true).assertCountEquals(2)
        // OK steps: left out → off → 5 % → 10 %.
        repeat(3) { compose.onNodeWithText("Gangtak").performClick() }
        compose.onNodeWithText("Sofa").performClick()
        compose.onNodeWithText("Lagre").performClick()
        assertEquals(mapOf("light.gone" to 20, "light.hall" to 10, "light.sofa" to 0), saved)
    }
}
