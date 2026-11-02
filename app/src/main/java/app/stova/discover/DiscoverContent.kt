package app.stova.discover

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.background.BackdropLayer
import app.stova.background.BackdropState
import app.stova.background.rememberBackdropState
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.PageHeader
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.softEntrance
import app.stova.home.RowHeader
import app.stova.media.ui.MediaCard
import app.stova.media.ui.communityText
import app.stova.profiles.BackgroundSource
import app.stova.profiles.MovieStyle
import app.stova.profiles.spacingDp

/** The page's rows of titles in their order, with their headings; the person's own requests come right after «Populært no». */
private val PAGE_ROWS = listOf(
    DiscoverRowKind.TRENDING to R.string.discover_row_trending,
    DiscoverRowKind.UPCOMING_MOVIES to R.string.discover_row_upcoming_movies,
    DiscoverRowKind.UPCOMING_SERIES to R.string.discover_row_upcoming_series,
)

/** Wide enough for a title and three lines of its story beside the backdrop's subject, as Home's large hero. */
private val HERO_WIDTH = 560.dp

/** The cards on this page stand for titles in Seerr; the account id only keeps them apart from library cards. */
internal const val SEERR_CARDS = "seerr"

/** The key of the row «Dine førespurnader», and the start of its cards' places; no [DiscoverRowKind] is named so. */
private const val REQUESTS_ROW = "REQUESTS"

/**
 * The Oppdag page's rows: a header with «Søk», a hero band for the card in focus over that title's backdrop,
 * and Seerr's rows below, with the person's own requests after «Populært no» when there are any. Until Spole
 * first answers it says it is fetching; when Seerr did not answer and there is nothing to show it offers to
 * try again; a person who may only look is told how to ask. The page opens on the first card, or on the card
 * it was left on while a title's page lay over it. [backdrop] is the whole Oppdag page's, so going to a title
 * and back never fades the picture in from the bare background.
 */
@Composable
fun DiscoverContent(state: DiscoverState?, onOpenTitle: (DiscoverTitle) -> Unit, onOpenSearch: () -> Unit,
    onOpenRequest: (DiscoverRequest) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier,
    backdrop: BackdropState = rememberBackdropState()) {
    val shown = remember(state?.rows) {
        PAGE_ROWS.mapNotNull { (kind, heading) -> state?.rows?.firstOrNull { it.kind == kind && it.items.isNotEmpty() }?.let { it to heading } }
    }
    // «Populært no», then the requests, then what is coming. A request Spole gave twice is drawn once.
    val (popular, coming) = remember(shown) { shown.partition { (row, _) -> row.kind == DiscoverRowKind.TRENDING } }
    val requests = remember(state?.requests) { state?.requests.orEmpty().distinctBy { it.key } }
    // The card in focus as «row/title», kept while a title's page lies over the rows so Back lands on it again.
    var focusedAt by rememberSaveable { mutableStateOf<String?>(null) }
    val focused = focusedAt?.let { at ->
        shown.firstNotNullOfOrNull { (row, _) -> row.items.firstOrNull { spot(row.kind, it) == at } }
            ?: requests.firstOrNull { requestSpot(it) == at }?.asDiscoverTitle()
    }
    val hero = focused ?: state?.rows?.firstOrNull { it.kind == DiscoverRowKind.TRENDING }?.items?.firstOrNull()
    val start = focusedAt.takeIf { focused != null } ?: popular.firstOrNull()?.let { (row, _) -> spot(row.kind, row.items.first()) }
        ?: requests.firstOrNull()?.let(::requestSpot) ?: coming.firstOrNull()?.let { (row, _) -> spot(row.kind, row.items.first()) }
    // «Prøv att» stands only while nothing is being asked: a retry says it is fetching, and the button comes back if Seerr fails again.
    val showRetry = state != null && state.failed && state.rows.isEmpty() && !state.loading
    val first = remember { FocusRequester() }
    val retry = remember { FocusRequester() }
    // Focus where the remote can act at once: on a card once there are rows, on «Prøv att» each time it is drawn.
    LaunchedEffect(start != null, showRetry) {
        withFrameNanos { }
        if (start != null) runCatching { first.requestFocus() } else if (showRetry) runCatching { retry.requestFocus() }
    }
    // The profile's background choice holds here too: the theme's own surface shows no title's picture.
    val themeSurface = LocalAppearance.current.backgroundSource == BackgroundSource.THEME
    Box(modifier.fillMaxSize()) {
        BackdropLayer(hero?.backdrop.takeUnless { themeSurface }, state = backdrop)
        Column(Modifier.fillMaxSize().padding(top = StovaLayout.TvSafeEdge)) {
            PageHeader(stringResource(R.string.discover_title), StovaIcons.Discover, Modifier.padding(horizontal = StovaLayout.TvSafeEdge),
                subtitle = stringResource(R.string.discover_subtitle)) {
                StovaButton(stringResource(R.string.discover_search), onOpenSearch, icon = StovaIcons.Search, compact = true)
            }
            if (state?.access == SeerrAccess.BROWSE) PageNote(R.string.discover_browse_only)
            when {
                state == null || (state.loading && state.rows.isEmpty()) -> PageNote(R.string.discover_loading)
                showRetry -> {
                    PageNote(R.string.discover_failed)
                    StovaButton(stringResource(R.string.discover_retry), onRetry,
                        Modifier.padding(start = StovaLayout.TvSafeEdge, top = 16.dp).focusRequester(retry))
                }
                else -> {
                    DiscoverHero(hero, Modifier.padding(start = StovaLayout.TvSafeEdge, end = StovaLayout.TvSafeEdge, top = 16.dp))
                    // Cards scroll out to the screen's edge as on Home; the rows' own padding keeps them at the safe edge.
                    LazyColumn(Modifier.weight(1f).testTag("discover-rows"), contentPadding = PaddingValues(bottom = StovaLayout.TvSafeEdge)) {
                        fun titleRows(rows: List<Pair<DiscoverRow, Int>>) {
                            for ((row, heading) in rows) item(key = row.kind.name) {
                                val here = start?.takeIf { it.startsWith("${row.kind.name}/") }
                                DiscoverTitleRow(stringResource(heading), row.items, onFocus = { focusedAt = spot(row.kind, it) }, onOpen = onOpenTitle,
                                    focus = first.takeIf { here != null }, focusOn = here?.substringAfter('/'))
                            }
                        }
                        titleRows(popular)
                        if (requests.isNotEmpty()) item(key = REQUESTS_ROW) {
                            val here = start?.takeIf { it.startsWith("$REQUESTS_ROW/") }
                            DiscoverRequestRow(stringResource(R.string.discover_row_requests), requests, onFocus = { focusedAt = requestSpot(it) },
                                onOpen = onOpenRequest, focus = first.takeIf { here != null }, focusOn = here?.substringAfter('/'))
                        }
                        titleRows(coming)
                    }
                }
            }
        }
    }
}

/** A card's place on the page: its row and its title, since a title can be in more than one row. */
private fun spot(kind: DiscoverRowKind, title: DiscoverTitle) = "${kind.name}/${title.key}"

/** A request's place on the page, in the row «Dine førespurnader». */
private fun requestSpot(request: DiscoverRequest) = "$REQUESTS_ROW/${request.key}"

@Composable
private fun PageNote(@StringRes text: Int) {
    Text(stringResource(text), Modifier.padding(start = StovaLayout.TvSafeEdge, end = StovaLayout.TvSafeEdge, top = 16.dp),
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The title in focus: its name, its facts (year, three genres, ★ and what Seerr says) and the start of its
 * story. Always three lines of story tall, so the rows below never move as focus walks.
 */
@Composable
internal fun DiscoverHero(title: DiscoverTitle?, modifier: Modifier = Modifier) {
    val locale = LocalLocale.current.platformLocale
    Column(modifier.testTag("discover-hero").widthIn(max = HERO_WIDTH).fillMaxWidth().softEntrance(title?.key),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title?.title.orEmpty(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        val facts = title?.let {
            listOfNotNull(it.year?.toString()) + it.genres.take(3) + listOfNotNull(
                it.community?.takeIf { score -> score in 0.0..10.0 }?.let { score -> stringResource(R.string.appearance_star_score, communityText(score, locale)) },
                seerrStatusLabel(it.status)?.let { label -> stringResource(label) })
        }.orEmpty().joinToString(" · ")
        Text(facts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title?.overview.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A heading and its titles as cards, drawn as Home draws its rows; OK, or a long press, opens the title.
 * [focus] goes on the card [focusOn] names, else on the first, and the row opens scrolled to it.
 */
@Composable
internal fun DiscoverTitleRow(heading: String, items: List<DiscoverTitle>, onFocus: (DiscoverTitle) -> Unit, onOpen: (DiscoverTitle) -> Unit,
    modifier: Modifier = Modifier, focus: FocusRequester? = null, focusOn: String? = null) {
    val appearance = LocalAppearance.current
    val poster = appearance.movieStyle == MovieStyle.POSTER
    val index = items.indexOfFirst { it.key == focusOn }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column(modifier) {
        RowHeader(heading)
        LazyRow(Modifier.focusRestorer(), state = state, contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(appearance.spacingDp.dp)) {
            itemsIndexed(items, key = { _, item -> item.key }) { i, item ->
                val card = remember(item) { item.asMediaItem(SEERR_CARDS) }
                MediaCard(card, poster, { onOpen(item) }, { onOpen(item) }, { onFocus(item) },
                    if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
            }
        }
    }
}

/** The person's requests as [DiscoverRequestCard]s, laid out as [DiscoverTitleRow] lays out titles; OK opens the title. */
@Composable
private fun DiscoverRequestRow(heading: String, requests: List<DiscoverRequest>, onFocus: (DiscoverRequest) -> Unit,
    onOpen: (DiscoverRequest) -> Unit, focus: FocusRequester? = null, focusOn: String? = null) {
    val appearance = LocalAppearance.current
    val index = requests.indexOfFirst { it.key == focusOn }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        RowHeader(heading)
        LazyRow(Modifier.focusRestorer(), state = state, contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(appearance.spacingDp.dp)) {
            itemsIndexed(requests, key = { _, request -> request.key }) { i, request ->
                DiscoverRequestCard(request, { onOpen(request) }, Modifier.onFocusChanged { if (it.hasFocus) onFocus(request) }
                    .then(if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier))
            }
        }
    }
}
