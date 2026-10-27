package app.stova.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import app.stova.core.theme.LocalArtTone
import app.stova.core.theme.cardTitleAlpha
import app.stova.core.theme.stovaMotion
import app.stova.home.LocalRowStyle
import app.stova.home.LocalRowHasFocus
import app.stova.home.showsCardTitles
import app.stova.home.LocalArtworkScale
import app.stova.home.artworkDimensions
import app.stova.home.seasonalTile
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.LocalDrawnFocus
import app.stova.core.ui.focusOutline

/** What a tile's content draws into: the artwork box and whether names sit under the tiles. */
internal class TileSpec(val width: Dp, val height: Dp, val iconStyle: Boolean, val captions: Boolean)

/**
 * The frame every favourite tile shares: size from the row's style, focus ring, surface, seasonal
 * decor placed by [seed], and the name under it. OK opens; holding OK or pressing Menu asks for
 * [onLongClick].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TileFrame(
    label: String,
    seed: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(TileSpec) -> Unit,
) {
    val prefs = LocalAppearance.current
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val style = LocalRowStyle.current
    val captions = style.showsCardTitles(prefs)
    val format = style.format ?: if (prefs.appStyle == AppStyle.ICON) RowFormat.ICON else RowFormat.WIDE
    val dimensions = artworkDimensions(format, style.size ?: prefs.cardSize)
    val fit = LocalArtworkScale.current
    val spec = TileSpec((dimensions.width * fit).dp, (dimensions.height * fit).dp, format == RowFormat.ICON, captions)
    val surface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = prefs.appBackgroundPercent / 100f)
    val tone = LocalArtTone.current
    Column(modifier.width(spec.width).semantics { contentDescription = label }
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) { onLongClick(); true } else false
        }.combinedClickable(source, indication = null, role = Role.Button, onLongClick = onLongClick, onClick = onClick)) {
        Box(Modifier.size(spec.width, spec.height).focusOutline(source, shape, zoom = true, dimSurfaceAlpha = prefs.appBackgroundPercent / 100f).clip(shape)
            .drawBehind { drawRect(tone?.surface(surface) ?: surface) }.seasonalTile(seed), contentAlignment = Alignment.Center) {
            content(spec)
        }
        if (captions) {
            val focused by source.collectIsFocusedAsState()
            val motion = stovaMotion
            val strength by animateFloatAsState(cardTitleAlpha(focused || LocalDrawnFocus.current, LocalRowHasFocus.current),
                motion.fade(motion.focusRing), label = "card-title")
            Text(label, Modifier.padding(top = 8.dp).graphicsLayer { alpha = strength }, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A 16:9 app tile: the app's own banner, or its icon and name on a raised surface. */
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
    val packs by art.iconPacks.packs.collectAsStateWithLifecycle()
    TileFrame(app.label, app.key.hashCode(), onClick, onLongClick, modifier) { spec ->
        val artwork by produceState<AppArtwork?>(null, app, art, prefs.iconPack, spec.iconStyle, iconOverride, packs) {
            value = null
            value = art.artwork(app, prefs, iconOverride)
        }
        val image = artwork
        if (image != null) {
            val centered = if (image.packIcon) prefs.iconFill == IconFill.CENTER else spec.iconStyle
            Image(image.bitmap, contentDescription = null, contentScale = if (centered) ContentScale.Fit else ContentScale.Crop,
                colorFilter = if (image.monochrome && prefs.iconColor == IconColor.ACCENT) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                modifier = if (centered && image.packIcon && image.bitmap.width > image.bitmap.height)
                    Modifier.fillMaxSize().padding(12.dp)
                else if (centered) Modifier.size(spec.height * if (image.packIcon) 0.82f else 0.65f) else Modifier.fillMaxSize())
        } else {
            IconAndName(app, art, spec.iconStyle, spec.height, spec.captions)
        }
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
