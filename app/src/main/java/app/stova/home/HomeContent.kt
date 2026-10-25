package app.stova.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.*
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.ui.*
import app.stova.profiles.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var topBarFocused by remember { mutableStateOf(false) }
    var topReturn by remember { mutableIntStateOf(0) }
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
        Box(Modifier.fillMaxWidth().onFocusChanged { state ->
            if (state.hasFocus && !topBarFocused) topReturn++
            topBarFocused = state.hasFocus
        }.focusGroup()) { topBar() }
        if (movingRow != null) Text(stringResource(R.string.custom_move_hint), Modifier.padding(horizontal = StovaLayout.TvSafeEdge),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.weight(1f)) {
            val viewportHeight = maxHeight
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
                when {
                    rows == null -> if (appearance.heroSize != HeroSize.OFF) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                    rows.isEmpty() -> {
                        if (appearance.heroSize != HeroSize.OFF) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                        EmptyHome(onOpenApps)
                    }
                    else -> {
                        val headerItems = if (appearance.heroSize == HeroSize.OFF) 0 else 1
                        fun scrollIndex(rowIndex: Int) = if (rowIndex == 0) 0 else rowIndex + headerItems
                        val firstRow = rows.indexOfFirst { it.key == restoreRow }.coerceAtLeast(0)
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = scrollIndex(firstRow))
                        LaunchedEffect(restoreRow, rows) {
                            // Media rows can arrive after the apps row; start focus follows them until the viewer has moved.
                            if (restoreRow == null || restoreRow == placedRow || (placedRow != null && viewerMoved)) return@LaunchedEffect
                            // A row inserted above the visible ones is not composed yet: bring it in first.
                            rows.indexOfFirst { it.key == restoreRow }.takeIf { it >= 0 }?.let { listState.scrollToItem(scrollIndex(it)) }
                            // Lazy rows compose during layout, after this effect starts; give the requester a few frames to attach.
                            repeat(10) {
                                if (runCatching { firstItem.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) { placedRow = restoreRow; return@LaunchedEffect }
                                withFrameNanos { }
                            }
                        }
                        val scope = rememberCoroutineScope()
                        val reduceMotion = LocalReduceMotion.current
                        LaunchedEffect(topReturn) {
                            if (topReturn > 0) {
                                if (reduceMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
                            }
                        }
                        val rowSpec = LocalBringIntoViewSpec.current
                        val lastStyle = homePrefs.rowFormats[rows.last().key] ?: RowStyle()
                        val lastArt = artworkDimensions(lastStyle.format ?: defaultRowFormat(rows.last().key, appearance), lastStyle.size ?: appearance.cardSize).height.dp
                        val lastRowHeight = appearance.rowSpacing.top.dp + (if (lastStyle.showTitle) 25.dp * fontScale + StovaLayout.SectionBottom else 0.dp) + lastArt +
                            (if (appearance.showTitles) 8.dp + 18.dp * fontScale else 0.dp) + 4.dp
                        // Even the last row can reach the top after the hero has scrolled away.
                        val bottomSpace = (viewportHeight - lastRowHeight).coerceAtLeast(StovaLayout.TvSafeEdge)
                        // The column does not scroll to a focused card; the focused row snaps its header
                        // to the top instead, so the row above never peeks out as a bare bar under the hero.
                        CompositionLocalProvider(LocalBringIntoViewSpec provides StayPut) {
                            LazyColumn(Modifier.fillMaxSize().focusRestorer(), state = listState,
                                contentPadding = PaddingValues(bottom = bottomSpace)) {
                                if (headerItems != 0) item(key = "spotlight-header", contentType = "hero") {
                                    ParallaxHero(spotlight, heroHeight, listState)
                                }
                                itemsIndexed(rows, key = { _, row -> row.key.value }) { index, row ->
                                    val requester = if (row.key == restoreRow) firstItem else null
                                    CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                        val rowRequester = remember(row.key) { requesters.getOrPut(row.key) { FocusRequester() } }
                                        val style = homePrefs.rowFormats[row.key] ?: RowStyle()
                                        var hasFocus by remember { mutableStateOf(false) }
                                        LaunchedEffect(movingRow, index) {
                                            if (movingRow == row.key) listState.scrollToItem(scrollIndex(index))
                                        }
                                        Box(Modifier.focusRestorer().onFocusChanged { state ->
                                            val gainedFocus = state.hasFocus && !hasFocus
                                            hasFocus = state.hasFocus
                                            if (gainedFocus) scope.launch {
                                                if (reduceMotion) listState.scrollToItem(scrollIndex(index)) else listState.animateScrollToItem(scrollIndex(index))
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
