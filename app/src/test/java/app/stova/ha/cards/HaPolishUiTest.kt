package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.StovaTheme
import app.stova.core.storage.StovaJson
import app.stova.ha.HaEntity
import app.stova.ha.ForecastDay
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
class HaPolishUiTest {
    @get:Rule val compose=createComposeRule()
    private fun light(state:String="on")=HaEntity("light.a",state,StovaJson.parseToJsonElement("""{"friendly_name":"Leselys","brightness":222,"color_temp_kelvin":2700}""").jsonObject,0,0)
    @Test fun namePrecedesMainBrightnessAndNoDuplicateOnBadge() {
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),light(),false,{},{},modifier=Modifier.width(264.dp))}}
        val name=compose.onNodeWithText("Leselys",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val value=compose.onNodeWithText("87 %",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertTrue(name.bottom<=value.top)
        compose.onNodeWithText("Varm kvit").assertIsDisplayed()
        compose.onNodeWithText("På").assertDoesNotExist()
    }
    @Test fun smallCardIsCompact152Dp() {
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a"),light("off"),false,{},{},modifier=Modifier.width(264.dp))}}
        compose.onNodeWithText("Leselys").assertHeightIsEqualTo(152.dp)
        compose.onNodeWithText("Av").assertExists()
    }
    @Test fun smallCardNameGetsTheFullTextWidthEvenWithAStatusBadge() {
        compose.setContent {StovaTheme {CardFrame(CardConfig("a",CardType.SENSOR,"sensor.a"),
            app.stova.core.ui.StovaIcons.Sensor,"Temperatur i vinterhagen","Fråkopla",false,{}, {},Modifier.width(264.dp)) {CardValue("21,5")}}}
        compose.onNodeWithText("Temperatur i vinterhagen",useUnmergedTree=true).assertWidthIsAtLeast(220.dp)
    }
    @Test fun largeCardSpansTwoCompactRowsAndGutter() {
        compose.setContent {StovaTheme {HaCard(CardConfig("a",CardType.LIGHT,"light.a",size=CardSize.L),light("off"),false,{},{},modifier=Modifier.width(544.dp))}}
        compose.onNodeWithText("Leselys").assertHeightIsEqualTo(320.dp)
    }
    @Test fun weatherMediumKeepsFiveDaysInOneCompactRow() {
        val weather=HaEntity("weather.home","sunny",StovaJson.parseToJsonElement("""{"temperature":12,"temperature_unit":"°C"}""").jsonObject,0,0)
        val forecast=List(5) {ForecastDay(it*86_400_000L,"sunny",15.0,5.0)}
        compose.setContent {StovaTheme {CardFrame(CardConfig("w",CardType.WEATHER,weather.id,size=CardSize.M),weatherIcon("sunny"),"Vêret",null,false,{},{},modifier=Modifier.width(544.dp)) {WeatherCard(weather,forecast,CardSize.M)}}}
        compose.onNodeWithText("Vêret").assertHeightIsEqualTo(152.dp)
        assertEquals(5,compose.onAllNodesWithText("15°",useUnmergedTree=true).fetchSemanticsNodes().size)
    }
    @Test fun focusedClimateShowsAdjustmentHintAndMode() {
        val climate=HaEntity("climate.home","heat",StovaJson.parseToJsonElement("""{"friendly_name":"Varmepumpe","temperature":23.5,"current_temperature":25}""").jsonObject,0,0)
        compose.setContent {StovaTheme {HaCard(CardConfig("c",CardType.CLIMATE,climate.id),climate,false,{},{},modifier=Modifier.width(264.dp))}}
        compose.onNodeWithText("Varmepumpe").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Mål 23,5°").assertIsDisplayed()
        compose.onNodeWithText("Varme").assertExists()
        val name=compose.onNodeWithText("Varmepumpe",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val value=compose.onNodeWithText("25",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertTrue(name.bottom<=value.top)
    }
    @Test fun mediumArtworkFillsTheEntireLeftSide() {
        compose.setContent {StovaTheme {CardFrame(CardConfig("m",CardType.MEDIA_PLAYER,"media_player.a",size=CardSize.M),app.stova.core.ui.StovaIcons.Play,"Musikk",null,false,{},{},
            modifier=Modifier.width(544.dp),leading={Box(it.testTag("album"))}) {CardValue("Spor")}}}
        val card=compose.onNodeWithText("Musikk").getUnclippedBoundsInRoot()
        val art=compose.onNodeWithTag("album",useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertEquals(card.top,art.top)
        assertEquals(card.bottom,art.bottom)
        assertEquals(card.left,art.left)
        assertEquals(136.dp,art.right-art.left)
    }
}
