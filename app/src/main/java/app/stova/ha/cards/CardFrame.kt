package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.*

@Composable
fun CardFrame(config: CardConfig, icon: ImageVector, label: String, badge: String?, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit,
    modifier:Modifier=Modifier,active:Boolean=false,pending:Boolean=false,onDirection:((Int)->Boolean)?=null,content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(StovaShapes.Large)
    Column(modifier.fillMaxWidth().heightIn(min=if(config.size==CardSize.L) 336.dp else 160.dp)
        .focusOutline(source,shape).clip(shape).background(MaterialTheme.colorScheme.surface)
        .onPreviewKeyEvent {event -> when {
            event.key==Key.Menu -> {if(event.type==KeyEventType.KeyDown) onLongClick();true}
            event.type==KeyEventType.KeyDown && event.key==Key.DirectionLeft -> onDirection?.invoke(-1) ?: false
            event.type==KeyEventType.KeyDown && event.key==Key.DirectionRight -> onDirection?.invoke(1) ?: false
            else -> false
        }}
        .combinedClickable(interactionSource=source, indication=null, role=Role.Button, onClick=onClick, onLongClick=onLongClick)
        .padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(StovaShapes.Medium)).background(if(active) app.stova.core.theme.StovaColors.PrimaryContainer else MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
                Icon(icon,null,Modifier.size(22.dp),tint=if(active) app.stova.core.theme.StovaColors.Caution else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            if (locked) Icon(StovaIcons.Lock,stringResource(R.string.ha_locked),Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            if (pending) Text(stringResource(R.string.ha_v2_pending),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else if (badge != null) Text(badge,Modifier.clip(RoundedCornerShape(StovaShapes.Small)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}
