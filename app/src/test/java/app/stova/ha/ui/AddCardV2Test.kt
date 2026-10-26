package app.stova.ha.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class AddCardV2Test {
    @get:Rule val compose=createComposeRule()
    private fun light(id:String)=HaEntity(id,"on",buildJsonObject {put("friendly_name",id.substringAfter('.'))},0,0)
    private data class PickerFixture(val socket:FakeHaConnection,var additions:Int=0,var completions:Int=0)
    private fun picker(existing:Set<String> = emptySet()):PickerFixture {
        val socket=FakeHaConnection().apply {allStates=List(18) {index->
            HaEntity("light.entity_$index","on",buildJsonObject {put("friendly_name","Eining %02d".format(index))},0,0)
        }}
        val fixture=PickerFixture(socket)
        compose.setContent {StovaTheme(reduceMotion=true) {AddCardFlow(socket,{_,_->fixture.additions++},{fixture.completions++},existing=existing)}}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Eining 00").fetchSemanticsNodes().isNotEmpty()}
        return fixture
    }
    private fun assertNoActions(fixture:PickerFixture) {
        assertEquals(0,fixture.socket.calls);assertEquals(0,fixture.additions);assertEquals(0,fixture.completions)
    }
    private fun focusScrolledCameraCategory() {
        compose.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("Alle"))).performScrollToIndex(3)
        compose.onNodeWithText("Kamera").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Kamera").assertIsFocused()
    }
    @Test fun downFromScrolledCategoryReachesEntitiesAndUpRestoresThatCategory() {
        val fixture=picker()
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 00").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Kamera").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun searchCategoriesAndEntitiesFollowTheVisibleVerticalOrder() {
        val fixture=picker()
        compose.onNodeWithText("Søk etter namn eller rom").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Alle").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 00").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Alle").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Søk etter namn eller rom").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun directionalNavigationSkipsAlreadyAddedTilesWithoutAddingAnything() {
        val fixture=picker(setOf("light.entity_0","light.entity_1","light.entity_3","light.entity_4"))
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 02").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 05").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Eining 02").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Kamera").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun rapidGridDirectionsReachLazyRowsAndHoldAtTheOuterEdges() {
        val fixture=picker()
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 00").assertIsFocused()
        compose.onRoot().performKeyInput {repeat(4) {pressKey(Key.DirectionDown)}}
        compose.onNodeWithText("Eining 12").assertIsFocused()
        compose.onRoot().performKeyInput {repeat(2) {pressKey(Key.DirectionRight)}}
        compose.onNodeWithText("Eining 14").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight);pressKey(Key.DirectionDown);pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Eining 17").assertIsFocused()
        compose.onRoot().performKeyInput {repeat(6) {pressKey(Key.DirectionUp)}}
        compose.onNodeWithText("Kamera").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun downStaysOnTheCategoryWhenEveryEntityIsAlreadyAdded() {
        val fixture=picker((0..17).map {"light.entity_$it"}.toSet())
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Kamera").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun downStaysOnTheCategoryWhenDisconnected() {
        val fixture=picker()
        compose.runOnIdle {fixture.socket.status.value=HaStatus.DISCONNECTED}
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Kamera").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun failedLoadRetryIsReachableAndReturnsToTheCategoryAfterReloading() {
        val socket=FakeHaConnection().apply {allStates=listOf(light("light.Lampe"))}
        val fixture=PickerFixture(socket)
        var fail=true
        val connection=object:HaConnection by socket {
            override suspend fun states():List<HaEntity> {
                if(fail) error("Controlled picker load failure")
                return socket.states()
            }
        }
        compose.setContent {StovaTheme(reduceMotion=true) {AddCardFlow(connection,{_,_->fixture.additions++},{fixture.completions++})}}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Prøv igjen").fetchSemanticsNodes().isNotEmpty()}
        focusScrolledCameraCategory()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Prøv igjen").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Kamera").assertIsFocused()
        compose.runOnIdle {fail=false}
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onRoot().performKeyInput {pressKey(Key.DirectionCenter)}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Lampe").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Kamera").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Lampe").assertIsFocused()
        assertNoActions(fixture)
    }
    @Test fun waitsForConnectionAndReloadsAfterReconnect() {
        val socket=FakeHaConnection().apply {status.value=HaStatus.CONNECTING;allStates=listOf(light("light.Lampe"))}
        compose.setContent {StovaTheme {AddCardFlow(socket,{_,_->},{})}}
        compose.onNodeWithContentDescription("Koplar til …").assertExists()
        compose.onNodeWithText("Koplar til …").assertDoesNotExist();assertEquals(0,socket.stateRequests)
        // The picker needs live states, so it holds the connection while shown.
        assertEquals(1,socket.holders)
        compose.runOnIdle {socket.status.value=HaStatus.CONNECTED}
        compose.waitUntil(5_000) {socket.stateRequests==1}
        compose.onNodeWithText("Lampe").assertExists()
        compose.runOnIdle {socket.status.value=HaStatus.DISCONNECTED}
        compose.onNodeWithText("Fråkopla").assertExists()
        compose.runOnIdle {socket.allStates=listOf(light("light.Ny"));socket.status.value=HaStatus.CONNECTED}
        compose.waitUntil(5_000) {socket.stateRequests==2}
        compose.onNodeWithText("Ny").assertExists()
    }
    @Test fun roomsSearchHiddenAndAlreadyAddedAreVisible() {
        val socket=FakeHaConnection().apply {
            allStates=listOf(light("light.Lampe"),light("light.Diagnose"))
            registryValue=HaRegistry(mapOf("light.Lampe" to HaEntityInfo(room="Stove"),"light.Diagnose" to HaEntityInfo(room="Gang",hidden=true)))
        }
        compose.setContent {StovaTheme {AddCardFlow(socket,{_,_->},{},existing=setOf("light.Lampe"))}}
        compose.waitUntil(5_000) {compose.onAllNodesWithText("Stove").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Lagt til").assertExists();compose.onNodeWithText("Diagnose").assertDoesNotExist()
        compose.onNodeWithText("Vis alle").performClick();compose.onNodeWithText("Diagnose").assertExists()
        compose.onNodeWithText("Søk etter namn eller rom").performTextInput("Stove")
        compose.onNodeWithText("Lampe").assertExists();compose.onNodeWithText("Diagnose").assertDoesNotExist()
    }
}
