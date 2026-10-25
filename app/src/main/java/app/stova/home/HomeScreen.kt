package app.stova.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import app.stova.profiles.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    profile: app.stova.profiles.Profile? = null,
    apps: AppRepository? = null,
    onHomeChange: (HomePrefs) -> Unit = {},
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
        val entities by haRepository.entities.collectAsStateWithLifecycle()
        val locale = LocalConfiguration.current.locales[0]
        cards.map { chipText(it, entities[it.entityId], locale) }
    } else emptyList()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val focused by vm.focused.collectAsStateWithLifecycle()
    val initial by vm.initialFocus.collectAsStateWithLifecycle()
    val spotlight by vm.spotlight.collectAsStateWithLifecycle()
    val reduceMotion = LocalReduceMotion.current
    val rotationEnabled = vm.rotationEnabled()
    val heroEnabled = app.stova.core.theme.LocalAppearance.current.heroSize != HeroSize.OFF
    LaunchedEffect(reduceMotion, rotationEnabled, heroEnabled) { vm.setSpotlightRotation(!reduceMotion && rotationEnabled && heroEnabled) }
    val context = LocalContext.current
    val handoff = remember(context) { SpoleHandoff(context) }
    var actions by remember { mutableStateOf<FocusedTarget.Media?>(null) }
    var appMenu by remember(profile?.id) { mutableStateOf<InstalledApp?>(null) }
    var missing by remember { mutableStateOf(false) }
    fun open(target: FocusedTarget.Media, action: SpoleAction) {
        when (handoff.open(target.account, target.item, action)) {
            HandoffResult.Opened -> Unit
            HandoffResult.OpenedWithoutLink -> Toast.makeText(context, R.string.spole_update_for_links, Toast.LENGTH_LONG).show()
            HandoffResult.SpoleMissing -> missing = true
            HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
        }
    }
    HomeContent(rows, focused, initial, art, catalog, onOpenApps, vm::focus,
        onMediaClick = { open(it, SpoleAction.DETAILS) }, onMediaLongClick = { actions = it }, topBar = { topBar(chips) },
        spotlight = spotlight, appIconOverrides = profile?.apps?.iconOverrides.orEmpty(),
        onAppLongClick = { app -> if (profile?.kid == false) appMenu = app },
        homePrefs = profile?.home ?: HomePrefs(),
        onHomeChange = onHomeChange, canEditRows = profile?.kid == false,
        onMediaPlay = { open(it, SpoleAction.PLAY) },
        appActions = { app, rowActions, dismiss ->
            if (profile != null && apps != null) AppMenuHost(app, apps, profile, art, catalog, rowActions, dismiss)
        })
    appMenu?.let { app -> if (profile != null && apps != null) AppMenuHost(app, apps, profile, art, catalog) { appMenu = null } }
    actions?.let { target ->
        ItemActionsMenu(target.item,
            onPlay = { actions = null; open(target, SpoleAction.PLAY) },
            onDetails = { actions = null; open(target, SpoleAction.DETAILS) },
            onDismiss = { actions = null })
    }
    if (missing) SpoleMissingDialog { missing = false }
}

/** The rendered home with controlled data, shared by the launcher and visual tests. */
@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec, the TV pivot hook
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
    spotlight: SpotlightEntry? = null,
    appIconOverrides: Map<String, IconOverride> = emptyMap(),
    onAppLongClick: (InstalledApp) -> Unit = {},
    homePrefs: HomePrefs = HomePrefs(),
    onHomeChange: (HomePrefs) -> Unit = {},
    canEditRows: Boolean = false,
    onMediaPlay: (FocusedTarget.Media) -> Unit = onMediaClick,
    appActions: (@Composable (InstalledApp, @Composable () -> Unit, () -> Unit) -> Unit)? = null,
) {
    val appearance = app.stova.core.theme.LocalAppearance.current
    val rowTargets = remember { mutableStateMapOf<HomeRowKey, FocusedTarget>() }
    var menuRow by remember { mutableStateOf<HomeRowUi?>(null) }
    var menuMedia by remember { mutableStateOf<FocusedTarget.Media?>(null) }
    var menuApp by remember { mutableStateOf<InstalledApp?>(null) }
    var movingRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var hiddenRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var returnRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var focusReturn by remember { mutableIntStateOf(0) }
    var undoFocused by remember { mutableStateOf(false) }
    val currentRows by rememberUpdatedState(rows)
    val requesters = remember { mutableMapOf<HomeRowKey, FocusRequester>() }
    fun restore(key: HomeRowKey?) { returnRow = key; focusReturn++ }
    fun closeMenu() { val key = menuRow?.key; menuRow = null; menuMedia = null; menuApp = null; restore(key) }
    BackHandler(enabled = movingRow != null) { val key = movingRow; movingRow = null; restore(key) }
    LaunchedEffect(focusReturn) {
        val requester = requesters[returnRow] ?: return@LaunchedEffect
        repeat(12) {
            withFrameNanos { }
            if (runCatching { requester.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    LaunchedEffect(hiddenRow) {
        val hidden = hiddenRow ?: return@LaunchedEffect
        delay(8_000)
        hiddenRow = null
        if (undoFocused) restore(currentRows.orEmpty().firstOrNull { it.key != hidden }?.key)
    }
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
    var placedRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var viewerMoved by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    SeasonalScene(Modifier.matchParentSize())
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) viewerMoved = true
        val key = movingRow
        if (key == null) false else when (event.key) {
            Key.DirectionUp, Key.DirectionDown -> {
                if (event.type == KeyEventType.KeyDown) {
                    onHomeChange(moveHomeRow(homePrefs, rows.orEmpty().map { it.key }, key, if (event.key == Key.DirectionUp) -1 else 1))
                }
                true
            }
            Key.DirectionCenter, Key.Enter, Key.Back, Key.Escape -> {
                if (event.type == KeyEventType.KeyUp) { movingRow = null; restore(key) }
                true
            }
            else -> true
        }
    }.semantics {
        // Automation marker, not user-facing copy. Null rows mean sources are still loading.
        if (rows != null) contentDescription = "home-ready"
    }) {
        topBar()
        if (movingRow != null) Text(stringResource(R.string.custom_move_hint), Modifier.padding(horizontal = StovaLayout.TvSafeEdge),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.weight(1f)) {
            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val artworkHeight = rows.orEmpty().maxOfOrNull { row ->
                val style = homePrefs.rowFormats[row.key] ?: RowStyle()
                artworkDimensions(style.format ?: defaultRowFormat(row.key, appearance), style.size ?: appearance.cardSize).height
            }?.dp ?: StovaLayout.WideHeight
            val rowHeight = appearance.rowSpacing.top.dp + 25.dp * fontScale + StovaLayout.SectionBottom + artworkHeight +
                (if (appearance.showTitles) 8.dp + 18.dp * fontScale else 0.dp) + 4.dp + StovaLayout.TvSafeEdge
            // Fixed for these profile/font choices, independent of each spotlight title. Reserve
            // room for a whole row; even a large poster's optional caption must remain reachable.
            val baseHeroHeight = if (fontScale >= 1.5f) maxHeight * (appearance.heroSize.fraction + 0.16f)
                else minOf(maxHeight * appearance.heroSize.fraction, (maxHeight - rowHeight).coerceAtLeast(0.dp))
            val heroHeight = if (appearance.heroSize == HeroSize.OFF) 0.dp else if (appearance.showOverview && fontScale < 1.5f) maxOf(baseHeroHeight, 208.dp) else baseHeroHeight
            Column {
                // A fixed height: the rows never move when the spotlight turns to a longer title.
                if (appearance.heroSize != HeroSize.OFF) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                when {
                    rows == null -> Unit
                    rows.isEmpty() -> EmptyHome(onOpenApps)
                    else -> {
                        val firstRow = rows.indexOfFirst { it.key == restoreRow }.coerceAtLeast(0)
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstRow)
                        LaunchedEffect(restoreRow, rows) {
                            // Media rows can arrive after the apps row; start focus follows them until the viewer has moved.
                            if (restoreRow == null || restoreRow == placedRow || (placedRow != null && viewerMoved)) return@LaunchedEffect
                            // A row inserted above the visible ones is not composed yet: bring it in first.
                            rows.indexOfFirst { it.key == restoreRow }.takeIf { it >= 0 }?.let { listState.scrollToItem(it) }
                            // Lazy rows compose during layout, after this effect starts; give the requester a few frames to attach.
                            repeat(10) {
                                if (runCatching { firstItem.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) { placedRow = restoreRow; return@LaunchedEffect }
                                withFrameNanos { }
                            }
                        }
                        val scope = rememberCoroutineScope()
                        val reduceMotion = LocalReduceMotion.current
                        val rowSpec = LocalBringIntoViewSpec.current
                        // The column does not scroll to a focused card; the focused row snaps its header
                        // to the top instead, so the row above never peeks out as a bare bar under the hero.
                        CompositionLocalProvider(LocalBringIntoViewSpec provides StayPut) {
                            LazyColumn(Modifier.fillMaxSize().focusRestorer(), state = listState,
                                contentPadding = PaddingValues(bottom = StovaLayout.TvSafeEdge)) {
                                itemsIndexed(rows, key = { _, row -> row.key.value }) { index, row ->
                                    val requester = if (row.key == restoreRow) firstItem else null
                                    CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                        val rowRequester = remember(row.key) { requesters.getOrPut(row.key) { FocusRequester() } }
                                        val style = homePrefs.rowFormats[row.key] ?: RowStyle()
                                        var hasFocus by remember { mutableStateOf(false) }
                                        LaunchedEffect(movingRow, index) {
                                            if (movingRow == row.key) listState.scrollToItem(index)
                                        }
                                        Box(Modifier.focusRestorer().onFocusChanged { state ->
                                            hasFocus = state.hasFocus
                                            if (state.hasFocus) scope.launch {
                                                if (reduceMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)
                                            }
                                        }.focusGroup().then(if (movingRow == row.key) Modifier.padding(horizontal = 32.dp)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large) else Modifier)) {
                                          CompositionLocalProvider(LocalRowStyle provides style,
                                            LocalRowHasFocus provides hasFocus) {
                                            val rowMenu: () -> Unit = { if (canEditRows) menuRow = row }
                                            val rowTarget = rowTargets[row.key] ?: restoreTarget
                                            val trackFocus: (FocusedTarget) -> Unit = { rowTargets[row.key] = it; onFocus(it) }
                                            when (row) {
                                                is HomeRowUi.WatchNext -> WatchNextRow(row, requester, trackFocus, rowMenu, rowRequester, rowTarget)
                                                is HomeRowUi.Media -> MediaRow(row, requester, rowTarget, trackFocus, onMediaClick, { target ->
                                                    if (canEditRows) { menuMedia = target; rowMenu() } else onMediaLongClick(target)
                                                }, rowRequester)
                                                is HomeRowUi.Apps -> AppsRow(row, art, catalog, requester, rowTarget, appIconOverrides, { app ->
                                                    if (canEditRows) { menuApp = app; rowMenu() } else onAppLongClick(app)
                                                }, rowRequester) { trackFocus(FocusedTarget.App(it)) }
                                            }
                                          }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    hiddenRow?.let { hidden ->
        androidx.compose.material3.Surface(Modifier.align(Alignment.BottomCenter).padding(24.dp), shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.custom_row_hidden), color = MaterialTheme.colorScheme.onSurfaceVariant)
                StovaButton(stringResource(R.string.custom_undo), {
                    onHomeChange(homePrefs.copy(hiddenRows = homePrefs.hiddenRows - hidden)); hiddenRow = null; restore(hidden)
                }, Modifier.onFocusChanged { undoFocused = it.hasFocus })
            }
        }
    }
    }
    menuRow?.let { row ->
        val controls: @Composable () -> Unit = {
            RowMenuSection(row.key, homePrefs, onHomeChange,
                onMove = { movingRow = row.key; closeMenu() },
                onHide = {
                    onHomeChange(homePrefs.copy(hiddenRows = homePrefs.hiddenRows + row.key,
                        rowOrder = (homePrefs.rowOrder + rows.orEmpty().map { it.key }).distinct()))
                    hiddenRow = row.key; closeMenu()
                    restore(rows.orEmpty().firstOrNull { it.key != row.key }?.key)
                })
        }
        when {
            menuApp != null && appActions != null -> appActions(menuApp!!, controls, ::closeMenu)
            menuMedia != null -> ItemActionsMenu(menuMedia!!.item,
                { val target = menuMedia!!; closeMenu(); onMediaPlay(target) },
                { val target = menuMedia!!; closeMenu(); onMediaClick(target) }, ::closeMenu, controls)
            else -> app.stova.core.ui.StovaDialog(::closeMenu) {
                androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.width(440.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { controls() }
                }
            }
        }
    }
}

private val StayPut = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
}

@Composable
private fun MediaRow(row: HomeRowUi.Media, focus: FocusRequester?, restore: FocusedTarget?,
    onFocus: (FocusedTarget) -> Unit, onClick: (FocusedTarget.Media) -> Unit, onLongClick: (FocusedTarget.Media) -> Unit, returnFocus: FocusRequester?) {
    val index = row.items.indexOfFirst { it.id == (restore as? FocusedTarget.Media)?.item?.id }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        if (LocalRowStyle.current.showTitle) MediaRowHeader(row) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.cardSpacing.dp.dp)) {
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
private fun AppsRow(row: HomeRowUi.Apps, art: AppArtCache, catalog: AppCatalog, focus: FocusRequester?, restore: FocusedTarget?, iconOverrides: Map<String, IconOverride>, onLongClick: (InstalledApp) -> Unit, returnFocus: FocusRequester?, onFocused: (String) -> Unit) {
    val index = row.apps.indexOfFirst { it.label == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    Column {
        if (LocalRowStyle.current.showTitle) RowHeader(stringResource(if (row.favorites) R.string.row_favorite_apps else R.string.row_apps)) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), state = state,
            contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.cardSpacing.dp.dp)) {
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
private fun WatchNextRow(row: HomeRowUi.WatchNext, focus: FocusRequester?, onFocus: (FocusedTarget) -> Unit, onLongClick: () -> Unit,
    returnFocus: FocusRequester?, restore: FocusedTarget?) {
    val context = LocalContext.current
    val index = row.entries.indexOfFirst { it.title == (restore as? FocusedTarget.App)?.label }.coerceAtLeast(0)
    Column {
        if (LocalRowStyle.current.showTitle) RowHeader(stringResource(R.string.row_watch_next)) else Spacer(Modifier.height(app.stova.core.theme.LocalAppearance.current.rowSpacing.top.dp))
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge),
            horizontalArrangement = Arrangement.spacedBy(app.stova.core.theme.LocalAppearance.current.cardSpacing.dp.dp)) {
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
