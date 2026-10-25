package app.stova.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaLayout

/** One symbol and one text edge, shared by pages and their navigation destinations. */
@Composable
fun CategoryIcon(icon: ImageVector, modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, size: Dp = StovaLayout.PageIconSize) {
    Box(modifier.size(size).clip(MaterialTheme.shapes.medium)
        .background(lerp(MaterialTheme.colorScheme.surface, tint, 0.08f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.58f), tint = tint)
    }
}

@Composable
fun PageHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier, subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, iconModifier: Modifier = Modifier,
    metadata: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        CategoryIcon(icon, iconModifier, tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, Modifier.weight(1f, fill = false).semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                metadata()
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
