package app.stova.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.apps.AppArtCache
import app.stova.apps.AppCatalog
import app.stova.apps.AppTile
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    art: AppArtCache,
    catalog: AppCatalog,
    onOpenApps: () -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val initial by vm.initialFocus.collectAsStateWithLifecycle()
    val firstItem = remember { FocusRequester() }
    var placedFocus by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initial) {
        if (initial != null && !placedFocus) {
            placedFocus = runCatching { firstItem.requestFocus() }.isSuccess
        }
    }

    Column(Modifier.fillMaxSize()) {
        topBar()
        if (rows.isEmpty()) {
            EmptyHome(onOpenApps)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().focusRestorer(),
                contentPadding = PaddingValues(bottom = StovaLayout.TvSafeEdge),
            ) {
                items(rows, key = { it.key.value }) { row ->
                    when (row) {
                        is HomeRowUi.Apps -> AppsRow(row, art, catalog, if (row.key == initial) firstItem else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?) {
    Column {
        RowHeader(stringResource(R.string.row_favorite_apps))
        LazyRow(
            Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
        ) {
            itemsIndexed(row.apps, key = { _, app -> app.key }) { index, app ->
                val modifier = if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier
                AppTile(app, art, onClick = { catalog.launch(app) }, onLongClick = {}, modifier = modifier)
            }
        }
    }
}

@Composable
private fun EmptyHome(onOpenApps: () -> Unit) {
    // Something must hold focus on a TV, or the first press of the remote only wakes focus up.
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
