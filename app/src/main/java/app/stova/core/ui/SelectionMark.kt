package app.stova.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import app.stova.core.theme.stovaMotion

/** Selection stays visible when focus moves away. Its reserved slot prevents label jumps. */
@Composable
internal fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier, icon: ImageVector = StovaIcons.Check) {
    val alpha=animateFloatAsState(if(selected) 1f else 0f,stovaMotion.fade(stovaMotion.quick),label="selection-mark")
    Icon(icon,null,modifier.size(18.dp).graphicsLayer {this.alpha=alpha.value},tint=MaterialTheme.colorScheme.onSurface)
}
