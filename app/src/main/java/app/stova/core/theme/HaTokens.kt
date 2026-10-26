package app.stova.core.theme

import androidx.compose.ui.graphics.Color

/** Flat, subdued light colour; the action accent is reserved for controls and progress. */
object HaTokens {
    val WarmGlow = Color(0xFFD4B58C)

    /** The sky the weather card is washed in: warm for sun, blue for rain, silver for cloud, deep for night. */
    fun sky(condition: String): Color = when (condition) {
        "sunny" -> Color(0xFFE2B56E)
        "clear-night" -> Color(0xFF5E6FA3)
        "rainy", "pouring" -> Color(0xFF5C87B5)
        "lightning", "lightning-rainy", "exceptional" -> Color(0xFF8B7CBF)
        "snowy", "snowy-rainy", "hail" -> Color(0xFFC9D6E4)
        "fog" -> Color(0xFFA7AFB6)
        "windy", "windy-variant" -> Color(0xFF8EB2AE)
        else -> Color(0xFF98A7BA)
    }

    /** White light as it looks in the room: amber at candle warmth, cream at neutral, a cool blue by daylight. */
    fun whiteColor(kelvin:Float):Color {
        val t=((kelvin-2000f)/4500f).coerceIn(0f,1f)
        return if(t<.45f) androidx.compose.ui.graphics.lerp(Color(0xFFD9A066),Color(0xFFDCCDB0),t/.45f)
        else androidx.compose.ui.graphics.lerp(Color(0xFFDCCDB0),Color(0xFFA9C1DD),(t-.45f)/.55f)
    }

    fun lightColor(hue:Float,saturation:Float):Color =
        Color.hsv(((hue % 360f)+360f)%360f,saturation.coerceIn(0f,100f)/100f*.35f,.8f)

    fun lightColor(red:Float,green:Float,blue:Float):Color {
        val r=red.coerceIn(0f,255f)/255f;val g=green.coerceIn(0f,255f)/255f;val b=blue.coerceIn(0f,255f)/255f
        val max=maxOf(r,g,b);val delta=max-minOf(r,g,b)
        val hue=when {delta==0f->0f;max==r->60f*((g-b)/delta);max==g->60f*((b-r)/delta+2f);else->60f*((r-g)/delta+4f)}
        return lightColor(hue,if(max==0f) 0f else delta/max*100f)
    }
}
