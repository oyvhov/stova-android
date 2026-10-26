package app.stova.core.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtToneTest {
    private fun picture(vararg runs: Pair<Color, Int>) = runs.flatMap { (c, n) -> List(n) { c.toArgb() } }.toIntArray()

    @Test fun aGreyPictureWithARedCoatLeansRed() {
        val tone = artTone(picture(Color(0.4f, 0.4f, 0.42f) to 900, Color(0.8f, 0.15f, 0.12f) to 100))!!
        val (hue, saturation, _) = tone.hsl()
        assertTrue("hue $hue", hue < 20f || hue > 340f)
        assertTrue(saturation > 0.5f)
    }

    @Test fun aGreyPictureHasNoTone() {
        assertNull(artTone(picture(Color(0.3f, 0.3f, 0.3f) to 500, Color(0.7f, 0.7f, 0.72f) to 500)))
        assertNull(artTone(IntArray(0)))
    }

    @Test fun blackAndBlownOutPixelsBarelyCount() {
        // A blue sky and a deep, nearly black green: the sky decides.
        val tone = artTone(picture(Color(0.25f, 0.45f, 0.85f) to 300, Color(0f, 0.1f, 0f) to 700))!!
        assertTrue(tone.hsl().first in 200f..230f)
    }

    @Test fun hslRoundTripsThroughCompose() {
        for (c in listOf(Color(0.8f, 0.2f, 0.1f), Color(0.1f, 0.6f, 0.3f), Color(0.2f, 0.3f, 0.9f))) {
            val (h, s, l) = c.hsl()
            val back = Color.hsl(h, s, l)
            assertEquals(c.red, back.red, 0.01f); assertEquals(c.green, back.green, 0.01f); assertEquals(c.blue, back.blue, 0.01f)
        }
    }

    @Test fun theTintKeepsEachColoursLightnessSoContrastHolds() {
        val tone = ArtToneState(mutableStateOf(Color(0.8f, 0.2f, 0.1f)), mutableStateOf(1f))
        val ink = Color(0.07f, 0.06f, 0.1f)
        val tinted = tone.ink(ink)
        assertEquals(ink.hsl().third, tinted.hsl().third, 0.01f)
        assertTrue("leans red", tinted.red > ink.red)
        val light = Color(0.92f, 0.9f, 0.95f)
        assertEquals(light.hsl().third, tone.light(light).hsl().third, 0.01f)
    }

    @Test fun withoutAToneNothingChanges() {
        val off = ArtToneState(mutableStateOf(Color(0.8f, 0.2f, 0.1f)), mutableStateOf(0f))
        val base = Color(0.2f, 0.2f, 0.25f, 0.5f)
        assertEquals(base, off.surface(base))
    }
}
