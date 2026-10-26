package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.border
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*

internal val LocalCardFocused = staticCompositionLocalOf { false }

@Composable
fun CardFrame(config: CardConfig, icon: ImageVector, label: String, badge: String?, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit,
    modifier:Modifier=Modifier,glow:LightGlow?=null,pending:Boolean=false,onMenu:(()->Unit)?=null,
    onMediaKey:((Key)->Boolean)?=null,onMove:((Int)->Unit)?=null,editing:Boolean=false,inputSuspended:Boolean=false,
    leading:(@Composable (Modifier)->Unit)?=null,wash:androidx.compose.ui.graphics.Brush?=null,content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scope=rememberCoroutineScope()
    val timeout=LocalViewConfiguration.current.longPressTimeoutMillis
    var held by remember {mutableStateOf(false)}
    var consumed by remember {mutableStateOf(false)}
    var longPress by remember {mutableStateOf<Job?>(null)}
    val latestClick by rememberUpdatedState(onClick)
    val latestLongClick by rememberUpdatedState(onLongClick)
    val windowFocused=LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused,windowFocused,inputSuspended) {if(!focused || !windowFocused || inputSuspended) {longPress?.cancel();held=false;consumed=true}}
    DisposableEffect(Unit) {onDispose {longPress?.cancel()}}
    val shape = MaterialTheme.shapes.large
    val lit=glow!=null && glow.alpha>0f
    val surface=MaterialTheme.colorScheme.surface
    val fill=if(lit) glow.color.copy(alpha=glow.alpha).compositeOver(surface) else surface
    val small=config.size==CardSize.S
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min=if(config.size==CardSize.L) 320.dp else 152.dp)
        .focusOutline(source,shape).then(if(editing) Modifier.border(1.dp,MaterialTheme.colorScheme.outline,shape) else Modifier).clip(shape).background(fill)
        .then(if(wash!=null) Modifier.background(wash) else Modifier)
        .onPreviewKeyEvent {event -> when {
            event.key==Key.Menu -> {if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount==0) onMenu?.invoke();true}
            focused && event.key in listOf(Key.DirectionCenter,Key.Enter,Key.NumPadEnter) -> {
                if(event.type==KeyEventType.KeyDown && !held) {
                    held=true;consumed=false
                    longPress=scope.launch {delay(timeout);consumed=true;latestLongClick()}
                } else if(event.type==KeyEventType.KeyUp) {
                    longPress?.cancel();held=false
                    if(!consumed) latestClick()
                }
                true
            }
            focused && held && onMove!=null && event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown) -> {
                longPress?.cancel();consumed=true
                if(event.type==KeyEventType.KeyDown) onMove(when(event.key) {Key.DirectionLeft->-1;Key.DirectionRight->1;Key.DirectionUp->-3;else->3})
                true
            }
            event.type==KeyEventType.KeyDown -> onMediaKey?.invoke(event.key) ?: false
            else -> false
        }}
        .combinedClickable(interactionSource=source, indication=null, role=Role.Button, onClick=onClick, onLongClick=onLongClick)) {
        leading?.invoke(Modifier.width(if(config.size==CardSize.L) 184.dp else 136.dp).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight().padding(if(small) 12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(small) 4.dp else 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(if(small) 28.dp else 36.dp).clip(MaterialTheme.shapes.medium).background(if(lit) glow.color else MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
                Icon(icon,null,Modifier.size(22.dp),tint=if(lit) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(config.size==CardSize.S) Spacer(Modifier.weight(1f))
            else Text(label,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis)
            if (locked) Icon(StovaIcons.Lock,stringResource(R.string.ha_locked),Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.animation.Crossfade(pending to badge, animationSpec = app.stova.core.theme.stovaMotion.fade(), label = "card-status") { (busy, text) ->
                if (busy) Text(stringResource(R.string.ha_v2_pending),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else if (text != null) Text(text,Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(config.size==CardSize.S) Text(label,Modifier.fillMaxWidth(),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis)
        Column(Modifier.weight(1f),verticalArrangement=if(config.size==CardSize.L) Arrangement.Center else Arrangement.Bottom) {
            Column(verticalArrangement=Arrangement.spacedBy(if(small) 3.dp else 4.dp)) {
                CompositionLocalProvider(LocalCardFocused provides focused) {content()}
            }
        }
        }
    }
}
