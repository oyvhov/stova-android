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

/** Long OK on a film or an episode: its picture, Play and Details, then the row it sits in. */
@Composable fun ItemActionsMenu(item: MediaItem, onPlay: () -> Unit, onDetails: () -> Unit, onDismiss: () -> Unit,
    rowActions: @Composable (SheetPages) -> Unit = {}) {
    StovaSheet(onDismiss) { pages ->
        if (pages.current == null) {
            val episode = item.type == ItemType.EPISODE
            val art = item.art.backdrop ?: item.art.wide
            SheetHeader(if (episode) item.seriesName ?: item.title else item.title, itemLine(item), StovaIcons.Play,
                art = art?.let { url -> { SheetArt(url) } })
            StovaButton(stringResource(R.string.item_play), onPlay, Modifier.fillMaxWidth(), primary = true, icon = StovaIcons.Play)
            StovaButton(stringResource(R.string.item_details), onDetails, Modifier.fillMaxWidth(), icon = StovaIcons.Info)
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
