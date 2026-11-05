package app.stova.ha.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class OfflineUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun cachedLightIsExplainedAndCannotSendFromTheDashboard() {
        val light=HaEntity("light.stova","on",buildJsonObject {put("friendly_name","Stovelys");put("brightness",200)},0,0)
        var calls=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            SmartHomeContent(Profile("p","A",AvatarColor.LIME),listOf(CardConfig("c",CardType.LIGHT,light.id)),
                mapOf(light.id to light),HaStatus.DISCONNECTED,onPerform={_,_->calls++})
        }}
        compose.onNodeWithText("Sist kjende verdiar").assertIsDisplayed()
        compose.onNodeWithText("Stovelys").performClick()
        assertEquals(0,calls)
        compose.onNodeWithText("Prøv igjen").assertIsDisplayed()
    }
    @Test fun disconnectedAddPickerExplainsTheEmptyAreaAndOffersRecovery() {
        val socket=FakeHaConnection().apply {status.value=HaStatus.DISCONNECTED}
        compose.setContent {StovaTheme(reduceMotion=true) {AddCardFlow(socket,{_,_->},{})}}
        compose.onNodeWithText("Kan ikkje nå Home Assistant.").assertIsDisplayed()
        compose.onNodeWithText("Prøv igjen").assertIsDisplayed()
        assertEquals(0,socket.stateRequests)
    }
    @Test fun anOpenClimatePanelStopsAcceptingChangesWhenTheConnectionDrops() {
        val entity=HaEntity("climate.stova","heat",buildJsonObject {
            put("friendly_name","Stovevarme");put("temperature",22.5);put("current_temperature",21)
            put("min_temp",5);put("max_temp",30)
        },0,0)
        val socket=FakeHaConnection()
        var calls=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            CardDialogHost(CardDialog.Climate(entity.id),mapOf(entity.id to entity),socket,null,false,{_,_->calls++},{})
        }}
        compose.onNodeWithTag("slider-temperature").assertIsEnabled()
        compose.runOnIdle {socket.status.value=HaStatus.DISCONNECTED}
        compose.onNodeWithTag("slider-temperature").assertIsNotEnabled()
        compose.onNodeWithTag("slider-temperature").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithTag("slider-temperature").performKeyInput {pressKey(Key.DirectionRight)}
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {assertEquals(0,calls)}
        compose.onNodeWithText("Sist kjende verdiar").assertIsDisplayed()
    }
}
