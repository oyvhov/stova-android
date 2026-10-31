package app.stova.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
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
 * A favourite library, scene or Spole profile, in the same frame as the apps. A library shows the
 * image it has in Jellyfin or Emby, as Spole does; without one, or for a scene, an icon and the name.
 * A Spole profile shows Spole's banner with the person's picture on it.
 */
@Composable
fun ShortcutTile(tile: FavoriteTile, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    TileFrame(tile.label, tile.key.hashCode(), onClick, onLongClick, modifier) { spec ->
        if (tile is FavoriteTile.SpoleProfile) {
            SpoleProfileArt(tile, spec.width, spec.height)
            return@TileFrame
        }
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

/** Spole's banner, so the tile says what opens, and the person's picture on it, so it says as whom; their initial without one. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SpoleProfileArt(tile: FavoriteTile.SpoleProfile, width: Dp, height: Dp) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val banner by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, width, height) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            spoleBanner(context, with(density) { width.roundToPx() }, with(density) { height.roundToPx() })
        }
    }
    // Spole's name keeps the left two thirds, on the banner's own ground; the picture takes the right.
    banner?.let {
        val ground = remember(it) { androidx.compose.ui.graphics.Color(it.asAndroidBitmap().getPixel(1, 1)) }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(ground))
        Image(it, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(0.66f).aspectRatio(width / height))
    }
    val size = (height * 0.5f).coerceAtMost(96.dp)
    val shape = androidx.compose.foundation.shape.CircleShape
    androidx.compose.foundation.layout.Box(
        Modifier.align(if (banner != null) Alignment.CenterEnd else Alignment.Center)
            .padding(end = if (banner != null) height * 0.1f else 0.dp).size(size)
            .clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .9f), shape).testTag("spole-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        Text(tile.label.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        var failed by remember(tile.image) { mutableStateOf(false) }
        if (tile.image != null && !failed) {
            val request = remember(tile.image, size) {
                ImageRequest.Builder(context).data(tile.image).size(with(density) { size.roundToPx() }).build()
            }
            AsyncImage(request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onError = { failed = true })
        }
    }
}

/** The banner Spole shows on the TV's own home, drawn at the tile's size; null without Spole or a banner. */
private fun spoleBanner(context: android.content.Context, width: Int, height: Int): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val pm = context.packageManager
    val pkg = app.stova.media.SpoleHandoff(context).installedPackages().firstOrNull() ?: return null
    val drawable = pm.getLeanbackLaunchIntentForPackage(pkg)?.let { pm.getActivityBanner(it) } ?: pm.getApplicationBanner(pkg) ?: return null
    drawable.toBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1)).asImageBitmap()
}.getOrNull()

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
