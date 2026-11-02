package app.stova.discover

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.media.model.ItemArt
import app.stova.media.model.ItemType
import app.stova.media.model.MediaItem
import app.stova.media.ui.MediaCard
import kotlinx.coroutines.launch

/**
 * A request the person follows: a poster card the size and shape of Home's, and a line below it saying how
 * far the request has come («Lastar ned · 42 %»). Only a download draws the progress bar. OK, or a long
 * press, opens the title.
 */
@Composable
fun DiscoverRequestCard(request: DiscoverRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (label, percent) = requestCaption(request.stage, request.percent)
    val card = remember(request) { request.asMediaItem(percent) }
    val caption = if (percent == null) stringResource(label)
        else stringResource(R.string.discover_request_caption_percent, stringResource(label), percent)
    // Focus brings only the poster into view; the card asks for the line under it too, so a row at the screen's edge is not cut.
    val whole = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    // The line is as wide as the card, whatever size the profile gives cards, and never widens it.
    Layout({
        MediaCard(card, poster = true, onClick, onClick, {})
        Text(caption, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, modifier.bringIntoViewRequester(whole).onFocusChanged { if (it.hasFocus) scope.launch { whole.bringIntoView() } }) { (art, line), constraints ->
        val drawn = art.measure(constraints)
        val text = line.measure(Constraints(maxWidth = drawn.width))
        layout(drawn.width, drawn.height + text.height) {
            drawn.place(0, 0)
            text.place(0, drawn.height)
        }
    }
}

/** What a request's card is known by on the page: a title can be asked for twice (one in 4K), each request with its own id. */
internal val DiscoverRequest.key: String get() = "${type.wire}-$tmdbId-${requestId ?: 0}" + if (is4k) "-4k" else ""

/** The request as the title its card opens: what Spole gave of it, while Seerr is asked for the rest. */
fun DiscoverRequest.asDiscoverTitle(): DiscoverTitle =
    DiscoverTitle(type, tmdbId, title, null, null, poster, backdrop, SeerrStatus.UNKNOWN, null, emptyList())

/** The request as a card; [percent] is the download's, drawn as the card's progress. */
private fun DiscoverRequest.asMediaItem(percent: Int?): MediaItem = MediaItem(
    id = key, accountId = SEERR_CARDS, type = if (type == DiscoverType.MOVIE) ItemType.MOVIE else ItemType.SERIES, title = title,
    progress = percent?.let { it / 100f }, art = ItemArt(backdrop = backdrop, logo = null, poster = poster, wide = backdrop))
