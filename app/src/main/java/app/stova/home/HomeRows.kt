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
                MediaCard(item, row.kind == MediaRowKind.LATEST_MOVIES && app.stova.core.theme.LocalAppearance.current.movieStyle == app.stova.profiles.MovieStyle.POSTER,
                    { onClick(target) }, { onLongClick(target) }, { onFocus(target) },
                    (if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
                        .then(if (i == index && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier))
            }
        }
    }
}

@Composable
internal fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?, restore: FocusedTarget?, iconOverrides: Map<String, IconOverride>, onLongClick: (InstalledApp) -> Unit, returnFocus: FocusRequester?, onFocused: (String) -> Unit) {
    val index = row.apps.indexOfFirst { it.label == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        if (LocalRowStyle.current.showTitle) RowHeader(stringResource(if (row.favorites) R.string.row_favorite_apps else R.string.row_apps)) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.spacingDp.dp)) {
            itemsIndexed(row.apps, key = { _, app -> app.key }) { i, app ->
                val modifier = (if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
                    .then(if (i == index && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier)
                AppTile(app, art, onClick = { catalog.launch(app) }, onLongClick = { onLongClick(app) },
                    modifier = modifier.onFocusChanged { if (it.isFocused) onFocused(app.label) }, iconOverride = iconOverrides[app.key])
            }
        }
    }
}

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
