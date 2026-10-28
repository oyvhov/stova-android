package app.stova.ha.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.ha.HaEntity
import app.stova.ha.cards.*
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeStatusChipsUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun capsAtThreeWithoutFocusableNodes() {
        compose.setContent {StovaTheme {HomeStatusChips(List(4) {ChipContent(CardType.SENSOR,"$it")})}}
        compose.onNodeWithText("2").assertExists();compose.onNodeWithText("3").assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).assertCountEquals(0)
    }
    @Test fun iconOnlyPillsShowNoTextNameTheirValueAndSixFit() {
        compose.setContent {StovaTheme {HomeStatusChips(List(7) {ChipContent(CardType.CLIMATE,"2$it°",iconOnly=true)})}}
        compose.onNodeWithText("20°").assertDoesNotExist()
        compose.onNodeWithContentDescription("20°").assertExists()
        compose.onNodeWithContentDescription("25°").assertExists();compose.onNodeWithContentDescription("26°").assertDoesNotExist()
    }
    @Test fun onOffComesFromTranslatedResources() {
        val entity=HaEntity("light.x","on",JsonObject(emptyMap()),0,0)
        compose.setContent {StovaTheme {HomeStatusChips(listOf(chipText(CardConfig("1",CardType.LIGHT,entity.id),entity,Locale.forLanguageTag("nn"))))}}
        compose.onNodeWithText("På").assertExists()
    }
    @Test fun longChipsLeaveTopBarButtonsVisibleAtDoubleFontScale() {
        compose.setContent {StovaTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)) {
            app.stova.navigation.TopBar(app.stova.profiles.Profile("a","A",app.stova.core.theme.AvatarColor.LIME),{},{},
                onSmartHome={},leading={HomeStatusChips(List(3) {ChipContent(CardType.MEDIA_PLAYER,"Ein svært lang medietittel $it")})})
        }}}
        compose.onNodeWithText("Innstillingar").assertIsDisplayed()
        compose.onNodeWithText("Byt profil").assertIsDisplayed()
        repeat(3) {
            val node=compose.onNodeWithText("Ein svært lang medietittel $it").assertIsDisplayed().fetchSemanticsNode()
            org.junit.Assert.assertTrue("Chips must stay on one text line: ${node.boundsInRoot}",node.boundsInRoot.height<=112f)
        }
    }
}
