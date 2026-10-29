package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.semantics
import app.stova.core.ui.focusOutline
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.stova.core.ui.SheetPages
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaDialog
import app.stova.ha.*

sealed interface CardDialog {
    val entityId:String
    data class Brightness(override val entityId:String):CardDialog
    data class Climate(override val entityId:String,val hiddenModes:Set<String> = emptySet()):CardDialog
    data class Graph(override val entityId:String,val today:Boolean=false):CardDialog
    data class Camera(override val entityId:String):CardDialog
    data class Cover(override val entityId:String):CardDialog
    data class Lock(override val entityId:String):CardDialog
    data class Controls(override val entityId:String,val type:CardType):CardDialog
}

fun controlPanel(type:CardType,id:String,hiddenModes:Set<String> = emptySet()):CardDialog=when(type) {
    CardType.LIGHT->CardDialog.Brightness(id)
    CardType.CLIMATE->CardDialog.Climate(id,hiddenModes)
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
    // Outlives each dialog: a last slider edit is still sent after Back.
    val sendScope=rememberCoroutineScope()
    if(dialog==null) return
    val entity=entities[dialog.entityId]
    val pages=remember(dialog) {SheetPages()}
    val first=remember(dialog) {FocusRequester()}
    val close=remember(dialog) {FocusRequester()}
    var panelStatus by remember(dialog) {mutableStateOf<String?>(null)}
    StovaDialog(onDismiss,onBack=pages::back) {
        val windowHeight=with(LocalDensity.current) {LocalWindowInfo.current.containerSize.height.toDp()}
        val scroll=rememberScrollState()
        LaunchedEffect(pages.current) {
            scroll.scrollTo(0)
            withFrameNanos { }
            val returning=pages.returning;pages.returning=null
            val restored=returning!=null && runCatching {pages.opener(returning).requestFocus()}.getOrDefault(false)
            if(!restored && !runCatching {first.requestFocus(FocusDirection.Enter)}.getOrDefault(false)) runCatching {close.requestFocus()}
        }
        // Name and footer never scroll. A stable frame also keeps pending/error states from jumping.
        val frame=if(dialog is CardDialog.Camera) Modifier.fillMaxSize() else Modifier
            .widthIn(max=640.dp).fillMaxWidth()
            .height((windowHeight-72.dp).coerceIn(160.dp,440.dp))
        Column(frame.background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.extraLarge).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(name ?: entity?.let(::friendlyName) ?: dialog.entityId,style=MaterialTheme.typography.headlineSmall,
                        maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    room?.takeIf {it.isNotBlank()}?.let {CardDetail(it)}
                }
                if(locked || dialog.entityId.startsWith("alarm_control_panel.")) CardDetail(stringResource(R.string.ha_action_read_only))
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides app.stova.core.ui.ScrollOnlyWhenHidden) {
            Column(Modifier.weight(1f).fillMaxWidth().focusRequester(first).focusGroup()
                .then(if(dialog is CardDialog.Camera) Modifier else Modifier.verticalScroll(scroll)),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
            when(dialog) {
                is CardDialog.Graph -> HistoryPanel(dialog,entity,socket)
                is CardDialog.Camera -> {
                    CardDetail(stringResource(R.string.ha_v2_fullscreen))
                    HaImage("/api/camera_proxy/${dialog.entityId}",images,Modifier.weight(1f).fillMaxWidth(),2_000)
                }
                else -> DeviceControls(dialog,entity,socket,locked,send,sendScope,pages,onStatus={panelStatus=it})
            }
            }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                val hint=when {
                    pages.current!=null->R.string.ha_panel_choice_hint
                    locked || dialog is CardDialog.Graph || dialog is CardDialog.Camera ||
                        (dialog is CardDialog.Controls && dialog.type==CardType.WEATHER)->R.string.ha_panel_browse_hint
                    else->R.string.ha_panel_remote_hint
                }
                Text(panelStatus ?: stringResource(hint),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                StovaButton(stringResource(if(pages.current==null) R.string.ha_close else R.string.custom_back),
                    {if(!pages.back()) onDismiss()},Modifier.focusRequester(close),compact=true,quiet=true)
            }
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
        PriceChart(energy ?: entity,tomorrow,Modifier.fillMaxWidth().height(112.dp),quarters=quarters,zone=socket.timeZone,focusDetails=true)
    } else {
        Text(stringResource(R.string.ha_v2_history),style=MaterialTheme.typography.labelLarge)
        if(history.size<2) Text(stringResource(R.string.ha_no_history)) else {
            Sparkline(history,Modifier.fillMaxWidth().height(120.dp))
        }
        val focus=remember {MutableInteractionSource()}
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants=true) {}.focusOutline(focus,MaterialTheme.shapes.medium)
            .focusable(interactionSource=focus).padding(8.dp)) {
            if(history.size>=2) CardDetail(stringResource(R.string.ha_action_extrema,formatNumber(history.min(),locale),formatNumber(history.max(),locale)))
            entity?.let {CardDetail(stringResource(R.string.ha_action_changed,panelTime(it.lastChanged,socket.timeZone,locale)))}
        }
    }
}

internal fun panelTime(time:Long,zone:java.time.ZoneId,locale:java.util.Locale):String=
    java.time.Instant.ofEpochMilli(time).atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm",locale))
