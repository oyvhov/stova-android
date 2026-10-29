package app.stova.ha.cards

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

/** Off is zero; an absent measurement while on remains unknown. Shared by both control sizes. */
internal fun lightLevel(entity:HaEntity):Double? = if(entity.state=="off") 0.0 else
    entity.attributes.number("brightness")?.let {(it/255*100).roundToInt().coerceIn(0,100).toDouble()}

@Composable internal fun PanelReading(icon:ImageVector,label:String,value:String,detail:String?=null) {
    val source=remember {MutableInteractionSource()}
    // Large text scrolls the reading and control separately. It must remain reachable with Up.
    val readingFocus=if(LocalDensity.current.fontScale>=1.5f) Modifier.semantics(mergeDescendants=true) {}
        .focusOutline(source,MaterialTheme.shapes.medium).focusable(interactionSource=source) else Modifier
    Row(Modifier.fillMaxWidth().then(readingFocus),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
        Icon(icon,null,Modifier.size(56.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface)
            detail?.takeIf {it.isNotBlank()}?.let {Text(it,style=MaterialTheme.typography.bodyMedium,maxLines=1,
                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}

@Composable internal fun ControlPageLink(pages:SheetPages,page:String,label:String,icon:ImageVector,value:String?=null) =
    SheetAction(label,icon,{pages.open(page)},Modifier.focusRequester(pages.opener(page)),value=value,opensPage=true)

@Composable internal fun presetLabel(value:String):String {
    val resource=when(value) {
        "none"->R.string.ha_preset_normal;"quiet","silent"->R.string.ha_preset_quiet
        "powerful","boost"->R.string.ha_preset_powerful;"eco"->R.string.ha_preset_eco
        "away"->R.string.ha_preset_away;"comfort"->R.string.ha_preset_comfort
        "sleep"->R.string.ha_preset_sleep;else->null
    }
    return resource?.let {stringResource(it)} ?: value
}

@Composable internal fun DeviceChoices(pages:SheetPages,dialog:CardDialog,entity:HaEntity,enabled:Boolean,onAct:(ServiceCall)->Unit) {
    val attrs=entity.attributes
    val title=when(pages.current) {
        "light"->R.string.ha_light_colour;"hvac"->R.string.ha_climate_operation
        "presets"->R.string.ha_climate_presets;else->R.string.ha_media_source
    }
    Text(stringResource(title),style=MaterialTheme.typography.titleLarge)
    fun choose(call:ServiceCall) {if(enabled) {onAct(call);pages.back()}}
    when(pages.current) {
        "light" -> {
            val modes=attrs.strings("supported_color_modes")
            if("color_temp" in modes) {
                Text(stringResource(R.string.ha_white_tone),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.ha_action_warm to 2700,R.string.ha_v2_neutral_white to 4000,R.string.ha_action_cold to 6000).forEach {(label,kelvin)->
                        StovaButton(stringResource(label),{
                            val min=attrs.number("min_color_temp_kelvin")?.toInt() ?: 2000
                            val max=(attrs.number("max_color_temp_kelvin")?.toInt() ?: 6500).coerceAtLeast(min)
                            choose(ServiceCall("light","turn_on",buildJsonObject {put("color_temp_kelvin",kelvin.coerceIn(min,max))}))
                        },enabled=enabled)
                    }
                }
            }
            if(modes.any {it in setOf("hs","rgb","rgbw","rgbww","xy")}) {
                Text(stringResource(R.string.ha_colour),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.ha_action_red to 0,R.string.ha_action_green to 120,R.string.ha_action_blue to 240).forEach {(label,hue)->
                        StovaButton(stringResource(label),{choose(ServiceCall("light","turn_on",buildJsonObject {putJsonArray("hs_color") {add(hue);add(80)}}))},enabled=enabled)
                    }
                }
            }
        }
        "hvac" -> attrs.strings("hvac_modes").filter {it !in (dialog as CardDialog.Climate).hiddenModes}.forEach {mode->
            StovaButton(modeLabel(mode),{choose(ServiceCall("climate","set_hvac_mode",buildJsonObject {put("hvac_mode",mode)}))},
                Modifier.fillMaxWidth(),enabled=enabled,primary=entity.state==mode)
        }
        "presets" -> attrs.strings("preset_modes").filter {it !in (dialog as CardDialog.Climate).hiddenModes}.forEach {preset->
            StovaButton(presetLabel(preset),{choose(ServiceCall("climate","set_preset_mode",buildJsonObject {put("preset_mode",preset)}))},
                Modifier.fillMaxWidth(),enabled=enabled,primary=attrs.text("preset_mode")==preset)
        }
        "sources" -> attrs.strings("source_list").forEach {source->
            val features=attrs.number("supported_features")?.toInt()
            StovaButton(source,{choose(ServiceCall("media_player","select_source",buildJsonObject {put("source",source)}))},
                Modifier.fillMaxWidth(),enabled=enabled && (features==null || features and 2048!=0),primary=attrs.text("source")==source)
        }
    }
}

/** Readable forecast columns; focus can scroll through all hours with the same D-pad. */
@Composable internal fun ForecastTimeline(forecast:List<ForecastDay>,hourly:Boolean,zone:java.time.ZoneId,locale:java.util.Locale) {
    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(4.dp)) {
        items(forecast,key={it.time}) {day->
            val source=remember {MutableInteractionSource()}
            Column(Modifier.widthIn(min=92.dp).focusOutline(source,MaterialTheme.shapes.medium).focusable(interactionSource=source).padding(10.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(java.time.Instant.ofEpochMilli(day.time).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern(if(hourly) "HH:mm" else "EEE dd.MM",locale)),
                    style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(weatherIcon(day.condition),null,Modifier.size(28.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatNumber(day.high,locale)+"°",style=MaterialTheme.typography.titleLarge)
                day.low?.let {Text(stringResource(R.string.ha_weather_low,formatNumber(it,locale)),style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
}
