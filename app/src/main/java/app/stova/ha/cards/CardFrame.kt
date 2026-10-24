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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.*

@Composable
fun CardFrame(config: CardConfig, icon: ImageVector, label: String, badge: String?, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(StovaShapes.Large)
    Column(Modifier.width(if(config.wide) 496.dp else 240.dp).heightIn(min=160.dp)
        .focusOutline(source,shape).clip(shape).background(MaterialTheme.colorScheme.surface)
        .combinedClickable(interactionSource=source, indication=null, role=Role.Button, onClick={if(!locked) onClick()}, onLongClick={if(!locked) onLongClick()})
        .padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(StovaShapes.Medium)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
                Icon(icon,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            if (locked) Icon(StovaIcons.Lock,stringResource(R.string.ha_locked),Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            else if (badge != null) Text(badge,Modifier.clip(RoundedCornerShape(StovaShapes.Medium)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
