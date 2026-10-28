package app.stova.ha.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.R
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.TopDropdown
import app.stova.ha.HaEntity
import app.stova.ha.cards.*
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ChipDropdownTest {
    @get:Rule val compose = createComposeRule()
    private fun entity(id: String, state: String, attrs: String = "{}") = HaEntity(id, state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0)
    private val lamp = entity("light.stova", "on", """{"brightness":127.5,"supported_color_modes":["brightness"]}""")

    @Test fun aLightSetsItsBrightnessAndOkSwitchesIt() {
        val control = quickControl(CardType.LIGHT, lamp)!!
        assertEquals(50.0, control.value)
        assertEquals(55.0, control.nudge(50.0, right = true, held = false), 0.0)
        assertEquals(60.0, control.nudge(50.0, right = true, held = true), 0.0)
        assertEquals(brightnessCall(55), control.set(55.0))
        // All the way down is off; from off, Right turns it on dimmed.
        assertEquals(ServiceCall("light", "turn_off"), control.set(0.0))
        assertEquals(ServiceCall("light", "turn_off"), control.ok)
        val off = quickControl(CardType.LIGHT, entity("light.stova", "off", """{"supported_color_modes":["brightness"]}"""))!!
        assertEquals(0.0, off.value)
        assertEquals(5.0, off.nudge(0.0, right = true, held = false), 0.0)
        assertEquals(ServiceCall("light", "turn_on"), off.ok)
        assertNull(quickControl(CardType.LIGHT, entity("light.plain", "on", """{"supported_color_modes":["onoff"]}"""))!!.value)
    }

    @Test fun mediaSetsItsVolumeAndClimateItsTarget() {
        val player = quickControl(CardType.MEDIA_PLAYER, entity("media_player.stova", "playing", """{"volume_level":0.3}"""))!!
        assertEquals(30.0, player.value)
        assertEquals(32.0, player.nudge(30.0, right = true, held = false), 0.0)
        assertEquals(0.32, player.set(32.0).data.number("volume_level")!!, 1e-9)
        assertEquals(ServiceCall("media_player", "media_play_pause"), player.ok)
        assertNull(quickControl(CardType.MEDIA_PLAYER, entity("media_player.stova", "off"))!!.ok)
        val heat = quickControl(CardType.CLIMATE, entity("climate.stova", "heat", """{"temperature":21,"min_temp":7,"max_temp":30}"""))!!
        assertEquals(21.0, heat.value)
        assertEquals(20.5, heat.nudge(21.0, right = false, held = true), 0.0)
        assertEquals(30.0, heat.nudge(30.0, right = true, held = false), 0.0)
        assertNull(heat.ok)
        val fan = quickControl(CardType.TOGGLE, entity("fan.stova", "on", """{"supported_features":1,"percentage":40}"""))!!
        assertEquals(40.0 to ServiceCall("fan", "turn_off"), fan.value to fan.ok)
        assertNull(quickControl(CardType.TOGGLE, entity("switch.kaffi", "off"))!!.value)
        assertNull(quickControl(CardType.WEATHER, entity("weather.home", "rainy")))
    }

    // On Home: OK on a light's chip drops a small card. Left and Right set the light and OK switches
    // it; Smarthus has its own button, so the card has no way there. A chip with nothing to set
    // opens Smarthus as before.
    @Test fun aChipDropsACardTheRemoteControls() {
        val sent = mutableListOf<ServiceCall>()
        val opened = mutableListOf<String>()
        var live by mutableStateOf(lamp)
        val chips = listOf(ChipContent(CardType.LIGHT, "", R.string.ha_on, cardId = "lamp"), ChipContent(CardType.WEATHER, "10°", cardId = "weather"))
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                val scope = rememberCoroutineScope()
                HomeStatusChips(chips, onOpenCard = { opened += it }) { chip, close ->
                    TopDropdown(close, 320.dp) {
                        ChipDropdown(chip.icon, "Stova", live, enabled = true, send = { sent += it; live = optimisticEntity(live, it); true },
                            sendScope = scope)
                    }
                }
            }
        }
        compose.onNodeWithText("10°").performClick()
        assertEquals(listOf("weather"), opened)

        compose.onNodeWithText("På").performClick()
        val card = compose.onNode(hasText("Stova") and hasClickAction())
        card.assertIsFocused()
        compose.onNodeWithText("50 %").assertIsDisplayed()
        card.performKeyInput { pressKey(Key.DirectionRight) }
        compose.waitUntil(2_000) { sent.isNotEmpty() }
        assertEquals(listOf(brightnessCall(55)), sent)
        compose.onNodeWithText("55 %").assertIsDisplayed()

        card.performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitUntil(2_000) { sent.size == 2 }
        assertEquals(ServiceCall("light", "turn_off"), sent.last())
        compose.onNodeWithText("Av").assertIsDisplayed()
        compose.onNodeWithText("Meir i Smarthus").assertDoesNotExist()
        assertEquals(listOf("weather"), opened)
    }

    @Test fun backClosesTheCardAndAKidWithoutControlOnlyReadsIt() {
        val sent = mutableListOf<ServiceCall>()
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                val scope = rememberCoroutineScope()
                HomeStatusChips(listOf(ChipContent(CardType.LIGHT, "", R.string.ha_on, cardId = "lamp")), onOpenCard = {}) { _, close ->
                    TopDropdown(close, 320.dp) { ChipDropdown(CardType.LIGHT, "Stova", lamp, enabled = false, send = { sent += it; true }, sendScope = scope) }
                }
            }
        }
        compose.onNodeWithText("På").performClick()
        val card = compose.onNode(hasText("Stova") and hasClickAction())
        card.performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(emptyList<ServiceCall>(), sent)
        compose.onNodeWithText("50 %").assertIsDisplayed()
        card.performKeyInput { pressKey(Key.Back) }
        compose.onNodeWithText("50 %").assertDoesNotExist()
    }
}
