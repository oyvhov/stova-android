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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.core.theme.LocalAppearance
import app.stova.profiles.AppStyle
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
    val prefs = LocalAppearance.current
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val iconStyle = prefs.appStyle == AppStyle.ICON
    val wide = prefs.cardSize.width.dp
    val height = wide * 9 / 16
    val width = if (iconStyle) height else wide
    val banner by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime, iconStyle) {
        value = if (iconStyle) art.icon(app) else art.banner(app)
    }
    Column(modifier.width(width).semantics { contentDescription = app.label }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) { onLongClick(); true } else false
        }.combinedClickable(source, indication = null, role = Role.Button, onLongClick = onLongClick, onClick = onClick)) {
        Box(Modifier.size(width, height).focusOutline(source, shape, zoom = true).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            val image = banner
            if (image != null) {
                Image(image, contentDescription = null, contentScale = if (iconStyle) ContentScale.Fit else ContentScale.Crop,
                    modifier = if (iconStyle) Modifier.size(height * 0.65f) else Modifier.fillMaxSize())
            } else {
                IconAndName(app, art, iconStyle)
            }
        }
        if (prefs.showTitles) Text(app.label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IconAndName(app: InstalledApp, art: AppArtCache, iconStyle: Boolean) {
    val icon by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.icon(app) }
    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(56.dp)) }
        if (icon == null || !iconStyle) Text(app.label, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
