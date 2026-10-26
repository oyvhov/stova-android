package app.stova.ha.cards

import app.stova.R
import app.stova.ha.HaEntity
import app.stova.core.storage.StovaJson
import app.stova.core.theme.HaTokens
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class LightPresentationTest {
    private fun light(state:String="on",attrs:String="{}")=HaEntity("light.a",state,StovaJson.parseToJsonElement(attrs).jsonObject,0,0)
    @Test fun temperatureWordsRespectBothBoundaries() {
        assertEquals(R.string.ha_v2_warm_white,temperatureWord(2999))
        assertEquals(R.string.ha_v2_neutral_white,temperatureWord(3000))
        assertEquals(R.string.ha_v2_neutral_white,temperatureWord(4500))
        assertEquals(R.string.ha_v2_cool_white,temperatureWord(4501))
    }
    @Test fun glowFollowsBrightnessAndTurnsOff() {
        assertEquals(.08f,lightGlow(light(attrs="""{"brightness":0}""")).alpha,.001f)
        assertEquals(.15f,lightGlow(light(attrs="""{"brightness":127.5}""")).alpha,.001f)
        assertEquals(.22f,lightGlow(light(attrs="""{"brightness":255}""")).alpha,.001f)
        assertEquals(0f,lightGlow(light("off","""{"brightness":255}""")).alpha,0f)
        assertEquals(.22f,lightGlow(light()).alpha,.001f)
    }
    @Test fun temperatureModeIgnoresStaleRgbAndUsesWarmToken() {
        assertEquals(HaTokens.WarmGlow,lightGlow(light(attrs="""{"color_mode":"color_temp","rgb_color":[255,0,0]}""")).color)
    }
    @Test fun whiteLightsGlowWarmOrCoolByTheirKelvin() {
        val warm=lightGlow(light(attrs="""{"color_mode":"color_temp","color_temp_kelvin":2200}""")).color
        val cool=lightGlow(light(attrs="""{"color_mode":"color_temp","color_temp_kelvin":6500}""")).color
        assertTrue("warm white leans amber: $warm",warm.red>warm.blue+.1f)
        assertTrue("cool white leans blue: $cool",cool.blue>=cool.red)
    }
    @Test fun rgbAndHsColorsAreMutedButKeepTheirHue() {
        val rgb=lightGlow(light(attrs="""{"color_mode":"rgb","rgb_color":[255,0,0]}""")).color
        val hs=lightGlow(light(attrs="""{"color_mode":"hs","hs_color":[0,100]}""")).color
        assertEquals(rgb,hs)
        assertTrue(rgb.red>rgb.green)
        assertTrue(rgb.red-rgb.green<=.36f)
    }
}
