package app.stova.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.focusOutline

/**
 * A 16:9 app tile: the app's own banner, or its icon and name on a raised surface. OK opens it;
 * holding OK or pressing Menu asks for [onLongClick].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: InstalledApp,
    art: AppArtCache,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(StovaShapes.Medium)
    val banner by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.banner(app) }
    Box(
        modifier
            .size(StovaLayout.WideWidth, StovaLayout.WideHeight)
            .focusOutline(source, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) { onLongClick(); true } else false
            }
            .combinedClickable(source, indication = null, role = Role.Button, onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val image = banner
        if (image != null) {
            Image(image, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            IconAndName(app, art)
        }
    }
}

@Composable
private fun IconAndName(app: InstalledApp, art: AppArtCache) {
    val icon by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.icon(app) }
    Column(
        Modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
        Text(
            app.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
