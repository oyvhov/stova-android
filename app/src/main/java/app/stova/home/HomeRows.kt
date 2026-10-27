package app.stova.home

import app.stova.profiles.spacingDp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.*
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.ui.*
import app.stova.profiles.*
import kotlinx.coroutines.launch

@Composable
internal fun MediaRow(row: HomeRowUi.Media, focus: FocusRequester?, restore: FocusedTarget?,
    onFocus: (FocusedTarget) -> Unit, onClick: (FocusedTarget.Media) -> Unit, onLongClick: (FocusedTarget.Media) -> Unit, returnFocus: FocusRequester?) {
    val index = row.items.indexOfFirst { it.id == (restore as? FocusedTarget.Media)?.item?.id }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        if (LocalRowStyle.current.showTitle) MediaRowHeader(row) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.spacingDp.dp)) {
            itemsIndexed(row.items, key = { _, item -> item.id }) { i, item ->
                val target = FocusedTarget.Media(item, row.account)
                DrawnFocus(i == index && focus != null) {
                    MediaCard(item, row.kind == MediaRowKind.LATEST_MOVIES && app.stova.core.theme.LocalAppearance.current.movieStyle == app.stova.profiles.MovieStyle.POSTER,
                        { onClick(target) }, { onLongClick(target) }, { onFocus(target) },
                        (if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
                            .then(if (i == index && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier))
                }
            }
        }
    }
}

@Composable
internal fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?, restore: FocusedTarget?, iconOverrides: Map<String, IconOverride>, onLongClick: (InstalledApp) -> Unit, returnFocus: FocusRequester?,
    onShortcut: (FavoriteTile) -> Unit = {}, onShortcutLongClick: (FavoriteTile) -> Unit = {}, onFocused: (String) -> Unit) {
    val index = row.tiles.indexOfFirst { it.label == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        if (LocalRowStyle.current.showTitle) RowHeader(stringResource(if (row.favorites) R.string.row_favorite_apps else R.string.row_apps)) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.spacingDp.dp)) {
            itemsIndexed(row.tiles, key = { _, tile -> tile.key }) { i, tile ->
                val modifier = (if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
                    .then(if (i == index && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) onFocused(tile.label) }
                DrawnFocus(i == index && focus != null) {
                    if (tile is FavoriteTile.App) AppTile(tile.app, art, onClick = { catalog.launch(tile.app) }, onLongClick = { onLongClick(tile.app) },
                        modifier = modifier, iconOverride = iconOverrides[tile.key])
                    else ShortcutTile(tile, onClick = { onShortcut(tile) }, onLongClick = { onShortcutLongClick(tile) }, modifier = modifier)
                }
            }
        }
    }
}

/** In Settings' small Home, the card that focus would land on is drawn focused. */
@Composable
private fun DrawnFocus(start: Boolean, content: @Composable () -> Unit) =
    CompositionLocalProvider(app.stova.core.ui.LocalDrawnFocus provides (start && LocalHomePreview.current), content = content)

@Composable
internal fun EmptyHome(onOpenApps: () -> Unit) {
    val button = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { button.requestFocus() } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.home_empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StovaButton(stringResource(R.string.home_open_apps), onOpenApps, Modifier.focusRequester(button), primary = true)
        }
    }
}

@Composable
internal fun WatchNextRow(row: HomeRowUi.WatchNext, focus: FocusRequester?, onFocus: (FocusedTarget) -> Unit, onLongClick: () -> Unit,
    returnFocus: FocusRequester?, restore: FocusedTarget?) {
    val context = LocalContext.current
    val index = row.entries.indexOfFirst { it.title == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    Column {
        if (LocalRowStyle.current.showTitle) RowHeader(stringResource(R.string.row_watch_next)) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.spacingDp.dp)) {
            itemsIndexed(row.entries, key = { _, entry -> entry.id }) { i, entry ->
              DrawnFocus(i == index && focus != null) {
                app.stova.watchnext.WatchNextCard(entry, onClick = {
                    val intent = app.stova.watchnext.launchIntentFor(entry)
                    try {
                        if (intent == null) throw android.content.ActivityNotFoundException()
                        context.startActivity(intent)
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(context, R.string.app_open_failed, Toast.LENGTH_SHORT).show()
                    } catch (_: SecurityException) {
                        Toast.makeText(context, R.string.app_open_failed, Toast.LENGTH_SHORT).show()
                    }
                }, onFocused = { onFocus(FocusedTarget.App(entry.title)) },
                    modifier = (if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
                        .then(if (i == index && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier), onLongClick = onLongClick)
              }
            }
        }
    }
}
