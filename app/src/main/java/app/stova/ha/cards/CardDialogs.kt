package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.ha.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

sealed interface CardDialog {
    val entityId:String
    data class Brightness(override val entityId:String):CardDialog
    data class Climate(override val entityId:String):CardDialog
    data class Graph(override val entityId:String,val today:Boolean=false):CardDialog
    data class Camera(override val entityId:String):CardDialog
    data class Cover(override val entityId:String):CardDialog
    data class Lock(override val entityId:String):CardDialog
    data class Controls(override val entityId:String,val type:CardType):CardDialog
}

fun controlPanel(type:CardType,id:String):CardDialog=when(type) {
    CardType.LIGHT->CardDialog.Brightness(id)
    CardType.CLIMATE->CardDialog.Climate(id)
    CardType.SENSOR->CardDialog.Graph(id)
    CardType.ENERGY_PRICE->CardDialog.Graph(id,true)
    CardType.CAMERA->CardDialog.Camera(id)
    CardType.COVER->CardDialog.Cover(id)
    CardType.LOCK->CardDialog.Lock(id)
    else->CardDialog.Controls(id,type)
}

@Composable
fun CardDialogHost(dialog:CardDialog?,entities:Map<String,HaEntity>,socket:HaConnection,images:HaImageSource?,locked:Boolean,
    perform:(ServiceCall,String)->Unit,onDismiss:()->Unit,room:String?=null,name:String?=null,
    send:suspend (ServiceCall,String)->Boolean={call,id->perform(call,id);true}) {
    if(dialog==null) return
    val entity=entities[dialog.entityId]
    val energyGraph=dialog is CardDialog.Graph && dialog.today
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=dialog !is CardDialog.Camera && !energyGraph)) {
        val windowHeight=with(LocalDensity.current) {LocalWindowInfo.current.containerSize.height.toDp()}
        val frame=if(dialog is CardDialog.Camera) Modifier.fillMaxSize() else Modifier
            .width(if(energyGraph) 640.dp else 480.dp).heightIn(max=(windowHeight-96.dp).coerceAtLeast(160.dp))
            .background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).verticalScroll(rememberScrollState())
        Column(frame.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(name ?: entity?.let(::friendlyName) ?: dialog.entityId,style=MaterialTheme.typography.titleLarge)
            room?.takeIf {it.isNotBlank()}?.let {CardDetail(it)}
            if(locked || dialog.entityId.startsWith("alarm_control_panel.")) CardDetail(stringResource(R.string.ha_action_read_only))
            when(dialog) {
                is CardDialog.Graph -> HistoryPanel(dialog,entity,socket)
                is CardDialog.Camera -> {
                    CardDetail(stringResource(R.string.ha_v2_fullscreen))
                    HaImage("/api/camera_proxy/${dialog.entityId}",images,Modifier.weight(1f).fillMaxWidth(),2_000)
                }
                else -> DeviceControls(dialog,entity,socket,locked,send)
            }
            StovaButton(stringResource(R.string.ha_close),onDismiss)
        }
    }
}

@Composable private fun HistoryPanel(dialog:CardDialog.Graph,entity:HaEntity?,socket:HaConnection) {
    val locale=LocalConfiguration.current.locales[0]
    var history by remember(dialog.entityId) {mutableStateOf(emptyList<Double>())}
    var energy by remember(dialog.entityId) {mutableStateOf(entity)}
    LaunchedEffect(dialog.entityId,entity!=null) {
        if(entity!=null && dialog.today) energy=socket.energy(entity)
        else history=socket.history(dialog.entityId).map {it.second}
    }
    entity?.let {StateValue(it)}
    if(dialog.today && entity!=null) {
        var tomorrow by remember {mutableStateOf(false)}
        var quarters by remember {mutableStateOf(false)}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            StovaButton(stringResource(R.string.ha_v2_today),{tomorrow=false},primary=!tomorrow)
            StovaButton(stringResource(R.string.ha_v2_tomorrow),{tomorrow=true},primary=tomorrow)
            StovaButton(stringResource(R.string.ha_v2_hourly),{quarters=false},primary=!quarters)
            StovaButton(stringResource(R.string.ha_v2_quarter_hours),{quarters=true},primary=quarters)
        }
        PriceChart(energy ?: entity,tomorrow,Modifier.fillMaxWidth().height(132.dp),quarters=quarters,zone=socket.timeZone)
    } else {
        Text(stringResource(R.string.ha_v2_history),style=MaterialTheme.typography.labelLarge)
        if(history.size<2) Text(stringResource(R.string.ha_no_history)) else {
            Sparkline(history,Modifier.fillMaxWidth().height(120.dp))
            CardDetail(stringResource(R.string.ha_action_extrema,formatNumber(history.min(),locale),formatNumber(history.max(),locale)))
        }
        entity?.let {CardDetail(stringResource(R.string.ha_action_changed,panelTime(it.lastChanged,socket.timeZone,locale)))}
    }
}

internal fun panelTime(time:Long,zone:java.time.ZoneId,locale:java.util.Locale):String=
    java.time.Instant.ofEpochMilli(time).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm",locale))
