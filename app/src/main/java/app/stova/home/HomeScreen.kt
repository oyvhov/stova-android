package app.stova.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    onRefresh: suspend () -> Unit = {},
    topBar: @Composable () -> Unit = {},
) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, onRefresh) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) { onRefresh(); kotlinx.coroutines.delay(300_000) }
        }
    }
    val rows by vm.rows.collectAsStateWithLifecycle()
    val focused by vm.focused.collectAsStateWithLifecycle()
    val reduceMotion = app.stova.core.theme.LocalReduceMotion.current
    val rotationUrls = rows.orEmpty().filterIsInstance<HomeRowUi.Media>().filter { it.kind != app.stova.media.MediaRowKind.CONTINUE }.flatMap { it.items }.mapNotNull { it.art.backdrop }.distinct()
    val rotationEnabled = vm.rotationEnabled()
    LaunchedEffect(rotationUrls, reduceMotion, rotationEnabled) { vm.backdrop.rotation(rotationUrls, !reduceMotion && rotationEnabled) }
    val backdrop by vm.backdrop.current.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(backdrop, rotationUrls, reduceMotion) {
        if (!reduceMotion && rotationUrls.isNotEmpty() && vm.rotationEnabled()) {
            val next = rotationUrls[(rotationUrls.indexOf(backdrop) + 1) % rotationUrls.size]
            coil3.SingletonImageLoader.get(context).enqueue(coil3.request.ImageRequest.Builder(context).data(next).size(1920, 1080).build())
        }
    }
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
        BoxWithConstraints(Modifier.weight(1f)) {
        val heroHeight = maxHeight * 0.45f
        Column {
        HeroPanel(focused, Modifier.fillMaxWidth().heightIn(min = heroHeight))
        val loaded = rows
        when {
            loaded == null -> Unit // still loading: the top bar alone, never a flash of the empty state
            loaded.isEmpty() -> EmptyHome(onOpenApps)
            else -> LazyColumn(
                Modifier.fillMaxSize().focusRestorer(),
                contentPadding = PaddingValues(bottom = StovaLayout.TvSafeEdge),
            ) {
                items(loaded, key = { it.key.value }) { row ->
                    when (row) {
                        is HomeRowUi.Media -> Column {
                            app.stova.media.ui.MediaRowHeader(row)
                            LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge), horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap)) {
                                itemsIndexed(row.items, key = { _, item -> item.id }) { index, item ->
                                    app.stova.media.ui.MediaCard(item, row.kind == app.stova.media.MediaRowKind.LATEST_MOVIES, {}, {}, { vm.focus(FocusedTarget.Media(item, row.account)) },
                                        if (index == 0 && row.key == initial) Modifier.focusRequester(firstItem) else Modifier)
                                }
                            }
                        }
                        is HomeRowUi.Apps -> AppsRow(row, art, catalog, if (row.key == initial) firstItem else null) { label -> vm.focus(FocusedTarget.App(label)) }
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
private fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?, onFocused: (String) -> Unit) {
    Column {
        RowHeader(stringResource(R.string.row_favorite_apps))
        LazyRow(
            Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap),
        ) {
            itemsIndexed(row.apps, key = { _, app -> app.key }) { index, app ->
                val modifier = if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier
                AppTile(app, art, onClick = { catalog.launch(app) }, onLongClick = {}, modifier = modifier.onFocusChanged { if (it.isFocused) onFocused(app.label) })
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
