package app.stova.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.core.ui.StovaIcons
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/**
 * A favourite library or scene, in the same frame as the apps. A library shows the image it has in
 * Jellyfin or Emby, as Spole does; without one, or for a scene, an icon and the name.
 */
@Composable
fun ShortcutTile(tile: FavoriteTile, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    TileFrame(tile.label, tile.key.hashCode(), onClick, onLongClick, modifier) { spec ->
        val image = (tile as? FavoriteTile.Library)?.image
        var failed by remember(image) { mutableStateOf(false) }
        if (image != null && !failed) {
            val context = LocalContext.current
            val density = LocalDensity.current
            val request = remember(image, spec.width, spec.height) {
                ImageRequest.Builder(context).data(image)
                    .size(with(density) { spec.width.roundToPx() }, with(density) { spec.height.roundToPx() }).build()
            }
            AsyncImage(request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onError = { failed = true })
        } else {
            ShortcutIcon(if (tile is FavoriteTile.Scene) StovaIcons.Sun else StovaIcons.Rows, tile.label, spec.height, showName = !spec.captions)
        }
    }
}

@Composable
private fun ShortcutIcon(icon: ImageVector, label: String, height: Dp, showName: Boolean) {
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.titleSmall.lineHeight.toDp() }
    val size = (height - 16.dp - if (showName) lineHeight + 6.dp else 0.dp).coerceIn(0.dp, 48.dp)
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (size > 0.dp) Icon(icon, contentDescription = null, modifier = Modifier.size(size), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showName) Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
