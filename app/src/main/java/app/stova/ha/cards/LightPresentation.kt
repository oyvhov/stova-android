package app.stova.ha.cards

import androidx.compose.ui.graphics.Color
import app.stova.R
import app.stova.core.theme.HaTokens
import app.stova.ha.HaEntity
import app.stova.ha.text
import kotlinx.serialization.json.*

fun temperatureWord(kelvin:Int):Int = when {
    kelvin<3000 -> R.string.ha_v2_warm_white
    kelvin<=4500 -> R.string.ha_v2_neutral_white
    else -> R.string.ha_v2_cool_white
}

data class LightGlow(val color:Color,val alpha:Float,val colored:Boolean=false)
fun lightGlow(entity:HaEntity?):LightGlow {
    val attrs=entity?.attributes
    val colorMode=attrs?.text("color_mode")
    val colored=colorMode !in setOf("color_temp","brightness","onoff","white")
    fun numbers(key:String)= (attrs?.get(key) as? JsonArray)?.mapNotNull {(it as? JsonPrimitive)?.floatOrNull?.takeIf(Float::isFinite)}
    val rgb=if(colored) numbers("rgb_color")?.takeIf {it.size==3} else null
    val hs=if(colored) numbers("hs_color")?.takeIf {it.size==2} else null
    val color=when {rgb!=null -> HaTokens.lightColor(rgb[0],rgb[1],rgb[2]);hs!=null -> HaTokens.lightColor(hs[0],hs[1]);else -> HaTokens.WarmGlow}
    val brightness=attrs?.number("brightness")?.coerceIn(0.0,255.0) ?: 255.0
    return LightGlow(color,if(entity?.state=="on") .08f+.14f*(brightness/255).toFloat() else 0f,rgb!=null || hs!=null)
}
