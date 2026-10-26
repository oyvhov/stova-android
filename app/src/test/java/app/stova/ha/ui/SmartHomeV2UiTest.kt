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
class SmartHomeV2UiTest {
    @get:Rule val compose=createComposeRule()
    private val adult=Profile("a","A",AvatarColor.LIME)
    @Test fun menuOpensOnSizeSoTheFirstOkEntersTheSizeChoices() {
        compose.setContent { StovaTheme(reduceMotion=true) { SmartHomeContent(adult,
            listOf(CardConfig("a",CardType.SENSOR,"sensor.a")),emptyMap(),HaStatus.CONNECTED) } }
        compose.onRoot().performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Storleik").assertIsFocused().performKeyInput {pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("S · 1 × 1").assertIsFocused()
    }
    @Test fun delayedSizeWriteRestoresTheSameCardAfterItMovesToAnotherBand() {
        var cards by mutableStateOf(List(9) { CardConfig("$it",CardType.SENSOR,"sensor.$it") })
        var delayed: CardConfig? = null
        compose.setContent { StovaTheme(reduceMotion=true) { SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED,
            onConfigure={delayed=it}) } }
        compose.onNodeWithText("sensor.3").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput { pressKey(Key.Menu) }
        compose.onNodeWithText("Storleik").performClick()
        compose.onNodeWithText("L · 2 × 2").performClick()
        compose.onNodeWithText("sensor.3").assertIsFocused()
        compose.runOnIdle { cards=cards.map { if(it.id==delayed?.id) delayed!! else it } }
        compose.onNodeWithText("sensor.3").assertIsFocused().assertIsDisplayed()
    }

    @Test fun layoutChoicesAccumulateWhilePersistenceIsDelayed() {
        val changes=mutableListOf<DashboardPrefs>()
        compose.setContent { StovaTheme { DashboardLayoutDialog(DashboardPrefs(),{changes+=it},{}) } }
        compose.onNodeWithText("Gruppering").performClick()
        compose.onNodeWithText("Kompakt · fleire kort").performClick()
        assertEquals(DashboardPrefs(DashboardGrouping.ROOM,DashboardDensity.COMPACT),changes.last())
    }

    @Test fun backFromSizeChoicesReturnsToCardOptions() {
        compose.setContent { StovaTheme { SmartHomeContent(adult,listOf(CardConfig("a",CardType.SENSOR,"sensor.a")),emptyMap(),HaStatus.CONNECTED) } }
        compose.onRoot().performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Storleik").performClick()
        compose.onNodeWithText("L · 2 × 2").performKeyInput {pressKey(Key.Back)}
        compose.onNodeWithText("Flytt kort").assertExists()
        compose.onNodeWithText("Storleik").assertIsFocused()
    }
    @Test fun downFromHeaderReturnsToCardsInsteadOfFollowingTheButtonsHorizontalPosition() {
        val cards=listOf(CardConfig("a",CardType.SENSOR,"sensor.a",name="Veranda",size=CardSize.L),
            CardConfig("b",CardType.SENSOR,"sensor.b",name="Kontor",size=CardSize.S),
            CardConfig("c",CardType.SENSOR,"sensor.c",name="Stove",size=CardSize.S))
        compose.setContent {StovaTheme(reduceMotion=true) {SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED)}}
        compose.onNodeWithText("Legg til kort").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Veranda").assertIsFocused()
    }
    @Test fun topRowUpReturnsToTheHeaderAndDownRestoresTheLastCard() {
        val cards=listOf(CardConfig("a",CardType.SENSOR,"sensor.a",name="Veranda"),
            CardConfig("b",CardType.SENSOR,"sensor.b",name="Kontor"))
        compose.setContent {StovaTheme(reduceMotion=true) {SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED)}}
        compose.onNodeWithText("Legg til kort").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Veranda").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Kontor").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Legg til kort").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Kontor").assertIsFocused()
    }
    @Test fun mixedCardSizesReachLazyBandsWithoutDiagonalJumpsOrEscapingTheBottom() {
        val cards=listOf(CardConfig("a",CardType.SENSOR,"sensor.a",name="Veranda",size=CardSize.L))+
            listOf("b","c","d","e","f").map {CardConfig(it,CardType.SENSOR,"sensor.$it",size=CardSize.S)}
        compose.setContent {StovaTheme(reduceMotion=true) {SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED)}}
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("sensor.b").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("sensor.d").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("sensor.f").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown);pressKey(Key.DirectionRight)}
        compose.onNodeWithText("sensor.f").assertIsFocused()
    }
    @Test fun mediaPreviewKeepsOneCardFocusStopAndDoesNotPerformActionsWhileNavigating() {
        val entity=HaEntity("media_player.a","playing",buildJsonObject {put("media_title","Musikk")},0,0)
        val cards=listOf(CardConfig("p",CardType.MEDIA_PLAYER,entity.id,name="Spelar",size=CardSize.M),
            CardConfig("s",CardType.SENSOR,"sensor.side"),CardConfig("r",CardType.SENSOR,"sensor.right"),CardConfig("b",CardType.SENSOR,"sensor.below"))
        var calls=0
        compose.setContent {StovaTheme(reduceMotion=true) {SmartHomeContent(adult,cards,mapOf(entity.id to entity),HaStatus.CONNECTED,onPerform={_,_->calls++})}}
        compose.onNodeWithContentDescription("Pause").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Neste spor").assertIsNotEnabled()
        compose.onNodeWithText("Spelar").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("sensor.side").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("sensor.below").assertIsFocused()
        assertEquals(0,calls)
    }
    @Test fun firstCardReceivesInitialRemoteFocus() {
        compose.setContent {StovaTheme {SmartHomeContent(adult,listOf(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys")),emptyMap(),HaStatus.CONNECTED)}}
        compose.onNodeWithText("Leselys").assertIsFocused()
    }
    @Test fun menuChangesSizeThenRestoresFocus() {
        var card by mutableStateOf(CardConfig("a",CardType.LIGHT,"light.a",name="Leselys"))
        compose.setContent {StovaTheme {SmartHomeContent(adult,listOf(card),emptyMap(),HaStatus.CONNECTED,onConfigure={card=it})}}
        compose.onNodeWithText("Leselys").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Leselys").performKeyInput {pressKey(Key.Menu)}
        compose.onNodeWithText("Storleik").performClick()
        compose.onNodeWithText("L · 2 × 2").performClick()
        compose.runOnIdle {assertEquals(CardSize.L,card.size)}
        compose.onNodeWithText("Leselys").assertIsFocused()
    }
    @Test fun roomSectionsAndFavoritesAreVisible() {
        val cards=listOf(CardConfig("a",CardType.SENSOR,"sensor.a",favorite=true),CardConfig("b",CardType.SENSOR,"sensor.b"))
        compose.setContent {StovaTheme {SmartHomeContent(adult.copy(smartHome=DashboardPrefs(grouping=DashboardGrouping.ROOM)),cards,emptyMap(),HaStatus.CONNECTED,registry=HaRegistry(mapOf("sensor.b" to HaEntityInfo(room="Stove"))))}}
        compose.onNodeWithText("Favorittar").assertExists()
        compose.onNodeWithText("Stove").performScrollTo().assertIsDisplayed()
    }
    @Test fun binaryDoorShowsOpenWord() {
        val door=HaEntity("binary_sensor.door","on",buildJsonObject {put("device_class","door")},0,0)
        compose.setContent {StovaTheme {HaCard(CardConfig("d",CardType.SENSOR,door.id),door,false,{},{})}}
        compose.onNodeWithText("Open").assertExists();compose.onNodeWithText("På").assertDoesNotExist()
    }
    @Test fun remoteScrollKeepsEntireFocusedCardInsideGrid() {
        val cards=List(15) {CardConfig("$it",CardType.SENSOR,"sensor.$it")}
        compose.setContent {StovaTheme {SmartHomeContent(adult,cards,emptyMap(),HaStatus.CONNECTED)}}
        repeat(3) {compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)};compose.waitForIdle()}
        val focused=compose.onNode(isFocused())
        val grid=compose.onNode(hasScrollAction()).getUnclippedBoundsInRoot()
        val bounds=focused.getUnclippedBoundsInRoot()
        assertTrue("Focused top $bounds must stay in $grid",bounds.top>=grid.top)
        assertTrue("Focused bottom $bounds must stay in $grid",bounds.bottom<=grid.bottom)
        compose.onNodeWithText("sensor.12").assertIsFocused()
    }
    @Test fun remoteHintHidesWhenFocusLeavesGrid() {
        compose.setContent {StovaTheme {SmartHomeContent(adult,listOf(CardConfig("a",CardType.SENSOR,"sensor.a")),emptyMap(),HaStatus.CONNECTED)}}
        compose.onNodeWithText("OK: utilgjengeleg",substring=true).assertExists()
        compose.onNodeWithText("Rediger").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("OK: utilgjengeleg",substring=true).assertDoesNotExist()
    }
}
