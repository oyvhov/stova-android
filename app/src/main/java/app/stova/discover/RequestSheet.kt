package app.stova.discover

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.SheetAction
import app.stova.core.ui.SheetHeader
import app.stova.core.ui.SheetSection
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.StovaSheet
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

/** The seasons as the sheet lists them: in order, with the specials (season 0) last. */
private val SEASON_ORDER = compareBy<DiscoverSeason>({ it.number == 0 }, { it.number })

/** The seasons chosen when the sheet opens: every one Seerr can still get, but never the specials. */
fun defaultSeasonSelection(details: DiscoverDetails): Set<Int> = details.requestableSeasons.map { it.number }.toSet()

/**
 * Asking Seerr for a title, under its poster and year. A film is a short confirmation. A series lists its
 * seasons with [defaultSeasonSelection] chosen; a season Seerr already has shows where it stands and cannot
 * be chosen, and nothing is sent with no season chosen. [onSend] gets the chosen seasons in order, none for
 * a film. Back closes the sheet and sends nothing.
 */
@Composable
fun RequestSheet(details: DiscoverDetails, onSend: (seasons: List<Int>) -> Unit, onDismiss: () -> Unit) {
    val title = details.title
    var chosen by remember(title.key) { mutableStateOf(defaultSeasonSelection(details)) }
    StovaSheet(onDismiss) {
        SheetHeader(title.title, title.year?.toString(), StovaIcons.Discover, art = title.poster?.let { url -> { PosterArt(url) } })
        if (title.type == DiscoverType.SERIES) {
            StovaButton(pluralStringResource(R.plurals.discover_send_seasons, chosen.size, chosen.size), { onSend(chosen.sorted()) },
                Modifier.fillMaxWidth(), primary = true, enabled = chosen.isNotEmpty())
            SheetSection(stringResource(R.string.discover_seasons))
            for (season in details.seasons.sortedWith(SEASON_ORDER)) {
                SheetAction(season.name.ifBlank { season.number.toString() }, StovaIcons.Tv,
                    onClick = { if (season.requestable) chosen = if (season.number in chosen) chosen - season.number else chosen + season.number },
                    value = seerrStatusLabel(season.status)?.takeUnless { season.requestable }?.let { stringResource(it) },
                    chosen = season.number in chosen)
            }
        } else {
            Text(stringResource(R.string.discover_request_movie), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            StovaButton(stringResource(R.string.discover_send), { onSend(emptyList()) }, Modifier.fillMaxWidth(), primary = true)
            StovaButton(stringResource(R.string.discover_cancel), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/** Taking the person's own request for [title] back. Back and «Behald» keep it. */
@Composable
fun WithdrawSheet(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    StovaSheet(onDismiss) {
        SheetHeader(stringResource(R.string.discover_withdraw_title, title), icon = StovaIcons.Discover)
        Text(stringResource(R.string.discover_withdraw_note), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SheetAction(stringResource(R.string.discover_withdraw), StovaIcons.Hide, onConfirm, danger = true)
        SheetAction(stringResource(R.string.discover_keep), StovaIcons.Check, onDismiss)
    }
}

/** The poster whole in the header's wide box, as a poster is taller than it is wide. */
@Composable
private fun BoxScope.PosterArt(url: String) {
    val context = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(context).data(url).size(POSTER_ART_WIDTH, POSTER_ART_HEIGHT).build() }
    AsyncImage(request, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize())
}

/** The poster at the header box's height, 2 px per dp on the Shield. */
private const val POSTER_ART_WIDTH = 243
private const val POSTER_ART_HEIGHT = 365
