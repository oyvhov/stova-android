package app.stova.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import app.stova.R
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaIcon
import app.stova.ha.HaIconFont
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
            ShortcutIcon(tile, spec.height, showName = !spec.captions)
        }
        // A scene does something in the house when pressed: Home Assistant's own mark says so. The
        // brand rules: unchanged, in no container, and clear space of a quarter of its height.
        if (tile is FavoriteTile.Scene) Image(painterResource(R.drawable.ic_home_assistant), contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(HaMarkSize * .4f).size(HaMarkSize).testTag("ha-badge"))
    }
}

private val HaMarkSize = 22.dp

@Composable
private fun ShortcutIcon(tile: FavoriteTile, height: Dp, showName: Boolean) {
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.titleSmall.lineHeight.toDp() }
    val size = (height - 16.dp - if (showName) lineHeight + 6.dp else 0.dp).coerceIn(0.dp, 48.dp)
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val tint = MaterialTheme.colorScheme.onSurfaceVariant
        if (size > 0.dp) {
            if (tile is FavoriteTile.Scene) HaIcon(tile.icon ?: HaIconFont.defaultFor(tile.entityId), StovaIcons.Sun, size, tint)
            else Icon(StovaIcons.Rows, contentDescription = null, modifier = Modifier.size(size), tint = tint)
        }
        if (showName) Text(tile.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
