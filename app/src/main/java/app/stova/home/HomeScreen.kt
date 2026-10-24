package app.stova.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.stova.R
import app.stova.apps.*
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.ha.HaConnection
import app.stova.ha.HaRepository
import app.stova.ha.ui.ChipContent
import app.stova.ha.ui.chipText
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.ui.*
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    art: AppArtCache,
    catalog: AppCatalog,
    onOpenApps: () -> Unit,
    onRefresh: suspend () -> Unit = {},
    topBar: @Composable (List<ChipContent>) -> Unit = {},
    haRepository: HaRepository? = null,
    haConnection: HaConnection? = null,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { refresh(); delay(300_000) }
        }
    }
    val chips = if (haRepository != null && haConnection != null) {
        val cards by haRepository.homeCards.collectAsStateWithLifecycle()
        val entities by haConnection.entities.collectAsStateWithLifecycle()
        val locale = LocalConfiguration.current.locales[0]
        cards.map { chipText(it, entities[it.entityId], locale) }
    } else emptyList()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val focused by vm.focused.collectAsStateWithLifecycle()
    val initial by vm.initialFocus.collectAsStateWithLifecycle()
    val backdrop by vm.backdrop.current.collectAsStateWithLifecycle()
    val reduceMotion = LocalReduceMotion.current
    val rotationEnabled = vm.rotationEnabled()
    val rotationUrls = rows.orEmpty().filterIsInstance<HomeRowUi.Media>()
        .filter { it.kind != MediaRowKind.CONTINUE }.flatMap { it.items }
        .mapNotNull { it.art.backdrop }.distinct()
    LaunchedEffect(rotationUrls, reduceMotion, rotationEnabled) {
        vm.backdrop.rotation(rotationUrls, !reduceMotion && rotationEnabled)
    }
    val context = LocalContext.current
    LaunchedEffect(backdrop, rotationUrls, reduceMotion, rotationEnabled) {
        if (!reduceMotion && rotationEnabled && rotationUrls.isNotEmpty()) {
            val next = rotationUrls[(rotationUrls.indexOf(backdrop) + 1) % rotationUrls.size]
            SingletonImageLoader.get(context).enqueue(ImageRequest.Builder(context).data(next).size(1920, 1080).build())
        }
    }
    val handoff = remember(context) { SpoleHandoff(context) }
    var actions by remember { mutableStateOf<FocusedTarget.Media?>(null) }
    var missing by remember { mutableStateOf(false) }
    fun open(target: FocusedTarget.Media, action: SpoleAction) {
        when (handoff.open(target.account, target.item, action)) {
            HandoffResult.Opened -> Unit
            HandoffResult.SpoleMissing -> missing = true
            HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
        }
    }
    HomeContent(rows, focused, initial, art, catalog, onOpenApps, vm::focus,
        onMediaClick = { open(it, SpoleAction.DETAILS) }, onMediaLongClick = { actions = it }, topBar = { topBar(chips) })
    actions?.let { target ->
        ItemActionsMenu(target.item,
            onPlay = { actions = null; open(target, SpoleAction.PLAY) },
            onDetails = { actions = null; open(target, SpoleAction.DETAILS) },
            onDismiss = { actions = null })
    }
    if (missing) SpoleMissingDialog { missing = false }
}

/** The rendered home with controlled data, shared by the launcher and visual tests. */
@Composable
fun HomeContent(
    rows: List<HomeRowUi>?,
    focused: FocusedTarget?,
    initial: HomeRowKey?,
    art: AppArtCache,
    catalog: AppCatalog,
    onOpenApps: () -> Unit,
    onFocus: (FocusedTarget) -> Unit,
    onMediaClick: (FocusedTarget.Media) -> Unit,
    onMediaLongClick: (FocusedTarget.Media) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    // The last focused title survives leaving Home for Spole or another launcher page.
    val restoreTarget = remember { focused }
    val restoreRow = rows?.firstOrNull { row ->
        when (row) {
            is HomeRowUi.Media -> (restoreTarget as? FocusedTarget.Media)?.let { target ->
                row.account.id == target.account.id && row.items.any { it.id == target.item.id }
            } == true
            is HomeRowUi.Apps -> restoreTarget is FocusedTarget.App
            is HomeRowUi.WatchNext -> false
        }
    }?.key ?: initial
    val firstItem = remember { FocusRequester() }
    var placedFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreRow, rows) {
        if (restoreRow != null && !placedFocus) placedFocus = runCatching { firstItem.requestFocus() }.isSuccess
    }
    Column(Modifier.fillMaxSize()) {
        topBar()
        BoxWithConstraints(Modifier.weight(1f)) {
            val heroHeight = maxHeight * 0.45f
            Column {
                HeroPanel(focused, Modifier.fillMaxWidth().heightIn(min = heroHeight))
                when {
                    rows == null -> Unit
                    rows.isEmpty() -> EmptyHome(onOpenApps)
                    else -> {
                        val firstRow = rows.indexOfFirst { it.key == restoreRow }.coerceAtLeast(0)
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstRow)
                        LazyColumn(Modifier.fillMaxSize().focusRestorer(), state = listState,
                            contentPadding = PaddingValues(bottom = StovaLayout.TvSafeEdge)) {
                            items(rows, key = { it.key.value }) { row ->
                                val requester = if (row.key == restoreRow) firstItem else null
                                when (row) {
                                    is HomeRowUi.WatchNext -> WatchNextRow(row, requester, onFocus)
                                    is HomeRowUi.Media -> MediaRow(row, requester, restoreTarget, onFocus, onMediaClick, onMediaLongClick)
                                    is HomeRowUi.Apps -> AppsRow(row, art, catalog, requester, restoreTarget) { onFocus(FocusedTarget.App(it)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(row: HomeRowUi.Media, focus: FocusRequester?, restore: FocusedTarget?,
    onFocus: (FocusedTarget) -> Unit, onClick: (FocusedTarget.Media) -> Unit, onLongClick: (FocusedTarget.Media) -> Unit) {
    val index = row.items.indexOfFirst { it.id == (restore as? FocusedTarget.Media)?.item?.id }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        MediaRowHeader(row)
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap)) {
            itemsIndexed(row.items, key = { _, item -> item.id }) { i, item ->
                val target = FocusedTarget.Media(item, row.account)
                MediaCard(item, row.kind == MediaRowKind.LATEST_MOVIES,
                    { onClick(target) }, { onLongClick(target) }, { onFocus(target) },
                    if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier)
            }
        }
    }
}

@Composable
private fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?, restore: FocusedTarget?, onFocused: (String) -> Unit) {
    val index = row.apps.indexOfFirst { it.label == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        RowHeader(stringResource(R.string.row_favorite_apps))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap)) {
            itemsIndexed(row.apps, key = { _, app -> app.key }) { i, app ->
                val modifier = if (i == index && focus != null) Modifier.focusRequester(focus) else Modifier
                AppTile(app, art, onClick = { catalog.launch(app) }, onLongClick = {},
                    modifier = modifier.onFocusChanged { if (it.isFocused) onFocused(app.label) })
            }
        }
    }
}

@Composable
private fun EmptyHome(onOpenApps: () -> Unit) {
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
private fun WatchNextRow(row: HomeRowUi.WatchNext, focus: FocusRequester?, onFocus: (FocusedTarget) -> Unit) {
    val context = LocalContext.current
    Column {
        RowHeader(stringResource(R.string.row_watch_next))
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(StovaLayout.RowGap)) {
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
                    modifier = if (i == 0 && focus != null) Modifier.focusRequester(focus) else Modifier)
            }
        }
    }
}
