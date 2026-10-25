package app.stova.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
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
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.core.ui.smoothScrollToItem
import app.stova.core.theme.stovaMotion
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
    onRowFocus: (HomeRowKey) -> Unit = {},
) {
    val appearance = app.stova.core.theme.LocalAppearance.current
    val deck = appearance.homeRows != HomeRows.SCROLLING
    val availableRows = remember(rows, deck) { if (deck) rows?.filter { it.hasFocusableItems() } else rows }
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
    val currentRows by rememberUpdatedState(availableRows)
    val requesters = remember { mutableMapOf<HomeRowKey, FocusRequester>() }
    fun restore(key: HomeRowKey?) { returnRow = key; focusReturn++ }
    fun closeMenu() { val key = menuRow?.key; menuRow = null; menuMedia = null; menuApp = null; restore(key) }
    BackHandler(enabled = movingRow != null) { val key = movingRow; movingRow = null; restore(key) }
    LaunchedEffect(hiddenRow) {
        val hidden = hiddenRow ?: return@LaunchedEffect
        delay(8_000)
        hiddenRow = null
        if (undoFocused) restore(currentRows.orEmpty().firstOrNull { it.key != hidden }?.key)
    }
    // The last focused title survives leaving Home for Spole or another launcher page.
    val restoreTarget = remember { focused }
    fun matchesRestore(row: HomeRowUi): Boolean = when (row) {
            is HomeRowUi.Media -> (restoreTarget as? FocusedTarget.Media)?.let { target ->
                row.account.id == target.account.id && row.items.any { it.id == target.item.id }
            } == true
            is HomeRowUi.Apps -> restoreTarget is FocusedTarget.App && row.apps.any { it.label == restoreTarget.label }
            is HomeRowUi.WatchNext -> restoreTarget is FocusedTarget.App && row.entries.any { it.title == restoreTarget.label }
    }
    val requestedRow = availableRows?.firstOrNull { it.key == initial && matchesRestore(it) }?.key
        ?: availableRows?.firstOrNull(::matchesRestore)?.key ?: initial
    val restoreRow = if (deck) requestedRow?.takeIf { key -> availableRows.orEmpty().any { it.key == key } }
        ?: availableRows?.firstOrNull()?.key else requestedRow
    val firstItem = remember { FocusRequester() }
    var placedRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var activeRow by remember { mutableStateOf(restoreRow) }
    var viewerMoved by remember { mutableStateOf(false) }
    val topBarFocus = remember { FocusRequester() }
    val focusScope = rememberCoroutineScope()
    var rowNavigation by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var pendingRow by remember { mutableStateOf<HomeRowKey?>(null) }
    var navigationVersion by remember { mutableIntStateOf(0) }
    val motion = stovaMotion
    fun scrollIndex(index: Int) = if (deck || index == 0 || appearance.heroSize == HeroSize.OFF) index else index + 1
    val initialRow = availableRows.orEmpty().indexOfFirst { it.key == (activeRow ?: restoreRow) }.coerceAtLeast(0)
    val listState = key(appearance.homeRows) { rememberLazyListState(initialFirstVisibleItemIndex = scrollIndex(initialRow)) }
    LaunchedEffect(focusReturn) {
        if (focusReturn == 0) return@LaunchedEffect
        val index = availableRows.orEmpty().indexOfFirst { it.key == returnRow }
        if (index < 0) return@LaunchedEffect
        listState.smoothScrollToItem(scrollIndex(index), motion)
        repeat(12) {
            withFrameNanos { }
            if (runCatching { requesters[returnRow]?.requestFocus() == true }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    LaunchedEffect(appearance.homeRows) {
        rowNavigation?.cancel(); pendingRow = null
        // Switching presentation replaces the lazy list, but keeps the viewer's row and card.
        if (placedRow != null && !topBarFocused) repeat(12) {
            withFrameNanos { }
            if (runCatching { requesters[activeRow]?.requestFocus(FocusDirection.Enter) == true }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    fun focusRow(index: Int) {
        rowNavigation?.cancel()
        navigationVersion++
        val version = navigationVersion
        pendingRow = null
        if (index < 0) { runCatching { topBarFocus.requestFocus(FocusDirection.Enter) }; return }
        val key = currentRows?.getOrNull(index)?.key ?: return
        pendingRow = key
        rowNavigation = focusScope.launch {
            try {
                withFrameNanos { }
                listState.smoothScrollToItem(scrollIndex(index), motion)
                repeat(12) {
                    if (runCatching { requesters[key]?.requestFocus(FocusDirection.Enter) == true }.getOrDefault(false)) return@launch
                    withFrameNanos { }
                }
            } finally {
                if (version == navigationVersion) pendingRow = null
            }
        }
    }
    fun nextRow(from: Int, step: Int): Int? = currentRows.orEmpty().indices
        .filter { if (step > 0) it > from else it < from }
        .let { if (step > 0) it else it.reversed() }
        .firstOrNull { currentRows!![it].hasFocusableItems() }
    Box(Modifier.fillMaxSize()) {
    SeasonalScene(Modifier.matchParentSize())
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            viewerMoved = true
            // A newer arrow owns focus, even when it only moves within the header or a row.
            // Never let a lazy-row request from an earlier Down steal it on the next frame.
            if (event.key in listOf(Key.DirectionLeft, Key.DirectionRight)) {
                rowNavigation?.cancel()
                pendingRow = null
            }
        }
        val key = movingRow
        if (key == null) false else when (event.key) {
            Key.DirectionUp, Key.DirectionDown -> {
                if (event.type == KeyEventType.KeyDown) {
                    onHomeChange(moveHomeRow(homePrefs, availableRows.orEmpty().map { it.key }, key, if (event.key == Key.DirectionUp) -1 else 1))
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
        if (availableRows != null) contentDescription = "home-ready"
    }) {
        Box(Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
            if (event.key != Key.DirectionDown) false else {
                if (event.type == KeyEventType.KeyDown) nextRow(-1, 1)?.let(::focusRow)
                true
            }
        }.focusRequester(topBarFocus).focusRestorer().onFocusChanged { state ->
            if (state.hasFocus && !topBarFocused) topReturn++
            topBarFocused = state.hasFocus
        }.focusGroup()) { topBar() }
        if (movingRow != null) Text(stringResource(R.string.custom_move_hint), Modifier.padding(horizontal = StovaLayout.TvSafeEdge),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.weight(1f)) {
            val viewportHeight = maxHeight
            val geometry = if (deck) homeDeckGeometry(availableRows.orEmpty(), homePrefs, appearance, maxWidth, maxHeight) else null
            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val artworkHeight = availableRows.orEmpty().maxOfOrNull { row ->
                val style = homePrefs.rowFormats[row.key] ?: RowStyle()
                artworkDimensions(style.format ?: defaultRowFormat(row.key, appearance), style.size ?: appearance.cardSize).height
            }?.dp ?: StovaLayout.WideHeight
            val rowHeight = appearance.rowSpacing.top.dp + 25.dp * fontScale + StovaLayout.SectionBottom + artworkHeight +
                (if (appearance.showTitles) 8.dp + 18.dp * fontScale else 0.dp) + 4.dp + StovaLayout.TvSafeEdge
            // Fixed for these profile/font choices, independent of each spotlight title. Reserve
            // room for a whole row; even a large poster's optional caption must remain reachable.
            val baseHeroHeight = if (fontScale >= 1.5f) maxHeight * (appearance.heroSize.fraction + 0.16f)
                else minOf(maxHeight * appearance.heroSize.fraction, (maxHeight - rowHeight).coerceAtLeast(0.dp))
            val deckHeroSpace = geometry?.let { (maxHeight - it.height - it.bottom).coerceAtLeast(0.dp) }
            val heroHeight = if (appearance.heroSize == HeroSize.OFF) 0.dp else if (deckHeroSpace != null) {
                if (deckHeroSpace < if (fontScale >= 1.5f) 140.dp else 112.dp) 0.dp
                else minOf(deckHeroSpace, maxOf(maxHeight * appearance.heroSize.fraction,
                    if (appearance.showOverview && fontScale < 1.5f) 208.dp else 0.dp))
            } else if (appearance.showOverview && fontScale < 1.5f) maxOf(baseHeroHeight, 208.dp) else baseHeroHeight
            Column {
                when {
                    availableRows == null -> if (appearance.heroSize != HeroSize.OFF) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                    availableRows.isEmpty() -> {
                        if (appearance.heroSize != HeroSize.OFF) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                        EmptyHome(onOpenApps)
                    }
                    else -> {
                        val headerItems = if (deck || appearance.heroSize == HeroSize.OFF) 0 else 1
                        if (deck) {
                            if (heroHeight > 0.dp) HeroPanel(spotlight, Modifier.fillMaxWidth().height(heroHeight))
                            Spacer(Modifier.weight(1f))
                        }
                        LaunchedEffect(restoreRow, availableRows) {
                            // Media rows can arrive after apps; start focus follows them until the viewer has moved.
                            if (restoreRow == null || restoreRow == placedRow || (placedRow != null && viewerMoved)) return@LaunchedEffect
                            // A row inserted above the visible ones is not composed yet: bring it in first.
                            availableRows.indexOfFirst { it.key == restoreRow }.takeIf { it >= 0 }?.let { listState.scrollToItem(scrollIndex(it)) }
                            // Lazy rows compose during layout, after this effect starts; allow the requester to attach.
                            repeat(10) {
                                if (runCatching { firstItem.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) { placedRow = restoreRow; return@LaunchedEffect }
                                withFrameNanos { }
                            }
                        }
                        val scope = rememberCoroutineScope()
                        LaunchedEffect(topReturn) {
                            if (topReturn > 0) {
                                if (!deck && pendingRow == null) listState.smoothScrollToItem(0, motion)
                            }
                        }
                        val rowSpec = LocalBringIntoViewSpec.current
                        val lastStyle = homePrefs.rowFormats[availableRows.last().key] ?: RowStyle()
                        val lastArt = artworkDimensions(lastStyle.format ?: defaultRowFormat(availableRows.last().key, appearance), lastStyle.size ?: appearance.cardSize).height.dp
                        val lastRowHeight = appearance.rowSpacing.top.dp + (if (lastStyle.showTitle) 25.dp * fontScale + StovaLayout.SectionBottom else 0.dp) + lastArt +
                            (if (appearance.showTitles) 8.dp + 18.dp * fontScale else 0.dp) + 4.dp
                        // Even the last row can reach the top after the hero has scrolled away.
                        val bottomSpace = (viewportHeight - lastRowHeight).coerceAtLeast(StovaLayout.TvSafeEdge)
                        // One animation owns the column, including the hero. Individual cards must
                        // not start a second vertical bring-into-view scroll during a row transition.
                        CompositionLocalProvider(LocalBringIntoViewSpec provides StayPut) {
                            LazyColumn(Modifier.then(if (geometry != null) Modifier.fillMaxWidth().height(geometry.height).clipToBounds()
                                else Modifier.fillMaxSize()).focusRestorer(), state = listState,
                                contentPadding = PaddingValues(bottom = if (deck) 0.dp else bottomSpace)) {
                                if (headerItems != 0) item(key = "spotlight-header", contentType = "hero") {
                                    ParallaxHero(spotlight, heroHeight, listState)
                                }
                                itemsIndexed(availableRows, key = { _, row -> row.key.value }) { index, row ->
                                    val requester = if (row.key == restoreRow) firstItem else null
                                    CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                                        val rowRequester = remember(row.key) { requesters.getOrPut(row.key) { FocusRequester() } }
                                        val style = homePrefs.rowFormats[row.key] ?: RowStyle()
                                        var hasFocus by remember { mutableStateOf(false) }
                                        LaunchedEffect(movingRow, index) {
                                            if (movingRow == row.key) listState.scrollToItem(scrollIndex(index))
                                        }
                                        Box(Modifier.onPreviewKeyEvent { event ->
                                            if (event.key !in listOf(Key.DirectionUp, Key.DirectionDown)) false else {
                                                if (event.type == KeyEventType.KeyDown) {
                                                    val step = if (event.key == Key.DirectionUp) -1 else 1
                                                    val from = pendingRow?.let { key -> currentRows.orEmpty().indexOfFirst { it.key == key } } ?: index
                                                    val next = nextRow(from, step)
                                                    if (next != null) focusRow(next) else if (step < 0) focusRow(-1)
                                                }
                                                true
                                            }
                                        }.focusProperties {
                                            onExit = { if (requestedFocusDirection in listOf(FocusDirection.Left, FocusDirection.Right)) cancelFocusChange() }
                                        }.focusRestorer().onFocusChanged { state ->
                                            val gainedFocus = state.hasFocus && !hasFocus
                                            hasFocus = state.hasFocus
                                            if (gainedFocus) { activeRow = row.key; onRowFocus(row.key) }
                                            if (gainedFocus && pendingRow == null) scope.launch {
                                                listState.smoothScrollToItem(scrollIndex(index), motion)
                                            }
                                        }.focusGroup().then(if (geometry != null) Modifier.fillMaxWidth().height(geometry.slotHeight).padding(vertical = 4.dp) else Modifier)
                                            .then(if (movingRow == row.key) Modifier.padding(horizontal = 32.dp)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large) else Modifier),
                                            contentAlignment = if (deck) Alignment.BottomStart else Alignment.TopStart) {
                                          CompositionLocalProvider(LocalRowStyle provides style,
                                            LocalRowHasFocus provides hasFocus,
                                            LocalArtworkScale provides (geometry?.artworkScale ?: 1f)) {
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
                        if (geometry != null) Box(Modifier.fillMaxWidth().height(geometry.bottom)
                            .padding(horizontal = StovaLayout.TvSafeEdge), contentAlignment = Alignment.CenterEnd) {
                            if (availableRows.size > 1) Text(stringResource(R.string.home_row_counter,
                                availableRows.indexOfFirst { it.key == (pendingRow ?: activeRow) }.coerceAtLeast(0) + 1, availableRows.size),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        rowOrder = (homePrefs.rowOrder + availableRows.orEmpty().map { it.key }).distinct()))
                    hiddenRow = row.key; closeMenu()
                    restore(availableRows.orEmpty().firstOrNull { it.key != row.key }?.key)
                })
        }
        when {
            menuApp != null && appActions != null -> appActions(menuApp!!, controls, ::closeMenu)
            menuMedia != null -> ItemActionsMenu(menuMedia!!.item,
                { val target = menuMedia!!; closeMenu(); onMediaPlay(target) },
                { val target = menuMedia!!; closeMenu(); onMediaClick(target) }, ::closeMenu, controls)
            else -> app.stova.core.ui.StovaDialog(::closeMenu) {
                androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.width(440.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { controls() }
                }
            }
        }
    }
}

private val StayPut = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
}

private fun HomeRowUi.hasFocusableItems(): Boolean = when (this) {
    is HomeRowUi.Apps -> apps.isNotEmpty()
    is HomeRowUi.Media -> items.isNotEmpty()
    is HomeRowUi.WatchNext -> entries.isNotEmpty()
}
