package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.SheetAction
import app.stova.core.ui.SheetPages
import app.stova.core.ui.StovaIcons
import app.stova.ha.*
import kotlinx.serialization.json.*
import kotlin.math.roundToInt

@Composable internal fun DeviceControls(dialog:CardDialog,entity:HaEntity?,socket:HaConnection,locked:Boolean,
    send:suspend (ServiceCall,String)->Boolean,sendScope:kotlinx.coroutines.CoroutineScope?=null,
    pages:SheetPages=remember {SheetPages()},onStatus:(String?)->Unit={}) {
    val scope=rememberCoroutineScope()
    val latestSend by rememberUpdatedState(send)
    val latestEntity by rememberUpdatedState(entity)
    val latestLocked by rememberUpdatedState(locked)
    val state=remember(dialog.entityId) {PanelState(scope,entity,sendScope ?: scope) {call ->
        if(latestLocked || stateText(latestEntity)==StateText.Unavailable || dialog.entityId.startsWith("alarm_control_panel.")) false
        else latestSend(call,dialog.entityId)
    }}
    val feedback=when {state.pending->stringResource(R.string.ha_v2_pending);state.failed->stringResource(R.string.ha_v2_failed);else->null}
    SideEffect {state.live=entity;onStatus(feedback)}
    DisposableEffect(state) {onDispose {state.close()}}
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
    if(pages.current!=null) {
        DeviceChoices(pages,dialog,shown,enabled) {call->act(call)}
        return
    }
    when(dialog) {
        is CardDialog.Brightness -> PanelReading(StovaIcons.Light,stringResource(R.string.ha_brightness),
            if(shown.state=="off") stringResource(R.string.ha_off) else lightLevel(shown)?.let {stringResource(R.string.ha_percent,it.toInt())} ?: stringResource(R.string.ha_dash))
        is CardDialog.Climate -> PanelReading(StovaIcons.Climate,stringResource(R.string.ha_room_temperature),
            attrs.number("current_temperature")?.let {formatNumber(it,locale)+"°"} ?: stringResource(R.string.ha_dash))
        is CardDialog.Controls -> when(dialog.type) {
            CardType.MEDIA_PLAYER -> PanelReading(StovaIcons.Speaker,stringResource(R.string.ha_action_volume),
                attrs.number("volume_level")?.let {stringResource(R.string.ha_percent,(it*100).roundToInt())} ?: stringResource(R.string.ha_dash),
                attrs.text("media_title") ?: attrs.text("source"))
            CardType.WEATHER -> PanelReading(weatherIcon(shown.state),entityStateLabel(shown),
                attrs.number("temperature")?.let {formatNumber(it,locale)+"°"} ?: stringResource(R.string.ha_dash))
            else -> PanelReading(cardIcon(dialog.type,shown),stringResource(R.string.ha_device_state),entityStateLabel(shown))
        }
        else -> CardValue(entityStateLabel(shown))
    }
    when(dialog) {
        is CardDialog.Brightness -> {
            val modes=attrs.strings("supported_color_modes")
            // The narrow button sits left of the wide rows' centre line, so the remote's Up and Down
            // would pass it by; the route through it is set here (found on the Shield, 2026-11-01).
            val onOff=remember {FocusRequester()}
            val throughButton=Modifier.focusProperties {if(enabled) {down=onOff}}
            if(attrs.number("brightness")!=null || modes.any {it!="onoff"}) {
                val level=lightLevel(shown)
                PanelSlider("brightness",stringResource(R.string.ha_brightness),level ?: 0.0,
                    level?.let {stringResource(R.string.ha_percent,it.toInt())} ?: stringResource(R.string.ha_choose_level),0.0..100.0,enabled,
                    modifier=throughButton) {
                    act(if(it<=0) ServiceCall("light","turn_off") else brightnessCall(it.toInt()),true)
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                StovaButton(stringResource(if(shown.state=="on") R.string.ha_action_turn_off else R.string.ha_action_turn_on),{act(ServiceCall("light",if(shown.state=="on") "turn_off" else "turn_on"))},
                    Modifier.focusRequester(onOff),enabled=enabled,primary=true)
            }
            if(modes.any {it in setOf("color_temp","hs","rgb","rgbw","rgbww","xy")})
                ControlPageLink(pages,"light",stringResource(R.string.ha_light_colour),StovaIcons.Palette,
                    modifier=Modifier.focusProperties {if(enabled) {up=onOff}})
        }
        is CardDialog.Climate -> {
            val min=attrs.number("min_temp") ?: 5.0
            val max=(attrs.number("max_temp") ?: 35.0).coerceAtLeast(min)
            val value=(attrs.number("temperature") ?: min).coerceIn(min,max)
            PanelSlider("temperature",stringResource(R.string.ha_target),value,
                attrs.number("temperature")?.let {formatNumber(it,locale)+"°"} ?: stringResource(R.string.ha_choose_temperature),min..max,enabled,0.5,false) {
                act(ServiceCall("climate","set_temperature",buildJsonObject {put("temperature",it)}),true)
            }
            if(attrs.strings("hvac_modes").any {it !in dialog.hiddenModes})
                ControlPageLink(pages,"hvac",stringResource(R.string.ha_climate_operation),StovaIcons.Climate,modeLabel(shown.state))
            if(attrs.strings("preset_modes").any {it !in dialog.hiddenModes})
                ControlPageLink(pages,"presets",stringResource(R.string.ha_climate_presets),StovaIcons.Rows,
                    attrs.text("preset_mode")?.let {presetLabel(it)})
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
                val features=(attrs.number("supported_features") ?: 0.0).toInt()
                val previous=features and 16!=0
                val playPause=(shown.state=="playing" && features and 1!=0) || (shown.state=="paused" && features and 16384!=0)
                val next=features and 32!=0
                val buttons=!attrs.text("source").equals("TV",true) && enabled && (previous || playPause || next)
                // As for a light: Up and Down go through the buttons, landing on play or pause where there is one.
                val action=remember {FocusRequester()}
                fun lands(here:Boolean)=if(here) Modifier.focusRequester(action) else Modifier
                val lead=if(playPause) 1 else if(previous) 0 else 2
                attrs.number("volume_level")?.let {volume ->
                    PanelSlider("volume",stringResource(R.string.ha_action_volume),volume*100,stringResource(R.string.ha_percent,(volume*100).roundToInt()),0.0..100.0,
                        enabled && (attrs.number("supported_features")==null || features and 4!=0),
                        modifier=Modifier.focusProperties {if(buttons) {down=action}}) {
                        act(ServiceCall("media_player","volume_set",buildJsonObject {put("volume_level",it/100)}),true)
                    }
                }
                if(!attrs.text("source").equals("TV",true)) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(previous) StovaButton(stringResource(R.string.ha_action_previous),{act(ServiceCall("media_player","media_previous_track"))},lands(lead==0),enabled=enabled)
                    if(playPause)
                        StovaButton(stringResource(if(shown.state=="playing") R.string.ha_pause else R.string.ha_play),{act(ServiceCall("media_player","media_play_pause"))},lands(lead==1),enabled=enabled)
                    if(next) StovaButton(stringResource(R.string.ha_v2_next),{act(ServiceCall("media_player","media_next_track"))},lands(lead==2),enabled=enabled)
                }
                if(attrs.strings("source_list").isNotEmpty()) ControlPageLink(pages,"sources",stringResource(R.string.ha_media_source),StovaIcons.Rows,attrs.text("source"),
                    modifier=Modifier.focusProperties {if(buttons) {up=action}})
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

@Composable internal fun modeLabel(mode:String)=stringResource(when(mode) {
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
    ForecastTimeline(forecast,hourly,socket.timeZone,locale)
}
