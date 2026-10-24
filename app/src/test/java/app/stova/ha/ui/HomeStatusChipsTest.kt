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
    @Test fun missingIsDash() = assertEquals("–",chipText(CardConfig("3",CardType.SENSOR,"sensor.x"),null,nn).text)
    @Test fun titleIsLimitedTo24Characters() { val text=chipText(CardConfig("4",CardType.MEDIA_PLAYER,"media_player.x"),entity("media_player.x","playing","""{"media_title":"Ein lang tittel som er mykje for lang"}"""),nn).text;assertEquals(24,text.length);assertTrue(text.endsWith("…")) }
    @Test fun unavailableWeatherIsDash() = assertEquals("–",chipText(CardConfig("1",CardType.WEATHER,"weather.home"),entity("weather.home","unavailable","""{"temperature":4.1}"""),nn).text)
}
