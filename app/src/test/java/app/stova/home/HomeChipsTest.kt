package app.stova.home

import app.stova.ha.HaEntity
import app.stova.ha.cards.CardConfig
import app.stova.ha.cards.CardType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeChipsTest {
    @Test fun unrelatedEntitiesAndTimestampsDoNotUpdateTheHomeSlot() = runTest {
        val cards = MutableStateFlow(listOf(CardConfig("light", CardType.LIGHT, "light.home", onHome = true)))
        fun light(time: Long, state: String = "on") = HaEntity("light.home", state, JsonObject(emptyMap()), time, time)
        val entities = MutableStateFlow(mapOf("light.home" to light(0)))
        var emissions = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            homeChipContents(cards, entities, Locale.ENGLISH).collect { emissions++ }
        }
        repeat(10) { n ->
            entities.value = mapOf("light.home" to light(n.toLong()), "sensor.other" to light(n.toLong()).copy(id = "sensor.other"))
        }
        assertEquals(1, emissions)
        entities.value = mapOf("light.home" to light(11, "off"))
        assertEquals(2, emissions)
    }
}
