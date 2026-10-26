package app.stova.watchnext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import app.stova.core.theme.StovaLayout
import app.stova.home.showsCardTitles
import app.stova.media.model.*
import app.stova.media.ui.MediaCard

@Composable
fun WatchNextCard(entry: WatchNextEntry, onClick: () -> Unit, onFocused: () -> Unit, modifier: Modifier = Modifier, onLongClick: () -> Unit = {}) {
    val prefs = app.stova.core.theme.LocalAppearance.current
    val style = app.stova.home.LocalRowStyle.current
    val dimensions = app.stova.home.artworkDimensions(style.format ?: app.stova.profiles.RowFormat.WIDE, style.size ?: prefs.cardSize)
    val width = (dimensions.width * app.stova.home.LocalArtworkScale.current).dp
    Column(Modifier.width(width)) {
        MediaCard(MediaItem(entry.id.toString(), entry.packageName, ItemType.OTHER, entry.title,
            progress = entry.progress, art = ItemArt(wide = entry.posterUri)),
            poster = false, onClick = onClick, onLongClick = onLongClick, onFocused = onFocused, modifier = modifier)
        if(style.showsCardTitles(prefs)) entry.subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
