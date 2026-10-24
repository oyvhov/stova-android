package app.stova.watchnext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.stova.media.model.*
import app.stova.media.ui.MediaCard
@Composable fun WatchNextCard(entry: WatchNextEntry, onClick: () -> Unit, onFocused: () -> Unit, modifier: Modifier = Modifier) {
    MediaCard(MediaItem(entry.id.toString(), entry.packageName, ItemType.OTHER,
        listOfNotNull(entry.title, entry.subtitle).joinToString(" · "), progress = entry.progress, art = ItemArt(wide = entry.posterUri)),
        poster = false, onClick = onClick, onLongClick = {}, onFocused = onFocused, modifier = modifier)
}
