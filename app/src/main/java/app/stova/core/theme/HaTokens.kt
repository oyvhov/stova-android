package app.stova.core.theme

import androidx.compose.ui.graphics.Color

/** Flat, subdued light colour; the action accent is reserved for controls and progress. */
object HaTokens {
    val WarmGlow = Color(0xFFD4B58C)

    fun lightColor(hue:Float,saturation:Float):Color =
        Color.hsv(((hue % 360f)+360f)%360f,saturation.coerceIn(0f,100f)/100f*.35f,.8f)

    fun lightColor(red:Float,green:Float,blue:Float):Color {
        val r=red.coerceIn(0f,255f)/255f;val g=green.coerceIn(0f,255f)/255f;val b=blue.coerceIn(0f,255f)/255f
        val max=maxOf(r,g,b);val delta=max-minOf(r,g,b)
        val hue=when {delta==0f->0f;max==r->60f*((g-b)/delta);max==g->60f*((b-r)/delta+2f);else->60f*((r-g)/delta+4f)}
        return lightColor(hue,if(max==0f) 0f else delta/max*100f)
    }
}
