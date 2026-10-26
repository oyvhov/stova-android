package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.core.theme.*
import app.stova.ha.*
import app.stova.ha.ui.SmartHomeContent
import app.stova.profiles.Profile
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk=[30],qualifiers=TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MixedDashboardLayoutTest {
    @get:Rule val compose=createComposeRule()
    private val light=HaEntity("light.a","on",buildJsonObject {
        put("brightness",24);put("color_temp_kelvin",2700)
    },0,0)
    private val climate=HaEntity("climate.a","heat",buildJsonObject {
        put("current_temperature",25);put("temperature",23.5)
    },0,0)
    private val weather=HaEntity("weather.a","rainy",buildJsonObject {
        put("temperature",9.7);put("temperature_unit","°C")
    },0,0)

    @Test fun smallControlsKeepTheirRoomAndDetailsInsideOneCompactRow() {
        compose.setContent {StovaTheme(reduceMotion=true) {Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            HaCard(CardConfig("l",CardType.LIGHT,light.id,name="Stova"),light,false,{},{},
                room="Småstova",modifier=Modifier.width(158.dp))
            HaCard(CardConfig("c",CardType.CLIMATE,climate.id,name="Varmepumpa"),climate,false,{},{},
                room="Kjøkken",modifier=Modifier.width(158.dp))
        }}}
        listOf("Stova","Varmepumpa").forEach {compose.onNodeWithText(it).assertHeightIsEqualTo(152.dp)}
        mapOf("Stova" to listOf("9 %","Varm kvit","Småstova"),
            "Varmepumpa" to listOf("25","Varme","Mål: 23,5","Kjøkken")).forEach { (name,details) ->
            val card=compose.onNodeWithText(name).getUnclippedBoundsInRoot()
            details.forEach {detail ->
                val text=compose.onNodeWithText(detail,useUnmergedTree=true).getUnclippedBoundsInRoot()
                assertTrue("$detail must be inside $name: $text / $card",text.top>=card.top && text.bottom<=card.bottom && text.left>=card.left && text.right<=card.right)
            }
        }
    }

    @Test fun fiveMixedCardsFitTheFirstBandWithoutClipping() {
        val media=HaEntity("media_player.a","playing",buildJsonObject {put("media_title","TV");put("volume_level",0.14)},0,0)
        val energy=HaEntity("sensor.price","117.7",buildJsonObject {put("unit_of_measurement","øre")},0,0)
        val cards=listOf(CardConfig("w",CardType.WEATHER,weather.id,name="Ute",size=CardSize.L),
            CardConfig("p",CardType.ENERGY_PRICE,energy.id,name="Straumpris",size=CardSize.M),
            CardConfig("l",CardType.LIGHT,light.id,name="Stova"),
            CardConfig("m",CardType.MEDIA_PLAYER,media.id,name="Sonos",size=CardSize.M),
            CardConfig("c",CardType.CLIMATE,climate.id,name="Varmepumpa"))
        val entities=listOf(weather,energy,light,media,climate).associateBy {it.id}
        val registry=HaRegistry(mapOf(light.id to HaEntityInfo(room="Småstova"),climate.id to HaEntityInfo(room="Kjøkken"),media.id to HaEntityInfo(room="Småstova")))
        val profile=Profile("a","A",AvatarColor.OCEAN,smartHome=DashboardPrefs(density=DashboardDensity.COMPACT))
        var calls=0
        compose.setContent {StovaTheme(reduceMotion=true) {
            val input=LocalInputModeManager.current
            SideEffect {input.requestInputMode(InputMode.Keyboard)}
            SmartHomeContent(profile,cards,entities,HaStatus.CONNECTED,registry=registry,onPerform={_,_->calls++})
        }}
        val grid=compose.onNode(hasScrollAction()).getUnclippedBoundsInRoot()
        cards.forEach {card ->
            val bounds=compose.onNodeWithText(card.name!!).getUnclippedBoundsInRoot()
            assertTrue("${card.name} must fit $grid, was $bounds",bounds.top>=grid.top && bounds.bottom<=grid.bottom)
        }
        compose.onNodeWithText("Ute").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Straumpris").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Sonos").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp)}
        compose.onNodeWithText("Straumpris").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight)}
        compose.onNodeWithText("Stova").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Varmepumpa").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionUp);pressKey(Key.DirectionLeft);pressKey(Key.DirectionDown)}
        compose.onNodeWithText("Sonos").assertIsFocused()
        assertEquals(0,calls)
    }

    @Test fun largeWeatherPlacesAFullWidthForecastBelowCurrentConditions() {
        val forecast=List(5) {ForecastDay(it*86_400_000L,"rainy",15.0+it,5.0+it)}
        compose.setContent {StovaTheme(reduceMotion=true) {
            CardFrame(CardConfig("w",CardType.WEATHER,weather.id,size=CardSize.L),weatherIcon(weather.state),"Ute",null,false,{},{},Modifier.width(332.dp)) {
                WeatherCard(weather,forecast,CardSize.L)
            }
        }}
        val current=compose.onNodeWithText("9,7",useUnmergedTree=true).getUnclippedBoundsInRoot()
        val first=compose.onNodeWithText("15°",useUnmergedTree=true).getUnclippedBoundsInRoot()
        val last=compose.onNodeWithText("19°",useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertTrue("Forecast must be below the current conditions",first.top>current.bottom)
        assertTrue("Forecast must use the width of the large card",last.right-first.left>250.dp)
        compose.onNodeWithText("Ute").assertHeightIsEqualTo(320.dp)
        val card=compose.onNodeWithText("Ute").getUnclippedBoundsInRoot()
        (5..9).plus(15..19).forEach {temperature ->
            val bounds=compose.onNodeWithText("$temperature°",useUnmergedTree=true).getUnclippedBoundsInRoot()
            assertTrue("Forecast value must remain inside the card",bounds.left>=card.left && bounds.right<=card.right && bounds.top>=card.top && bounds.bottom<=card.bottom)
        }
    }

    @Test @Config(fontScale=2f) fun smallClimateCanGrowForLargeTextAndDecimalValues() {
        val detailed=climate.copy(attributes=JsonObject(climate.attributes + ("current_temperature" to JsonPrimitive(25.9))))
        compose.setContent {StovaTheme(reduceMotion=true) {
            HaCard(CardConfig("c",CardType.CLIMATE,detailed.id,name="Varmepumpa i vinterhagen"),detailed,false,{},{},
                room="Vinterhagen",modifier=Modifier.width(264.dp))
        }}
        val card=compose.onNodeWithText("Varmepumpa i vinterhagen").getUnclippedBoundsInRoot()
        assertTrue("Large text must be allowed to grow the card",card.bottom-card.top>152.dp)
        listOf("25,9","°C","Varme","Mål: 23,5","Vinterhagen").forEach {detail ->
            val bounds=compose.onNodeWithText(detail,useUnmergedTree=true).getUnclippedBoundsInRoot()
            assertTrue("$detail must remain inside the card",bounds.left>=card.left && bounds.right<=card.right && bounds.top>=card.top && bounds.bottom<=card.bottom)
        }
        val unit=compose.onNodeWithText("°C",useUnmergedTree=true).getUnclippedBoundsInRoot()
        val mode=compose.onNodeWithText("Varme",useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertTrue("Temperature and mode must not overlap",unit.right<=mode.left)
    }
}
