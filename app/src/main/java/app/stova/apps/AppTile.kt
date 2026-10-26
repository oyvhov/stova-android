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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import app.stova.core.theme.LocalAppearance
import app.stova.profiles.AppStyle
import app.stova.profiles.appBackgroundPercent
import app.stova.profiles.RowFormat
import app.stova.home.LocalRowStyle
import app.stova.home.LocalArtworkScale
import app.stova.home.artworkDimensions
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
    val style = LocalRowStyle.current
    val format = style.format ?: if (prefs.appStyle == AppStyle.ICON) RowFormat.ICON else RowFormat.WIDE
    val iconStyle = format == RowFormat.ICON
    val dimensions = artworkDimensions(format, style.size ?: prefs.cardSize)
    val fit = LocalArtworkScale.current
    val height = (dimensions.height * fit).dp
    val width = (dimensions.width * fit).dp
    val packs by art.iconPacks.packs.collectAsStateWithLifecycle()
    val artwork by produceState<AppArtwork?>(null, app, art, prefs.iconPack, iconStyle, iconOverride, packs) {
        value = null
        value = art.artwork(app, prefs, iconOverride)
    }
    Column(modifier.width(width).semantics { contentDescription = app.label }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) { onLongClick(); true } else false
        }.combinedClickable(source, indication = null, role = Role.Button, onLongClick = onLongClick, onClick = onClick)) {
        Box(Modifier.size(width, height).focusOutline(source, shape, zoom = true, dimSurfaceAlpha = prefs.appBackgroundPercent / 100f).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = prefs.appBackgroundPercent / 100f)), contentAlignment = Alignment.Center) {
            val image = artwork
            if (image != null) {
                val centered = if (image.packIcon) prefs.iconFill == IconFill.CENTER else iconStyle
                Image(image.bitmap, contentDescription = null, contentScale = if (centered) ContentScale.Fit else ContentScale.Crop,
                    colorFilter = if (image.monochrome && prefs.iconColor == IconColor.ACCENT) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                    modifier = if (centered && image.packIcon && image.bitmap.width > image.bitmap.height)
                        Modifier.fillMaxSize().padding(12.dp)
                    else if (centered) Modifier.size(height * if (image.packIcon) 0.82f else 0.65f) else Modifier.fillMaxSize())
            } else {
                IconAndName(app, art, iconStyle, height, prefs.showTitles)
            }
        }
        if (prefs.showTitles) Text(app.label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IconAndName(app: InstalledApp, art: AppArtCache, iconStyle: Boolean, height: Dp, caption: Boolean) {
    val icon by produceState<ImageBitmap?>(null, app.key, app.lastUpdateTime) { value = art.icon(app) }
    val showName = icon == null || (!iconStyle && !caption)
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.titleSmall.lineHeight.toDp() }
    val verticalPadding = if (showName) ((height - lineHeight) / 2).coerceIn(0.dp, 8.dp) else 8.dp
    val iconSize = (height - verticalPadding * 2 - if (showName) lineHeight + 6.dp else 0.dp).coerceIn(0.dp, 56.dp)
    Column(Modifier.padding(horizontal = 8.dp, vertical = verticalPadding), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        icon?.let { if (iconSize > 0.dp) Image(it, contentDescription = null, modifier = Modifier.size(iconSize)) }
        if (showName) Text(app.label, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
