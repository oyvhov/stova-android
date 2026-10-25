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
}

@Composable
fun CardDialogHost(dialog:CardDialog?,entities:Map<String,HaEntity>,socket:HaConnection,images:HaImageSource?,locked:Boolean,perform:(ServiceCall,String)->Unit,onDismiss:()->Unit) {
    if(dialog==null) return
    val entity=entities[dialog.entityId]
    val locale=LocalConfiguration.current.locales[0]
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=dialog !is CardDialog.Camera)) {
        val windowHeight=with(LocalDensity.current) {LocalWindowInfo.current.containerSize.height.toDp()}
        val frame=if(dialog is CardDialog.Camera) Modifier.fillMaxSize() else Modifier
            .heightIn(max=(windowHeight-96.dp).coerceAtLeast(160.dp)).verticalScroll(rememberScrollState())
        Column(frame.background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            when(dialog) {
                is CardDialog.Brightness -> {
                    var percent by remember(dialog.entityId) { mutableIntStateOf(((entity?.attributes?.number("brightness") ?: 255.0)/255*100).toInt().coerceIn(1,100)) }
                    var changed by remember(dialog.entityId) { mutableStateOf(false) }
                    LaunchedEffect(percent,changed) { if(changed && !locked) { delay(300); perform(brightnessCall(percent),dialog.entityId) } }
                    fun adjust(delta:Int) { if(!locked) {percent=(percent+delta).coerceIn(1,100);changed=true} }
                    Column(Modifier.onPreviewKeyEvent { if(it.type==KeyEventType.KeyDown) when(it.key) { Key.DirectionLeft->{adjust(-10);true}; Key.DirectionRight->{adjust(10);true};else->false } else false }) {
                        Text(stringResource(R.string.ha_brightness)); CardValue(stringResource(R.string.ha_percent,percent))
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            StovaButton(stringResource(R.string.ha_decrease),{adjust(-10)},enabled=!locked)
                            StovaButton(stringResource(R.string.ha_increase),{adjust(10)},enabled=!locked)
                        }
                    }
                }
                is CardDialog.Climate -> {
                    var current by remember(dialog.entityId) { mutableStateOf(entity) }
                    fun adjust(delta:Double) { current?.let { e -> if(!locked) {
                        val call=climateCall(e,delta); current=e.copy(attributes=JsonObject(e.attributes+call.data));perform(call,dialog.entityId)
                    } } }
                    Column(Modifier.onPreviewKeyEvent { if(it.type==KeyEventType.KeyDown) when(it.key) { Key.DirectionLeft->{adjust(-0.5);true}; Key.DirectionRight->{adjust(0.5);true};else->false } else false }) {
                        Text(stringResource(R.string.ha_target)); CardValue(current?.attributes?.number("temperature")?.let {formatNumber(it,locale)} ?: stringResource(R.string.ha_unavailable))
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            StovaButton(stringResource(R.string.ha_decrease),{adjust(-0.5)},enabled=!locked)
                            StovaButton(stringResource(R.string.ha_increase),{adjust(0.5)},enabled=!locked)
                        }
                    }
                }
                is CardDialog.Graph -> {
                    var history by remember(dialog.entityId) { mutableStateOf(emptyList<Double>()) }
                    var energy by remember(dialog.entityId) {mutableStateOf(entity)}
                    LaunchedEffect(dialog.entityId) { if(entity!=null && dialog.today) {energy=socket.energy(entity);history=priceCurve(energy!!)} else history=socket.history(dialog.entityId).map { it.second } }
                    Text(entity?.let(::friendlyName) ?: dialog.entityId)
                    if(dialog.today && entity!=null) {
                        var tomorrow by remember {mutableStateOf(false)}
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            StovaButton(stringResource(R.string.ha_v2_today),{tomorrow=false},primary=!tomorrow)
                            StovaButton(stringResource(R.string.ha_v2_tomorrow),{tomorrow=true},primary=tomorrow)
                        }
                        PriceChart(energy ?: entity,tomorrow,Modifier.width(496.dp).height(180.dp))
                    } else {
                        Text(stringResource(R.string.ha_v2_history),style=MaterialTheme.typography.labelLarge)
                        if(history.size<2) Text(stringResource(R.string.ha_no_history)) else Sparkline(history,Modifier.width(496.dp).height(180.dp))
                    }
                }
                is CardDialog.Camera -> {
                    CardDetail(stringResource(R.string.ha_v2_fullscreen))
                    HaImage("/api/camera_proxy/${dialog.entityId}",images,Modifier.weight(1f).fillMaxWidth(),2_000)
                }
                is CardDialog.Cover -> {
                    Text(entity?.let(::friendlyName) ?: dialog.entityId,style=MaterialTheme.typography.titleLarge)
                    CardValue(entityStateLabel(entity))
                    val features=(entity?.attributes?.number("supported_features") ?: 0.0).toInt()
                    listOf(Triple(R.string.ha_v2_open_cover,"open_cover",1),Triple(R.string.ha_v2_stop_cover,"stop_cover",8),Triple(R.string.ha_v2_close_cover,"close_cover",2)).forEach {(label,service,bit) ->
                        StovaButton(stringResource(label),{if(!locked) perform(ServiceCall("cover",service),dialog.entityId)},enabled=!locked && (features and bit)!=0)
                    }
                }
                is CardDialog.Lock -> {
                    Text(stringResource(R.string.ha_v2_lock_confirm,entity?.let(::friendlyName) ?: dialog.entityId),style=MaterialTheme.typography.titleLarge)
                    StovaButton(stringResource(if(entity?.state=="locked") R.string.ha_v2_unlock_action else R.string.ha_v2_lock_action),{
                        if(!locked) perform(ServiceCall("lock",if(entity?.state=="locked") "unlock" else "lock"),dialog.entityId)
                        onDismiss()
                    },enabled=!locked)
                }
            }
            StovaButton(stringResource(R.string.ha_close),onDismiss)
        }
    }
}
