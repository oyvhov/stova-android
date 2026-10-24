package app.stova.ha.cards

import app.stova.ha.*
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.profiles.Profile
import java.util.Locale
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CardFormatTest {
    @Test fun unavailable() { assertEquals(StateText.Unavailable, stateText(null)); assertEquals(StateText.Unavailable, stateText(HaEntity("sensor.x", "unavailable", JsonObject(emptyMap()), 0, 0))) }
    @Test fun numberWithUnitNynorsk() {
        val s = stateText(HaEntity("sensor.t", "21.0", StovaJson.parseToJsonElement("""{"unit_of_measurement":"°C"}""").jsonObject, 0, 0)) as StateText.Number
        assertEquals("21", formatNumber(s.value, Locale.forLanguageTag("nn"))); assertEquals("4,1", formatNumber(4.06, Locale.forLanguageTag("nn"))); assertEquals("°C", s.unit)
    }
    @Test fun rawTodayPreferred() = assertEquals(listOf(1.0, 2.0), priceCurve(HaEntity("sensor.p", "1", StovaJson.parseToJsonElement("""{"raw_today":[{"value":1.0},{"value":2.0}],"today":[9.0]}""").jsonObject, 0, 0)))
}
