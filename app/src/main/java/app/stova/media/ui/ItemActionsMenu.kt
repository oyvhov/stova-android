package app.stova.media.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.SheetHeader
import app.stova.core.ui.SheetPages
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.StovaSheet
import app.stova.media.model.ItemType
import app.stova.media.model.MediaItem
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/** What Spole can do with a title from its long-press menu. */
enum class MediaItemAction { PLAY_FROM_START, MARK_WATCHED, REMOVE_FROM_CONTINUE }

/**
 * Long OK on a film or an episode: its picture, Play and Details, what Spole can do with it ([onAction],
 * when given), then the row it sits in. «Spel frå starten» and «Fjern frå Hald fram» are offered for a
 * title part-way through; «Merk som sett» for any film or episode.
 */
@Composable fun ItemActionsMenu(item: MediaItem, onPlay: () -> Unit, onDetails: () -> Unit, onDismiss: () -> Unit,
    rowActions: @Composable (SheetPages) -> Unit = {}, onAction: ((MediaItemAction) -> Unit)? = null) {
    StovaSheet(onDismiss) { pages ->
        if (pages.current == null) {
            val episode = item.type == ItemType.EPISODE
            val art = item.art.backdrop ?: item.art.wide
            SheetHeader(if (episode) item.seriesName ?: item.title else item.title, itemLine(item), StovaIcons.Play,
                art = art?.let { url -> { SheetArt(url) } })
            val resuming = item.progress?.let { it.isFinite() && it > 0f && it < 1f } == true
            val remaining = if (resuming) item.runtimeMinutes?.takeIf { it > 0 }?.let {
                kotlin.math.ceil(it * (1.0 - item.progress!!.toDouble())).toInt().coerceAtLeast(1)
            } else null
            val playLabel = when {
                remaining != null -> stringResource(R.string.item_resume_remaining, remaining)
                resuming -> stringResource(R.string.item_resume)
                else -> stringResource(R.string.item_play)
            }
            StovaButton(playLabel, onPlay, Modifier.fillMaxWidth(), primary = true, icon = StovaIcons.Play)
            StovaButton(stringResource(R.string.item_details), onDetails, Modifier.fillMaxWidth(), icon = StovaIcons.Info)
            if (onAction != null) {
                if (resuming) app.stova.core.ui.SheetAction(stringResource(R.string.item_play_from_start), StovaIcons.Play,
                    { onAction(MediaItemAction.PLAY_FROM_START) })
                if (item.type == ItemType.MOVIE || item.type == ItemType.EPISODE)
                    app.stova.core.ui.SheetAction(stringResource(R.string.item_mark_watched), StovaIcons.Check,
                        { onAction(MediaItemAction.MARK_WATCHED) })
                if (resuming) app.stova.core.ui.SheetAction(stringResource(R.string.item_remove_from_continue), StovaIcons.Hide,
                    { onAction(MediaItemAction.REMOVE_FROM_CONTINUE) })
            }
        }
        rowActions(pages)
    }
}

/** Season and episode with the episode's name, or a film's year and length. */
@Composable private fun itemLine(item: MediaItem): String? {
    if (item.type == ItemType.EPISODE) {
        val number = listOfNotNull(item.season?.let { "S$it" }, item.episode?.let { "E$it" }).joinToString(" ")
        return listOf(number, item.title.takeIf { item.seriesName != null }.orEmpty()).filter { it.isNotBlank() }
            .joinToString(" · ").ifBlank { null }
    }
    return app.stova.home.metaParts(item).map { part -> when (part) {
        is app.stova.home.MetaPart.Year -> part.y.toString()
        is app.stova.home.MetaPart.Runtime -> if (part.minutes >= 60) stringResource(R.string.runtime_hm, part.minutes / 60, part.minutes % 60)
            else stringResource(R.string.runtime_m, part.minutes)
    } }.joinToString(" · ").ifBlank { null }
}

@Composable private fun BoxScope.SheetArt(url: String) {
    val context = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(context).data(url).size(SHEET_ART_WIDTH, SHEET_ART_HEIGHT).build() }
    AsyncImage(request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().align(Alignment.Center))
}

/** The panel art at 2 px per dp on the Shield; Coil scales it down for the 324 dp wide box. */
private const val SHEET_ART_WIDTH = 720
private const val SHEET_ART_HEIGHT = 405
