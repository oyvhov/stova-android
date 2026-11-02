package app.stova.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.background.BackdropLayer
import app.stova.background.BackdropState
import app.stova.background.rememberBackdropState
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.media.ui.communityText
import app.stova.profiles.BackgroundSource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

/** Seerr's related rows in the order the page shows them, with their headings. */
private val RELATED_ROWS = listOf(
    DiscoverRowKind.SIMILAR to R.string.discover_row_similar,
    DiscoverRowKind.RECOMMENDED to R.string.discover_row_recommended,
)

/** The poster a size up from a row's card, as the title's page is about this one title. */
private const val POSTER_SCALE = 1.4f

/** Wide enough for five lines of story beside the poster, narrow enough to leave the backdrop's subject free. */
private val INFO_WIDTH = 600.dp

/**
 * A title's page on Oppdag: its backdrop, poster, facts and where it stands in Seerr, what the person may
 * do with it, and Seerr's related titles below. It opens with what [title]'s card showed while [details] is
 * null and fills in when Spole answers. Asking needs [SeerrAccess.PERSONAL] and a title Seerr can still get;
 * a request of the person's own can be taken back until it is in the library; «Opne i Spole» is always
 * there. Focus starts on the first button, once per title. [backdrop] is the whole Oppdag page's.
 */
@Composable
fun DiscoverTitleContent(title: DiscoverTitle, details: DiscoverDetails?, related: List<DiscoverRow>, access: SeerrAccess, busy: Boolean,
    onRequest: () -> Unit, onWithdraw: () -> Unit, onOpenSpole: () -> Unit, onOpenTitle: (DiscoverTitle) -> Unit,
    modifier: Modifier = Modifier, backdrop: BackdropState = rememberBackdropState()) {
    val shown = details?.title ?: title
    // The profile's background choice holds here too: the theme's own surface shows no title's picture.
    val themeSurface = LocalAppearance.current.backgroundSource == BackgroundSource.THEME
    val rows = remember(related) {
        RELATED_ROWS.mapNotNull { (kind, heading) -> related.firstOrNull { it.kind == kind && it.items.isNotEmpty() }?.let { it to heading } }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(title.key) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    // Back up from the related rows the whole head comes into sight, not just the button that takes focus.
    val head = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
        BackdropLayer(shown.backdrop.takeUnless { themeSurface }, state = backdrop)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = StovaLayout.TvSafeEdge)) {
            Row(Modifier.padding(horizontal = StovaLayout.TvSafeEdge).bringIntoViewRequester(head)
                .onFocusChanged { if (it.hasFocus) scope.launch { head.bringIntoView() } },
                horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                TitlePoster(shown.poster)
                Column(Modifier.widthIn(max = INFO_WIDTH), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(shown.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FactsLine(shown, details)
                    StatusLine(shown, details)
                    details?.tagline?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    shown.overview?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                    TitleActions(details, access, busy, first, onRequest, onWithdraw, onOpenSpole, Modifier.padding(top = 8.dp))
                }
            }
            for ((row, heading) in rows) DiscoverTitleRow(stringResource(heading), row.items, onFocus = {}, onOpen = onOpenTitle)
        }
    }
}

/** The poster, or the bare card surface while there is none. */
@Composable
private fun TitlePoster(poster: String?) {
    val width = StovaLayout.PosterWidth * POSTER_SCALE
    val height = StovaLayout.PosterHeight * POSTER_SCALE
    val context = LocalContext.current
    val density = LocalDensity.current
    // Decoded at the size it is drawn, as a card's art is.
    val request = remember(poster, density) {
        poster?.let { ImageRequest.Builder(context).data(it).size(with(density) { width.roundToPx() }, with(density) { height.roundToPx() }).build() }
    }
    Box(Modifier.size(width, height).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (request != null) AsyncImage(request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** Year · running time · genres · ★ · critics, each only when Seerr knows it. */
@Composable
private fun FactsLine(title: DiscoverTitle, details: DiscoverDetails?) {
    val locale = LocalLocale.current.platformLocale
    val facts = listOfNotNull(
        title.year?.toString(),
        details?.runtimeMinutes?.takeIf { it > 0 }?.let { stringResource(R.string.discover_runtime, it) },
    ) + title.genres.take(3) + listOfNotNull(
        title.community?.takeIf { it in 0.0..10.0 }?.let { stringResource(R.string.appearance_star_score, communityText(it, locale)) },
        details?.critic?.takeIf { it in 0..100 }?.let { stringResource(R.string.discover_critic, it) },
    )
    if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** How far the person's own request has come, else what Seerr says of the title; nothing for a title Seerr knows nothing of. */
@Composable
private fun StatusLine(title: DiscoverTitle, details: DiscoverDetails?) {
    val label = details?.requestStage?.let(::requestStageLabel) ?: seerrStatusLabel(title.status) ?: return
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Ask (the film, or a series' seasons), take a request back, and open the title in Spole; [first] goes on the first one there. */
@Composable
private fun TitleActions(details: DiscoverDetails?, access: SeerrAccess, busy: Boolean, first: FocusRequester, onRequest: () -> Unit,
    onWithdraw: () -> Unit, onOpenSpole: () -> Unit, modifier: Modifier = Modifier) {
    val request = details?.requestAction?.takeIf { access == SeerrAccess.PERSONAL }
    val withdraw = details?.withdrawable == true
    val firstFocus = Modifier.focusRequester(first)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (request != null) StovaButton(stringResource(if (request == RequestAction.MOVIE) R.string.discover_request else R.string.discover_choose_seasons),
            onRequest, firstFocus, primary = true, enabled = !busy)
        if (withdraw) StovaButton(stringResource(R.string.discover_withdraw), onWithdraw, if (request == null) firstFocus else Modifier,
            quiet = true, enabled = !busy)
        StovaButton(stringResource(R.string.discover_open_spole), onOpenSpole, if (request == null && !withdraw) firstFocus else Modifier,
            quiet = true, enabled = !busy)
    }
}
