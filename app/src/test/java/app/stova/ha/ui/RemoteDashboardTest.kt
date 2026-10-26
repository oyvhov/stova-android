package app.stova.ha.ui

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.*
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class RemoteDashboardTest {
    @get:Rule val compose=createComposeRule()
    private val adult=Profile("a","A",AvatarColor.LIME)
    private val cards=listOf(CardConfig("a",CardType.LIGHT,"light.a"),CardConfig("b",CardType.LIGHT,"light.b"),CardConfig("c",CardType.CLIMATE,"climate.c"))
    private val entities=cards.associate {it.entityId to HaEntity(it.entityId,"on",buildJsonObject {put("brightness",128)},0,0)}
    @Test fun contextHintChangesWithFocusThenFadesAfterFourSeconds() {
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED)}}
        compose.onNodeWithText("OK: slå av",substring=true).assertIsDisplayed()
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionRight);pressKey(Key.DirectionRight)}
        compose.onNodeWithText("climate.c").assertIsFocused()
        compose.onNodeWithText("OK: juster temperatur",substring=true).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(4300)
        compose.onNodeWithTag("remote-hint").assertDoesNotExist()
    }
    @Test fun summarySitsBesideTheTitleOnOneLineWithTheActions() {
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED)}}
        val title=compose.onNode(hasText("Smarthus") and isHeading()).getUnclippedBoundsInRoot()
        val lights=compose.onNodeWithText("2 lys på").getUnclippedBoundsInRoot()
        val layout=compose.onNodeWithText("Oppsett").getUnclippedBoundsInRoot()
        fun middle(r:androidx.compose.ui.unit.DpRect)=(r.top+r.bottom).value/2
        assertTrue("Summary must follow the title: $title / $lights",lights.left>=title.right)
        assertTrue("Summary must end before the actions: $lights / $layout",lights.right<=layout.left)
        assertEquals(middle(title),middle(lights),2f)
        assertEquals(middle(title),middle(layout),2f)
    }
    @Test fun atmosphereShowsTheHomePictureAndASkyFromTheWeatherCard() {
        val weather=CardConfig("w",CardType.WEATHER,"weather.home")
        val withWeather=entities+("weather.home" to HaEntity("weather.home","rainy",buildJsonObject {},0,0))
        var shown by mutableStateOf(cards+weather)
        compose.setContent {StovaTheme {SmartHomeContent(adult,shown,withWeather,HaStatus.CONNECTED,backdrop="file:///home-art.jpg")}}
        compose.onNodeWithTag("smarthome-backdrop:file:///home-art.jpg",useUnmergedTree=true).assertExists()
        compose.onNodeWithTag("smarthome-sky:rainy",useUnmergedTree=true).assertExists()
        compose.runOnIdle {shown=cards}
        compose.onNodeWithTag("smarthome-sky:none",useUnmergedTree=true).assertExists()
    }
    @Test fun allLightsNeedsConfirmationAndCancelDoesNothing() {
        val calls=mutableListOf<Pair<ServiceCall,String>>()
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,onPerform={c,id->calls+=c to id})}}
        compose.onNodeWithText("2 lys på").performClick()
        compose.onNodeWithText("Slå av alle 2 lys?").assertExists();assertTrue(calls.isEmpty())
        compose.onNodeWithText("Avbryt").assertIsFocused().performClick();assertTrue(calls.isEmpty())
        compose.onNodeWithText("2 lys på").performClick()
        compose.onNodeWithText("Ja, slå av").performClick()
        assertEquals(listOf("light.a","light.b"),calls.map {it.second})
        assertTrue(calls.all {it.first==ServiceCall("light","turn_off")})
    }
    @Test fun editModeOkOpensCardOptionsAndHeldOkArrowsMoveWithoutServices() {
        var cards by mutableStateOf(this.cards)
        val moved=mutableListOf<String>();var calls=0
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,onPerform={_,_->calls++},onMove={card,delta ->
            moved+=card.id
            val from=cards.indexOfFirst {it.id==card.id};val to=(from+delta).coerceIn(0,cards.lastIndex)
            cards=cards.toMutableList().apply {add(to,removeAt(from))}
        })}}
        compose.onNodeWithText("Rediger").performClick()
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("Storleik").assertExists()
        compose.onNodeWithText("Lukk").performClick()
        compose.onNodeWithText("light.a").assertIsFocused().performKeyInput {keyDown(Key.DirectionCenter);pressKey(Key.DirectionRight)}
        compose.waitForIdle()
        compose.onNodeWithText("light.a").assertIsFocused().performKeyInput {pressKey(Key.DirectionRight);keyUp(Key.DirectionCenter)}
        assertEquals(listOf("a","a"),moved);assertEquals(0,calls)
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.Back)}
        compose.onNodeWithText("Rediger").assertExists()
    }
    @Test fun childHasReadOnlyHintAndNoCardOptionsOrBulkAction() {
        compose.setContent {StovaTheme {SmartHomeContent(adult.copy(kid=true),cards,entities,HaStatus.CONNECTED)}}
        compose.onNodeWithText("OK: vis · Hald OK: vis · Lesemodus").assertExists()
        compose.onNodeWithText("2 lys på").assertHasNoClickAction()
        compose.onNodeWithText("light.a").performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Storleik").assertDoesNotExist();compose.onNodeWithText("Rediger").assertDoesNotExist()
    }
    @Test fun reopeningRestoresPreviouslyFocusedCardEvenOutsideInitialViewport() {
        var showing by mutableStateOf(true);var last:String?=null
        val many=List(18) {CardConfig("$it",CardType.SENSOR,"sensor.$it")}
        compose.setContent {StovaTheme {if(showing) SmartHomeContent(adult,many,emptyMap(),HaStatus.CONNECTED,initialFocusId=last,onCardFocused={last=it})}}
        repeat(4) {compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)};compose.waitForIdle()}
        val remembered=last;assertNotEquals("0",remembered)
        compose.runOnIdle {showing=false};compose.runOnIdle {showing=true}
        compose.onNodeWithText("sensor.$remembered").assertIsFocused().assertIsDisplayed()
    }
    @Test fun dialogCloseRestoresSameCard() {
        var panel by mutableStateOf<CardDialog?>(null)
        compose.setContent {StovaTheme {
            SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,onDialog={panel=it},overlayOpen=panel!=null)
            CardDialogHost(panel,entities,FakeHaConnection(),null,false,{_,_->},{panel=null})
        }}
        compose.onNodeWithText("light.b").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("light.b").performSemanticsAction(SemanticsActions.OnLongClick) {it()}
        compose.onNodeWithText("Lukk").performClick()
        compose.onNodeWithText("light.b").assertIsFocused()
    }
    @Test fun firstOkAfterHeldOkPanelIsNotLost() {
        var panel by mutableStateOf<CardDialog?>(null);var calls=0
        compose.setContent {StovaTheme {
            SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,onPerform={_,_->calls++},onDialog={panel=it},overlayOpen=panel!=null)
            CardDialogHost(panel,entities,FakeHaConnection(),null,false,{_,_->},{panel=null})
        }}
        compose.onNodeWithText("light.a").performKeyInput {keyDown(Key.DirectionCenter)}
        compose.mainClock.advanceTimeBy(800)
        compose.onNode(hasText("light.a") and hasClickAction()).performKeyInput {keyUp(Key.DirectionCenter)}
        compose.onNodeWithText("Lukk").performClick()
        compose.onNodeWithText("light.a").assertIsFocused().performKeyInput {pressKey(Key.DirectionCenter)}
        assertEquals(1,calls)
    }
    @Test fun homeChipOpensItsCardAndNeverCallsAService() {
        var opened:String?=null
        val chip=chipText(cards[1],entities.getValue("light.b"),java.util.Locale.forLanguageTag("nn"))
        compose.setContent {StovaTheme {HomeStatusChips(listOf(chip),onOpenCard={opened=it})}}
        compose.onNodeWithText("På").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("På").performKeyInput {pressKey(Key.DirectionCenter)}
        assertEquals("b",opened)
    }
}
