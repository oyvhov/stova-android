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
import androidx.compose.ui.graphics.ColorFilter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.profiles.IconColor
import app.stova.profiles.IconFill
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
    iconOverride: IconOverride? = null,
) {
    val prefs = LocalAppearance.current
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val iconStyle = prefs.appStyle == AppStyle.ICON
    val wide = prefs.cardSize.width.dp
    val height = wide * 9 / 16
    val width = if (iconStyle) height else wide
    val packs by art.iconPacks.packs.collectAsStateWithLifecycle()
    val artwork by produceState<AppArtwork?>(null, app, art, prefs.iconPack, iconStyle, iconOverride, packs) {
        value = art.artwork(app, prefs, iconOverride)
    }
    Column(modifier.width(width).semantics { contentDescription = app.label }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) { onLongClick(); true } else false
        }.combinedClickable(source, indication = null, role = Role.Button, onLongClick = onLongClick, onClick = onClick)) {
        Box(Modifier.size(width, height).focusOutline(source, shape, zoom = true).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            val image = artwork
            if (image != null) {
                val centered = if (image.packIcon) prefs.iconFill == IconFill.CENTER else iconStyle
                Image(image.bitmap, contentDescription = null, contentScale = if (centered) ContentScale.Fit else ContentScale.Crop,
                    colorFilter = if (image.monochrome && prefs.iconColor == IconColor.ACCENT) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                    modifier = if (centered) Modifier.size(height * if (image.packIcon) 0.82f else 0.65f) else Modifier.fillMaxSize())
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
