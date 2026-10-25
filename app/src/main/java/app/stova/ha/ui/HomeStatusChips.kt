package app.stova.ha.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
import app.stova.ha.cards.*
import java.util.Locale
import kotlin.math.round
import kotlinx.serialization.json.*

/** Numeric text stays pure; translatable states are resolved from Android resources at render time. */
data class ChipContent(val icon:CardType,val text:String,@param:StringRes val textResource:Int?=null,val symbol:ImageVector?=null)

fun chipText(config:CardConfig,entity:HaEntity?,locale:Locale):ChipContent {
    val state=stateText(entity)
    val symbol=cardIcon(config.type,entity,config.icon)
    if(state==StateText.Unavailable || entity==null) return ChipContent(config.type,"–",symbol=symbol)
    val text=when(config.type) {
        CardType.WEATHER -> entity.attributes.number("temperature")?.let {formatNumber(round(it),locale)+"°"} ?: "–"
        CardType.MEDIA_PLAYER -> entity.attributes["media_title"]?.jsonPrimitive?.contentOrNull?.let {
            if(it.codePointCount(0,it.length)>24) it.substring(0,it.offsetByCodePoints(0,23))+"…" else it
        } ?: "–"
        else -> when(state) {
            is StateText.Number -> listOfNotNull(formatNumber(state.value,locale),state.unit?.takeIf {it.isNotBlank()}).joinToString(" ")
            is StateText.OnOff -> return ChipContent(config.type,"",if(state.on) R.string.ha_on else R.string.ha_off,symbol)
            is StateText.Raw -> state.text
            StateText.Unavailable -> "–"
        }
    }
    return ChipContent(config.type,text,symbol=symbol)
}

@Composable fun HomeStatusChips(chips:List<ChipContent>) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
        chips.take(3).forEach {chip->
            val icon=chip.symbol ?: when(chip.icon) {
                CardType.LIGHT -> StovaIcons.Light;CardType.TOGGLE -> StovaIcons.Toggle
                CardType.CLIMATE -> StovaIcons.Climate;CardType.WEATHER -> StovaIcons.Cloud
                CardType.MEDIA_PLAYER -> StovaIcons.Play;CardType.SCENE -> StovaIcons.Home
                CardType.CAMERA -> StovaIcons.Camera;else -> StovaIcons.Sensor
            }
            Row(Modifier.weight(1f,fill=false).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.medium).padding(horizontal=10.dp,vertical=6.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Icon(icon,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(chip.textResource?.let {stringResource(it)} ?: chip.text,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
