package app.stova.ha.ui

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
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
@Config(sdk=[30], qualifiers=TV_QUALIFIERS)
class SmartHomeRoomsTest {
    @get:Rule val compose=createComposeRule()
    private val adult=Profile("a","A",AvatarColor.LIME)
    private val cards=listOf(CardConfig("light",CardType.LIGHT,"light.a",name="Leselys",favorite=true),
        CardConfig("pump",CardType.CLIMATE,"climate.a",name="Varmepumpa"))
    private val entities=mapOf("light.a" to HaEntity("light.a","off",buildJsonObject {},0,0),
        "climate.a" to HaEntity("climate.a","heat",buildJsonObject {put("current_temperature",23);put("temperature",22)},0,0))
    private val registry=HaRegistry(mapOf("light.a" to HaEntityInfo(room="Småstova"),"climate.a" to HaEntityInfo(room="Kjøkken")))
    private var calls=0
    private fun room(label:String)=compose.onNode(hasText(label) and SemanticsMatcher.expectValue(
        androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Tab))
    private fun show(reduceMotion:Boolean=true)=compose.setContent {StovaTheme(reduceMotion=reduceMotion) {
        SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,registry=registry,onPerform={_,_->calls++},onConfigure={calls++},onLayoutChange={calls++})
    }}
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun ordinaryRoomNamesFitWithoutBreakingWords() {
        show()
        for(label in listOf("Alle rom","Kjøkken","Småstova")) {
            val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNode(hasText(label) and hasAnyAncestor(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Tab)),useUnmergedTree=true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {it(layouts)}
            assertEquals(label,1,layouts.single().lineCount)
            assertFalse(label,layouts.single().hasVisualOverflow)
        }
    }
    @Test fun selectingARoomChangesOnlyTheVisibleCardsAndEntersItsFirstCard() {
        show()
        room("Kjøkken").requestFocus().performKeyInput {pressKey(Key.DirectionCenter)}
        room("Kjøkken").assertIsSelected()
        compose.onNodeWithText("Varmepumpa").assertIsFocused()
        compose.onNodeWithText("Leselys").assertDoesNotExist()
        room("Alle rom").performClick()
        compose.onNodeWithText("Leselys").assertExists()
        compose.onNodeWithText("Varmepumpa").assertExists()
        assertEquals(0,calls)
    }
    @Test fun leftFromTheFirstColumnFindsTheSelectedRoomAndRightReturnsToTheCard() {
        show()
        compose.onNodeWithText("Leselys").assertIsFocused().performKeyInput {pressKey(Key.DirectionLeft)}
        room("Alle rom").assertIsFocused().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Leselys").assertIsFocused()
        assertEquals(0,calls)
    }
    @Test fun favouritesShowOnlySavedFavouritesWithoutChangingThem() {
        show()
        room("Favorittar").performClick()
        compose.onNodeWithText("Leselys").assertExists()
        compose.onNodeWithText("Varmepumpa").assertDoesNotExist()
        assertTrue(cards.first().favorite)
        assertEquals(0,calls)
    }
    @Test fun aRoomThatDisappearsFallsBackToAllRooms() {
        var liveRegistry by mutableStateOf(registry)
        compose.setContent {StovaTheme(reduceMotion=true) {
            SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,registry=liveRegistry,onPerform={_,_->calls++})
        }}
        room("Kjøkken").performClick()
        compose.runOnIdle {liveRegistry=HaRegistry()}
        room("Alle rom").assertIsSelected()
        compose.onNodeWithText("Leselys").assertExists()
        compose.onNodeWithText("Varmepumpa").assertExists()
        assertEquals(0,calls)
    }
    @Test fun normalMotionSettlesWithTheSelectedRoomsCardFocused() {
        show(reduceMotion=false)
        room("Kjøkken").requestFocus().performKeyInput {pressKey(Key.DirectionCenter)}
        compose.waitForIdle()
        compose.onNodeWithText("Varmepumpa").assertIsFocused().assertIsDisplayed()
        room("Kjøkken").assertIsSelected()
        assertEquals(0,calls)
    }
    @Test @Config(fontScale=2f) fun largeTextCanNavigateFromTheRailIntoARoom() {
        show()
        room("Kjøkken").requestFocus().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Varmepumpa").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithText("Mål 22°").assertIsDisplayed()
        assertEquals(0,calls)
    }
    @Test fun closingAControlPanelKeepsItsRoomAndReturnsToItsCard() {
        var dialog by mutableStateOf<CardDialog?>(null)
        compose.setContent {StovaTheme(reduceMotion=true) {
            SmartHomeContent(adult,cards,entities,HaStatus.CONNECTED,registry=registry,overlayOpen=dialog!=null,
                onDialog={dialog=it},onPerform={_,_->calls++})
            CardDialogHost(dialog,entities,FakeHaConnection(),null,false,{_,_->calls++},{dialog=null})
        }}
        room("Kjøkken").requestFocus().performKeyInput {pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("Varmepumpa").performKeyInput {pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("Lukk").performClick()
        room("Kjøkken").assertIsSelected()
        compose.onNodeWithText("Varmepumpa").assertIsFocused()
        compose.onNodeWithText("Leselys").assertDoesNotExist()
        assertEquals(0,calls)
    }
}
