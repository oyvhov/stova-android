package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.ha.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

@Composable internal fun DeviceControls(dialog:CardDialog,entity:HaEntity?,socket:HaConnection,locked:Boolean,
    send:suspend (ServiceCall,String)->Boolean) {
    val scope=rememberCoroutineScope()
    val latestSend by rememberUpdatedState(send)
    val latestEntity by rememberUpdatedState(entity)
    val latestLocked by rememberUpdatedState(locked)
    val state=remember(dialog.entityId) {PanelState(scope,entity) {call ->
        if(latestLocked || stateText(latestEntity)==StateText.Unavailable || dialog.entityId.startsWith("alarm_control_panel.")) false
        else latestSend(call,dialog.entityId)
    }}
    SideEffect {state.live=entity}
    val shown=state.shown
    if(shown==null) {CardValue(stringResource(R.string.ha_unavailable));return}
    val enabled=!locked && stateText(entity)!=StateText.Unavailable && !entity!!.id.startsWith("alarm_control_panel.")
    val locale=LocalConfiguration.current.locales[0]
    val attrs=shown.attributes
    var confirm by remember(dialog.entityId) {mutableStateOf<ServiceCall?>(null)}
    val dangerousCover=entity?.id?.startsWith("cover.")==true && entity.attributes.text("device_class") in setOf("garage","gate")
    fun act(call:ServiceCall,delayed:Boolean=false) {
        if(!enabled) return
        if((dangerousCover && call.domain=="cover") || call.service=="unlock") confirm=call
        else state.submit(call,delayed)
    }
    val confirmation=confirm
    if(confirmation!=null) {
        Text(stringResource(if(confirmation.service=="unlock") R.string.ha_action_confirm_unlock else R.string.ha_action_confirm_cover),style=MaterialTheme.typography.titleMedium)
        val cancel=remember {FocusRequester()}
        StovaButton(stringResource(R.string.ha_cancel),{confirm=null},Modifier.focusRequester(cancel))
        LaunchedEffect(confirmation) {cancel.requestFocus()}
        StovaButton(stringResource(when(confirmation.service) {
            "unlock"->R.string.ha_action_yes_unlock;"open_cover"->R.string.ha_action_yes_open
            "close_cover"->R.string.ha_action_yes_close;"stop_cover"->R.string.ha_action_yes_stop
            else->R.string.ha_action_yes_move
        }),{confirm=null;if(enabled) state.submit(confirmation)},enabled=enabled)
        return
    }
    when(dialog) {
        is CardDialog.Brightness -> CardValue(stringResource(R.string.ha_percent,((attrs.number("brightness") ?: 0.0)/255*100).roundToInt()))
        is CardDialog.Climate -> CardValue(attrs.number("current_temperature")?.let {formatNumber(it,locale)} ?: entityStateLabel(shown),stringResource(R.string.ha_celsius))
        else -> CardValue(entityStateLabel(shown))
    }
    if(state.pending) CardDetail(stringResource(R.string.ha_v2_pending))
    if(state.failed) CardDetail(stringResource(R.string.ha_v2_failed))
    when(dialog) {
        is CardDialog.Brightness -> {
            val modes=attrs.strings("supported_color_modes")
            if(attrs.number("brightness")!=null || modes.any {it!="onoff"}) {
                val value=((attrs.number("brightness") ?: 255.0)/255*100).roundToInt().coerceIn(1,100).toDouble()
                PanelSlider("brightness",stringResource(R.string.ha_brightness),value,stringResource(R.string.ha_percent,value.toInt()),1.0..100.0,enabled) {act(brightnessCall(it.toInt()),true)}
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                StovaButton(stringResource(if(shown.state=="on") R.string.ha_action_turn_off else R.string.ha_action_turn_on),{act(ServiceCall("light",if(shown.state=="on") "turn_off" else "turn_on"))},enabled=enabled)
                if("color_temp" in modes) listOf(R.string.ha_action_warm to 2700,R.string.ha_v2_neutral_white to 4000,R.string.ha_action_cold to 6000).forEach {(label,kelvin) ->
                    StovaButton(stringResource(label),{act(ServiceCall("light","turn_on",buildJsonObject {
                        val min=attrs.number("min_color_temp_kelvin")?.toInt() ?: 2000
                        val max=(attrs.number("max_color_temp_kelvin")?.toInt() ?: 6500).coerceAtLeast(min)
                        put("color_temp_kelvin",kelvin.coerceIn(min,max))
                    }))},enabled=enabled)
                }
                if(modes.any {it in setOf("hs","rgb","rgbw","rgbww","xy")}) listOf(R.string.ha_action_red to 0,R.string.ha_action_green to 120,R.string.ha_action_blue to 240).forEach {(label,hue) ->
                    StovaButton(stringResource(label),{act(ServiceCall("light","turn_on",buildJsonObject {putJsonArray("hs_color") {add(hue);add(80)}}))},enabled=enabled)
                }
            }
        }
        is CardDialog.Climate -> {
            val min=attrs.number("min_temp") ?: 5.0
            val max=(attrs.number("max_temp") ?: 35.0).coerceAtLeast(min)
            val value=(attrs.number("temperature") ?: min).coerceIn(min,max)
            PanelSlider("temperature",stringResource(R.string.ha_target),value,formatNumber(value,locale)+"°",min..max,enabled,0.5,false) {
                act(ServiceCall("climate","set_temperature",buildJsonObject {put("temperature",it)}),true)
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                attrs.strings("hvac_modes").forEach {mode ->
                    StovaButton(modeLabel(mode),{act(ServiceCall("climate","set_hvac_mode",buildJsonObject {put("hvac_mode",mode)}))},enabled=enabled,primary=shown.state==mode)
                }
                attrs.strings("preset_modes").forEach {preset ->
                    StovaButton(preset,{act(ServiceCall("climate","set_preset_mode",buildJsonObject {put("preset_mode",preset)}))},enabled=enabled,primary=attrs.text("preset_mode")==preset)
                }
            }
        }
        is CardDialog.Cover -> {
            val features=(attrs.number("supported_features") ?: 0.0).toInt()
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(Triple(R.string.ha_v2_open_cover,"open_cover",1),Triple(R.string.ha_v2_stop_cover,"stop_cover",8),Triple(R.string.ha_v2_close_cover,"close_cover",2)).forEach {(label,service,bit) ->
                    StovaButton(stringResource(label),{act(ServiceCall("cover",service))},enabled=enabled && (features and bit)!=0)
                }
            }
            if((features and 4)!=0) {
                var position by remember(entity) {mutableDoubleStateOf(attrs.number("current_position") ?: 0.0)}
                var changed by remember {mutableStateOf(false)}
                LaunchedEffect(position,changed) {if(changed && dangerousCover) {kotlinx.coroutines.delay(300);act(ServiceCall("cover","set_cover_position",buildJsonObject {put("position",position.toInt())}));changed=false}}
                val value=if(dangerousCover) position else attrs.number("current_position") ?: 0.0
                PanelSlider("position",stringResource(R.string.ha_action_position),value,stringResource(R.string.ha_percent,value.toInt()),0.0..100.0,enabled) {
                    if(dangerousCover) {position=it;changed=true} else act(ServiceCall("cover","set_cover_position",buildJsonObject {put("position",it.toInt())}),true)
                }
            }
        }
        is CardDialog.Lock -> {
            StovaButton(stringResource(R.string.ha_v2_lock_action),{act(ServiceCall("lock","lock"))},enabled=enabled && shown.state!="locked")
            StovaButton(stringResource(R.string.ha_v2_unlock_action),{act(ServiceCall("lock","unlock"))},enabled=enabled && shown.state=="locked")
        }
        is CardDialog.Controls -> when(dialog.type) {
            CardType.MEDIA_PLAYER -> {
                CardDetail(attrs.text("media_title").orEmpty())
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.ha_action_previous to "media_previous_track",(if(shown.state=="playing") R.string.ha_pause else R.string.ha_play) to "media_play_pause",R.string.ha_v2_next to "media_next_track").forEach {(label,service) ->
                        StovaButton(stringResource(label),{act(ServiceCall("media_player",service))},enabled=enabled)
                    }
                }
                attrs.number("volume_level")?.let {volume ->
                    PanelSlider("volume",stringResource(R.string.ha_action_volume),volume*100,stringResource(R.string.ha_percent,(volume*100).toInt()),0.0..100.0,enabled) {
                        act(ServiceCall("media_player","volume_set",buildJsonObject {put("volume_level",it/100)}),true)
                    }
                }
                ChoiceButtons(attrs.strings("source_list"),enabled,attrs.text("source")) {act(ServiceCall("media_player","select_source",buildJsonObject {put("source",it)}))}
            }
            CardType.TOGGLE -> {
                val domain=shown.id.substringBefore('.')
                StovaButton(stringResource(if(shown.state=="on") R.string.ha_action_turn_off else R.string.ha_action_turn_on),{act(ServiceCall(domain,if(shown.state=="on") "turn_off" else "turn_on"))},enabled=enabled)
                if(domain=="fan") {
                    val features=(attrs.number("supported_features") ?: 0.0).toInt()
                    if((features and 1)!=0) {
                        val speed=attrs.number("percentage") ?: 0.0
                        PanelSlider("speed",stringResource(R.string.ha_action_speed),speed,stringResource(R.string.ha_percent,speed.toInt()),0.0..100.0,enabled) {
                            act(ServiceCall("fan","set_percentage",buildJsonObject {put("percentage",it.toInt())}),true)
                        }
                    }
                    if((features and 4)!=0) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(R.string.ha_action_forward to "forward",R.string.ha_action_reverse to "reverse").forEach {(label,direction) ->
                            StovaButton(stringResource(label),{act(ServiceCall("fan","set_direction",buildJsonObject {put("direction",direction)}))},enabled=enabled)
                        }
                    }
                }
            }
            CardType.SCENE -> {
                StovaButton(stringResource(R.string.ha_run),{primaryAction(CardType.SCENE,shown)?.let {act(it)}},enabled=enabled)
                val last=attrs.text("last_triggered")?.let {runCatching {haMillis(it)}.getOrNull()} ?: shown.state.let {runCatching {haMillis(it)}.getOrNull()}
                CardDetail(stringResource(R.string.ha_action_last_run,last?.let {panelTime(it,socket.timeZone,locale)} ?: stringResource(R.string.ha_dash)))
            }
            CardType.WEATHER -> WeatherPanel(shown,socket)
            else -> Unit
        }
        else -> Unit
    }
}

internal fun JsonObject.strings(key:String)=(get(key) as? JsonArray).orEmpty().mapNotNull {(it as? JsonPrimitive)?.contentOrNull}

@Composable private fun ChoiceButtons(values:List<String>,enabled:Boolean,selected:String?,onSelect:(String)->Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        values.forEach {value->StovaButton(value,{onSelect(value)},enabled=enabled,primary=value==selected)}
    }
}

@Composable private fun modeLabel(mode:String)=stringResource(when(mode) {
    "heat"->R.string.ha_v2_heat;"cool"->R.string.ha_v2_cool;"auto","heat_cool"->R.string.ha_v2_auto;"off"->R.string.ha_off
    "dry"->R.string.ha_action_dry;"fan_only"->R.string.ha_action_fan_mode;else->R.string.ha_action_mode
})

@Composable private fun WeatherPanel(entity:HaEntity,socket:HaConnection) {
    var days by remember(entity.id) {mutableStateOf(emptyList<ForecastDay>())}
    var hours by remember(entity.id) {mutableStateOf(emptyList<ForecastDay>())}
    var hourly by remember {mutableStateOf(false)}
    LaunchedEffect(entity.id) {days=socket.forecast(entity.id);hours=socket.hourlyForecast(entity.id)}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        StovaButton(stringResource(R.string.ha_action_hours),{hourly=true},primary=hourly)
        StovaButton(stringResource(R.string.ha_action_days),{hourly=false},primary=!hourly)
    }
    val forecast=if(hourly) hours else days
    if(forecast.isEmpty()) CardDetail(stringResource(R.string.ha_v2_forecast_missing))
    val locale=LocalConfiguration.current.locales[0]
    forecast.forEach {day ->
        CardDetail(java.time.Instant.ofEpochMilli(day.time).atZone(socket.timeZone).format(java.time.format.DateTimeFormatter.ofPattern(if(hourly) "EEE HH:mm" else "EEE dd.MM",locale)))
        CardValue(formatNumber(day.high,locale)+"°",day.low?.let {formatNumber(it,locale)+"°"})
    }
}
