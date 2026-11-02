package app.stova.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.TileFrame
import app.stova.core.theme.LocalAppearance
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.PageHeader
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.StovaIcons
import app.stova.home.LocalArtworkScale
import app.stova.home.LocalRowStyle
import app.stova.home.artworkDimensions
import app.stova.media.ui.MediaCard
import app.stova.profiles.RowFormat

/** Posters across the television's width, whatever card size Home uses. */
private const val SEARCH_COLUMNS = 6

/** The fewest characters, spaces at either end left out, that are searched for. */
internal const val MIN_QUERY = 2

/** The remote's arrows: the viewer moving on. */
private val MOVES = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)

/**
 * Search on the Oppdag page: the field, then what Seerr found as posters six across, with «Hent fleire» last while
 * Seerr has more. The field has focus as the step opens and Down goes from it to the first title; Back from a
 * title's page lands on the card it was opened from. [answered] is the query [page] answers: when it found nothing,
 * the page says so in its words. While Seerr is asked and nothing is shown yet, the page says it is fetching; when
 * Seerr gave nothing for the query's first page ([failed]) and nothing is shown, it says Seerr did not answer. A query
 * shorter than [MIN_QUERY] characters says nothing. After «Hent fleire» the next page's first title takes focus.
 */
@Composable
fun DiscoverSearchContent(query: String, page: DiscoverPage?, answered: String?, loading: Boolean, failed: Boolean, onQuery: (String) -> Unit,
    onMore: () -> Unit, onOpenTitle: (DiscoverTitle) -> Unit, modifier: Modifier = Modifier) {
    val items = page?.items.orEmpty()
    val field = remember { FocusRequester() }
    val first = remember { FocusRequester() }
    val back = remember { FocusRequester() }
    val grown = remember { FocusRequester() }
    // The card in focus, kept while a title's page lies over the search so Back lands on it again.
    var focusedAt by rememberSaveable { mutableStateOf<String?>(null) }
    val start = remember { focusedAt?.takeIf { key -> items.any { it.key == key } } }
    // How many titles there were when «Hent fleire» was pressed: the next page's first stands there. The viewer moving
    // with the remote forgets it. The system moving does not: with no page after it the tile goes, and focus with it.
    var grownFrom by remember { mutableStateOf<Int?>(null) }
    val grownAt = grownFrom?.coerceAtMost(items.lastIndex)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { if (start != null) back.requestFocus() else field.requestFocus() }
    }
    // A page added is never the first: a new query's answer takes no focus from the field.
    LaunchedEffect(page?.page) {
        if (grownFrom == null || (page?.page ?: 1) <= 1) return@LaunchedEffect
        withFrameNanos { }
        runCatching { grown.requestFocus() }
        grownFrom = null
    }
    val asked = query.trim()
    val note = when {
        items.isNotEmpty() -> null
        loading -> stringResource(R.string.discover_loading)
        asked.length < MIN_QUERY -> null
        failed -> stringResource(R.string.discover_failed)
        page != null && answered != null -> stringResource(R.string.discover_no_hits, answered)
        else -> null
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardWidth = artworkDimensions(RowFormat.POSTER, LocalAppearance.current.cardSize).width
        val fit = (maxWidth - StovaLayout.TvSafeEdge * 2 - StovaLayout.RowGap * (SEARCH_COLUMNS - 1)) / SEARCH_COLUMNS / cardWidth.dp
        CompositionLocalProvider(LocalArtworkScale provides fit) {
            LazyVerticalGrid(GridCells.Fixed(SEARCH_COLUMNS), Modifier.fillMaxSize().testTag("discover-search").onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key in MOVES) grownFrom = null
                    false
                },
                contentPadding = PaddingValues(StovaLayout.TvSafeEdge),
                horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap), verticalArrangement = Arrangement.spacedBy(StovaLayout.RowGap)) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    PageHeader(stringResource(R.string.discover_search), StovaIcons.Search)
                }
                item(key = "field", span = { GridItemSpan(maxLineSpan) }) {
                    // Down goes to the first title, not to the one under the middle of the field.
                    RemoteTextField(query, onQuery, stringResource(R.string.discover_search_hint),
                        Modifier.fillMaxWidth().focusRequester(field).focusProperties { if (items.isNotEmpty()) down = first }
                            .onFocusChanged { if (it.isFocused) focusedAt = null })
                }
                if (note != null) item(key = "note", span = { GridItemSpan(maxLineSpan) }) {
                    Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(items, key = { _, title -> title.key }) { i, title ->
                    val card = remember(title) { title.asMediaItem(SEERR_CARDS) }
                    MediaCard(card, poster = true, { onOpenTitle(title) }, { onOpenTitle(title) }, { focusedAt = title.key },
                        Modifier.then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                            .then(if (title.key == start) Modifier.focusRequester(back) else Modifier)
                            .then(if (i == grownAt) Modifier.focusRequester(grown) else Modifier))
                }
                if (page?.hasMore == true) item(key = "more") { MoreTile { grownFrom = items.size; onMore() } }
            }
        }
    }
}

/** «Hent fleire» as the grid's last tile, in a poster's shape, as «Oppdag» ends Home's row. */
@Composable
private fun MoreTile(onClick: () -> Unit) {
    val label = stringResource(R.string.discover_more)
    CompositionLocalProvider(LocalRowStyle provides LocalRowStyle.current.copy(format = RowFormat.POSTER)) {
        TileFrame(label, 0, onClick, onClick) { spec ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(StovaIcons.Plus, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!spec.captions) Text(label, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
