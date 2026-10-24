package app.stova.ha.cards

import app.stova.ha.*
import app.stova.core.storage.StovaJson
import app.stova.core.theme.AvatarColor
import app.stova.profiles.Profile
import java.util.Locale
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class CardActionsTest {
    private val climate = HaEntity("climate.stue", "heat", StovaJson.parseToJsonElement("""{"temperature":21.5,"min_temp":5,"max_temp":22}""").jsonObject, 0, 0)
    @Test fun climateStepsAndClamps() {
        assertEquals(22.0, climateCall(climate, +0.5).data["temperature"]!!.jsonPrimitive.double, 0.0)
        assertEquals(22.0, climateCall(climate, +2.0).data["temperature"]!!.jsonPrimitive.double, 0.0)
    }
    @Test fun scriptUsesScriptDomain() = assertEquals(ServiceCall("script", "turn_on"), primaryAction(CardType.SCENE, HaEntity("script.x", "off", JsonObject(emptyMap()), 0, 0)))
    @Test fun brightnessClamped() = assertEquals(1, brightnessCall(0).data["brightness_pct"]!!.jsonPrimitive.int)
    @Test fun kidWithoutPermissionCannotControl() { assertFalse(canControl(Profile("k", "K", AvatarColor.ROSE, kid = true))); assertTrue(canControl(Profile("k", "K", AvatarColor.ROSE, kid = true, kidSmartHomeControl = true))) }
}
