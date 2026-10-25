package app.stova.ha

import app.stova.core.storage.StovaJson
import app.stova.ha.cards.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HaEnergyTest {
    private fun e(id:String,attrs:String) = HaEntity(id,"0.742",StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun seaTemperatureTodayIsNotAPriceAndTibberIsDiscoverable() {
        assertEquals(listOf(CardType.SENSOR),cardTypesFor(e("sensor.sea","""{"device_class":"temperature","unit_of_measurement":"°C","today":[{"time":"2026-10-25","temperature":12}]}""")))
        assertEquals(listOf(CardType.ENERGY_PRICE,CardType.SENSOR),cardTypesFor(e("sensor.electricity_price_home","""{"unit_of_measurement":"NOK/kWh"}""")))
        assertEquals(listOf(CardType.ENERGY_PRICE,CardType.SENSOR),cardTypesFor(e("sensor.tibber_strom_pris","""{"unit_of_measurement":"øre"}""")))
    }
    @Test fun tibberReadServiceHasNoTargetAndExplicitResponse() {
        val message=splitIncoming(tibberPricesRequest(4,LocalDate.of(2026,10,25))).single()
        assertNull(message["target"]);assertTrue(message["return_response"]!!.jsonPrimitive.boolean)
        assertEquals("tibber",message["domain"]!!.jsonPrimitive.content)
        assertEquals("get_prices",message["service"]!!.jsonPrimitive.content)
    }
    @Test fun parsesTibberHomePricesAndConvertsOreWithRealIntervals() {
        val response=StovaJson.parseToJsonElement("""{"response":{"prices":{"Midttunet":[{"start_time":"2026-10-25T02:00:00+02:00","price":0.742},{"start_time":"2026-10-25T02:15:00+02:00","price":0.8},{"start_time":"2026-10-26T00:00:00+01:00","price":0.5}]}}}""").jsonObject
        val attributes=parseTibberPrices(response,e("sensor.tibber_strom_pris","""{"unit_of_measurement":"øre"}"""),LocalDate.of(2026,10,25),ZoneId.of("Europe/Oslo"))
        val enriched=e("sensor.tibber_strom_pris","""{"unit_of_measurement":"øre"}""").copy(attributes=attributes)
        assertEquals(74.2,pricePoints(enriched).first().value,0.001)
        assertEquals(900_000L,pricePoints(enriched).first().end-pricePoints(enriched).first().start)
        assertEquals(1,(attributes["raw_tomorrow"] as JsonArray).size)
    }
    @Test fun ambiguousHomesAreNeverMixed() {
        val response=StovaJson.parseToJsonElement("""{"response":{"prices":{"A":[{"start_time":"2026-10-25T01:00:00+02:00","price":1}],"B":[{"start_time":"2026-10-25T01:00:00+02:00","price":2}]}}}""").jsonObject
        assertTrue(parseTibberPrices(response,e("sensor.price","{}"))["raw_today"]==null)
    }
}
