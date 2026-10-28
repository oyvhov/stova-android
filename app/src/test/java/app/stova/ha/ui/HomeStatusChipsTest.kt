package app.stova.ha.ui

import app.stova.core.storage.StovaJson
import app.stova.ha.HaEntity
import app.stova.ha.cards.*
import java.util.Locale
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class HomeStatusChipsTest {
    private val nn=Locale.forLanguageTag("nn")
    private fun entity(id:String,state:String,attrs:String="{}")=HaEntity(id,state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun weatherShowsDegrees() = assertEquals("4°",chipText(CardConfig("1",CardType.WEATHER,"weather.home"),entity("weather.home","rainy","""{"temperature":4.1,"temperature_unit":"°C"}"""),nn).text)
    @Test fun priceWithUnit() = assertEquals("135 øre",chipText(CardConfig("2",CardType.ENERGY_PRICE,"sensor.p"),entity("sensor.p","135","""{"unit_of_measurement":"øre"}"""),nn).text)
    @Test fun climateShowsTheRoomTemperatureNotTheMode() {
        val chip=chipText(CardConfig("5",CardType.CLIMATE,"climate.varmepumpe"),entity("climate.varmepumpe","heat","""{"current_temperature":21.5,"temperature":22}"""),nn)
        assertEquals("21,5°",chip.text);assertNull(chip.textResource)
    }
    @Test fun climateWithoutATemperatureNamesTheModeInNynorsk() {
        val chip=chipText(CardConfig("6",CardType.CLIMATE,"climate.varmepumpe"),entity("climate.varmepumpe","heat"),nn)
        assertEquals("",chip.text);assertEquals(app.stova.R.string.ha_v2_heat,chip.textResource)
    }
    @Test fun theCardChoosesAnIconOnlyPill() {
        val config=CardConfig("7",CardType.SENSOR,"sensor.x",chipStyle=ChipStyle.ICON_ONLY)
        assertTrue(chipText(config,entity("sensor.x","4"),nn).iconOnly)
        assertFalse(chipText(config.copy(chipStyle=ChipStyle.ICON_AND_VALUE),entity("sensor.x","4"),nn).iconOnly)
    }
    @Test fun missingIsDash() = assertEquals("–",chipText(CardConfig("3",CardType.SENSOR,"sensor.x"),null,nn).text)
    @Test fun titleIsLimitedTo24Characters() { val text=chipText(CardConfig("4",CardType.MEDIA_PLAYER,"media_player.x"),entity("media_player.x","playing","""{"media_title":"Ein lang tittel som er mykje for lang"}"""),nn).text;assertEquals(24,text.length);assertTrue(text.endsWith("…")) }
    @Test fun unavailableWeatherIsDash() = assertEquals("–",chipText(CardConfig("1",CardType.WEATHER,"weather.home"),entity("weather.home","unavailable","""{"temperature":4.1}"""),nn).text)
}
