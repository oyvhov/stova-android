package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
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
}

@Composable
fun CardDialogHost(dialog:CardDialog?,entities:Map<String,HaEntity>,socket:HaConnection,images:HaImageSource?,locked:Boolean,perform:(ServiceCall,String)->Unit,onDismiss:()->Unit) {
    if(dialog==null) return
    val entity=entities[dialog.entityId]
    val locale=LocalConfiguration.current.locales[0]
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=dialog !is CardDialog.Camera)) {
        val frame=if(dialog is CardDialog.Camera) Modifier.fillMaxSize() else Modifier
        Column(frame.background(MaterialTheme.colorScheme.surface).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
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
                    Column(Modifier.onPreviewKeyEvent { if(it.type==KeyEventType.KeyDown) when(it.key) { Key.DirectionDown->{adjust(-0.5);true}; Key.DirectionUp->{adjust(0.5);true};else->false } else false }) {
                        Text(stringResource(R.string.ha_target)); CardValue(current?.attributes?.number("temperature")?.let {formatNumber(it,locale)} ?: stringResource(R.string.ha_unavailable))
                        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            StovaButton(stringResource(R.string.ha_decrease),{adjust(-0.5)},enabled=!locked)
                            StovaButton(stringResource(R.string.ha_increase),{adjust(0.5)},enabled=!locked)
                        }
                    }
                }
                is CardDialog.Graph -> {
                    var history by remember(dialog.entityId) { mutableStateOf(emptyList<Double>()) }
                    LaunchedEffect(dialog.entityId) { history=if(entity!=null && dialog.today) priceCurve(entity) else socket.history(dialog.entityId).map { it.second } }
                    Text(entity?.let(::friendlyName) ?: dialog.entityId)
                    if(history.size<2) Text(stringResource(R.string.ha_no_history)) else Sparkline(history,Modifier.width(496.dp))
                }
                is CardDialog.Camera -> HaImage("/api/camera_proxy/${dialog.entityId}",images,Modifier.weight(1f).fillMaxWidth(),2_000)
            }
            StovaButton(stringResource(R.string.ha_close),onDismiss)
        }
    }
}
