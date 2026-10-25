package app.stova.ha.cards

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.storage.StovaJson
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class HaCardTest {
    @get:Rule val compose = createComposeRule()
    private fun entity(state: String = "on", attrs: String = "{}") = HaEntity("light.stove",state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun lightClickPerformsToggle() {
        var call: ServiceCall? = null
        compose.setContent { StovaTheme { HaCard(CardConfig("1",CardType.LIGHT,"light.stove"),entity(),false,{call=it},{}) } }
        compose.onNodeWithText("light.stove").performClick()
        assertEquals(ServiceCall("light","toggle"),call)
    }
    @Test fun lockedCardOpensReadOnlyPanelAndShowsLock() {
        var calls=0; var dialog:CardDialog?=null
        compose.setContent { StovaTheme { HaCard(CardConfig("1",CardType.LIGHT,"light.stove"),entity(),true,{calls++},{dialog=it}) } }
        compose.onNodeWithText("light.stove").performClick()
        compose.onNodeWithContentDescription("Låst").assertExists(); assertEquals(0,calls); assertEquals(CardDialog.Brightness("light.stove"),dialog)
    }
    @Test fun missingEntityShowsUnavailableAndCanFocus() {
        compose.setContent { StovaTheme { HaCard(CardConfig("1",CardType.SENSOR,"sensor.x"),null,false,{},{}) } }
        compose.onNodeWithText("Utilgjengeleg").assertExists().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithText("Utilgjengeleg").assertIsFocused()
    }
    @Test fun sensorShowsValueAndUnit() {
        compose.setContent { StovaTheme { HaCard(CardConfig("1",CardType.SENSOR,"sensor.x"),entity("4.06","""{"unit_of_measurement":"°C"}"""),false,{},{}) } }
        compose.onNodeWithText("4,1").assertExists(); compose.onNodeWithText("°C").assertExists()
    }
    @Test fun cardsSurviveDoubleFontScale() {
        compose.setContent { StovaTheme { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
            HaCard(CardConfig("1",CardType.SENSOR,"sensor.x"),entity("4.06"),false,{},{})
        } } }
        compose.onNodeWithText("4,1").assertIsDisplayed()
    }
    @Test fun energyLongPressRequestsTodayGraph() {
        var dialog:CardDialog?=null
        compose.setContent { StovaTheme { HaCard(CardConfig("1",CardType.ENERGY_PRICE,"sensor.price"),entity("135"),false,{}, {dialog=it}) } }
        compose.onNodeWithText("light.stove").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnLongClick) { it() }
        assertEquals(CardDialog.Graph("light.stove",today=true),dialog)
    }
    @Test fun brightnessDpadDebouncesOneCall() {
        val calls=mutableListOf<ServiceCall>()
        compose.mainClock.autoAdvance=false
        compose.setContent { StovaTheme { CardDialogHost(CardDialog.Brightness("light.stove"),mapOf("light.stove" to entity(attrs="""{"brightness":128}""")),app.stova.ha.FakeHaConnection(),null,false,{call,_->calls+=call},{}) } }
        compose.onNodeWithText("Meir").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithText("Meir").performKeyInput { pressKey(Key.DirectionRight);pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(100); assertTrue(calls.isEmpty())
        compose.mainClock.advanceTimeBy(400); compose.waitForIdle()
        assertEquals(listOf(brightnessCall(70)),calls)
    }
}
