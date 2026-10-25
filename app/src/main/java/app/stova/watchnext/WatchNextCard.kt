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
import app.stova.media.model.*
import app.stova.media.ui.MediaCard

@Composable
fun WatchNextCard(entry: WatchNextEntry, onClick: () -> Unit, onFocused: () -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.width(app.stova.core.theme.LocalAppearance.current.cardSize.width.dp)) {
        MediaCard(MediaItem(entry.id.toString(), entry.packageName, ItemType.OTHER, entry.title,
            progress = entry.progress, art = ItemArt(wide = entry.posterUri)),
            poster = false, onClick = onClick, onLongClick = {}, onFocused = onFocused, modifier = modifier)
        if(app.stova.core.theme.LocalAppearance.current.showTitles) entry.subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
