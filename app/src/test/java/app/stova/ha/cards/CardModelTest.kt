package app.stova.ha.cards

import app.stova.ha.*
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.profiles.Profile
import java.util.Locale
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CardModelTest {
    private fun e(id: String, state: String = "on", attrs: String = "{}") = HaEntity(id, state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0)
    @Test fun nordpoolOffersPriceFirst() = assertEquals(listOf(CardType.ENERGY_PRICE, CardType.SENSOR), cardTypesFor(e("sensor.nordpool", "1.35", """{"today":[1.2,1.35]}""")))
    @Test fun domains() {
        assertEquals(listOf(CardType.TOGGLE), cardTypesFor(e("fan.kitchen"))); assertEquals(listOf(CardType.SCENE), cardTypesFor(e("script.goodnight")))
        assertEquals(emptyList<CardType>(), cardTypesFor(e("automation.x")))
    }
}
