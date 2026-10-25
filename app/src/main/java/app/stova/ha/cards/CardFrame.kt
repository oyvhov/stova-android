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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*

internal val LocalCardFocused = staticCompositionLocalOf { false }

@Composable
fun CardFrame(config: CardConfig, icon: ImageVector, label: String, badge: String?, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit,
    modifier:Modifier=Modifier,glow:LightGlow?=null,pending:Boolean=false,onDirection:((Int)->Boolean)?=null,
    leading:(@Composable (Modifier)->Unit)?=null,content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.large
    val lit=glow!=null && glow.alpha>0f
    val surface=MaterialTheme.colorScheme.surface
    val fill=if(lit) glow.color.copy(alpha=glow.alpha).compositeOver(surface) else surface
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min=if(config.size==CardSize.L) 320.dp else 152.dp)
        .focusOutline(source,shape).clip(shape).background(fill)
        .onPreviewKeyEvent {event -> when {
            event.key==Key.Menu -> {if(event.type==KeyEventType.KeyDown) onLongClick();true}
            focused && event.type==KeyEventType.KeyDown && event.key==Key.DirectionLeft -> onDirection?.invoke(-1) ?: false
            focused && event.type==KeyEventType.KeyDown && event.key==Key.DirectionRight -> onDirection?.invoke(1) ?: false
            else -> false
        }}
        .combinedClickable(interactionSource=source, indication=null, role=Role.Button, onClick=onClick, onLongClick=onLongClick)) {
        leading?.invoke(Modifier.width(if(config.size==CardSize.L) 184.dp else 136.dp).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.medium).background(if(lit) glow.color else MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
                Icon(icon,null,Modifier.size(22.dp),tint=if(lit) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis)
            if (locked) Icon(StovaIcons.Lock,stringResource(R.string.ha_locked),Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            if (pending) Text(stringResource(R.string.ha_v2_pending),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else if (badge != null) Text(badge,Modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.Bottom) {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                CompositionLocalProvider(LocalCardFocused provides focused) {content()}
            }
        }
        }
    }
}
