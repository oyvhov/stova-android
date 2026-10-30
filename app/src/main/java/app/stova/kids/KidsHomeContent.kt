package app.stova.kids

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.AppArtCache
import app.stova.apps.FavoriteTile
import app.stova.apps.InstalledApp
import app.stova.core.theme.KidsColors
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.smoothScrollToItem
import app.stova.home.FocusedTarget
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.model.ItemType
import app.stova.media.model.MediaItem
import app.stova.profiles.AvatarBadge
import app.stova.profiles.Profile
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The sky above the rows: room for the greeting, the sun or moon, and Pip above the card in focus. */
private val SkyBand = 188.dp
private val PosterWidth = 156.dp
private val PosterHeight = 234.dp
private val AppWidth = 252.dp
private val AppHeight = 142.dp
private fun pipWidth(threeD: Boolean) = if (threeD) 156.dp else 120.dp
private val CardShape = RoundedCornerShape(22.dp)
/** After this long without a press, Pip settles and the world stops drifting. */
private const val LIVELY_MILLIS = 15_000L
private const val SPEAK_AFTER_MILLIS = 900L

/**
 * The children's Home: a picture-book world by season and hour, big pictures in a few rows, and
 * Pip, who flies to whatever is in focus, says its name and reacts. Nothing here needs reading.
 * [onSpeak] reads a name aloud; null keeps Pip quiet.
 */
@OptIn(ExperimentalFoundationApi::class) // LocalBringIntoViewSpec, the TV pivot hook
@Composable
fun KidsHomeContent(
    profile: Profile,
    rows: List<HomeRowUi>?,
    art: AppArtCache?,
    date: LocalDate,
    time: LocalTime,
    onPlay: (FocusedTarget.Media) -> Unit,
    onApp: (InstalledApp) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeak: ((String) -> Unit)? = null,
    /** False when the parents chose the films' own artwork behind the rows instead of Pip's world. */
    world: Boolean = true,
    seasonOverride: KidsSeason? = null,
) {
    val season = seasonOverride ?: profile.appearance.kidsSeason(date)
    val part = DayPart.at(time)
    val bedtime = profile.pip.bedtimeAt(time)
    val shown = remember(rows) { kidsOrder(rows.orEmpty().filter { it.isKidsRow() }) }

    var poke by remember { mutableIntStateOf(0) }
    var lively by remember { mutableStateOf(true) }
    LaunchedEffect(poke) { lively = true; delay(LIVELY_MILLIS); lively = false }
    var welcome by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(6_000); welcome = false }

    var origin by remember { mutableStateOf(Offset.Zero) }
    var screen by remember { mutableStateOf(Offset.Zero) }
    val focusedCard = remember { mutableStateOf(Rect.Zero) }
    var focusedTitle by remember { mutableStateOf<String?>(null) }
    var focusedLabel by remember { mutableStateOf<String?>(null) }
    var focusedNew by remember { mutableStateOf(false) }
    var focusedRow by remember { mutableIntStateOf(0) }
    var curious by remember { mutableStateOf(false) }
    LaunchedEffect(focusedTitle) { if (focusedTitle != null) { curious = true; delay(600); curious = false } }

    val greeting = greetingFor(part, bedtime, profile.name)
    val speak by rememberUpdatedState(onSpeak)
    LaunchedEffect(Unit) { delay(700); if (profile.pip.readAloud) speak?.invoke(greeting) }
    LaunchedEffect(focusedTitle, bedtime) {
        val title = focusedTitle ?: return@LaunchedEffect
        if (bedtime == Bedtime.ASLEEP || !profile.pip.readAloud) return@LaunchedEffect
        delay(SPEAK_AFTER_MILLIS); speak?.invoke(title)
    }

    val density = LocalDensity.current
    val threeD = rememberPip3d()
    val pipSize = DpSize(pipWidth(threeD), pipWidth(threeD) / pipAspect(threeD))
    val pipW = with(density) { pipSize.width.toPx() }; val pipH = with(density) { pipSize.height.toPx() }
    val margin = with(density) { 16.dp.toPx() }
    /** Pip stands on the top edge of the card in focus, towards its right-hand side. */
    fun pipTarget(): Offset? {
        if (screen == Offset.Zero) return null
        val feet = pipH * pipFeet(threeD)
        val card = focusedCard.value
        // He stands a little in from the card's right-hand corner.
        val inset = with(density) { 24.dp.toPx() }
        return if (card == Rect.Zero) Offset(screen.x / 2f - pipW / 2f, with(density) { (SkyBand + 54.dp).toPx() } - feet)
        else Offset((card.right - inset - pipW / 2f).coerceIn(margin - pipW * 0.12f, (screen.x - pipW * 0.88f - margin).coerceAtLeast(margin)),
            card.top - feet + with(density) { 4.dp.toPx() })
    }
    val reduce = app.stova.core.theme.LocalReduceMotion.current
    val pip = remember { PipMotion() }
    SideEffect { pip.lively = lively; pip.still = reduce; pip.asleep = bedtime == Bedtime.ASLEEP }
    LaunchedEffect(pip) { pip.run() }
    LaunchedEffect(Unit) { pip.greet(2.8f) }
    LaunchedEffect(Unit) { snapshotFlow { pipTarget() }.collect { target -> if (target != null) pip.moveTo(target) } }
    val scope = rememberCoroutineScope()
    // A hop and a cheer before the film opens, so the press feels answered.
    val play: (FocusedTarget.Media) -> Unit = { target ->
        pip.celebrate()
        scope.launch { if (!reduce) delay(520); onPlay(target) }
    }
    val mood = when {
        bedtime == Bedtime.ASLEEP -> PipMood.ASLEEP
        bedtime == Bedtime.SOON -> PipMood.SLEEPY
        welcome -> PipMood.WAVE
        rows == null || curious -> PipMood.CURIOUS
        focusedNew -> PipMood.EXCITED
        else -> PipMood.HAPPY
    }
    val outfit = if (bedtime != Bedtime.AWAKE) PipOutfit.NIGHTCAP else season.outfit()

    Box(modifier.fillMaxSize()
        .onGloballyPositioned { origin = it.positionInRoot(); screen = Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
        .onPreviewKeyEvent { poke++; if (welcome) welcome = false; false }
        .testTag("kids-home")) {
        if (world) KidsWorld(season, part, lively)

        val listState = rememberLazyListState()
        val motion = stovaMotion
        // By the row too, not just its place: a row that arrives above (Continue, on a first visit) takes that place.
        LaunchedEffect(focusedRow, shown.getOrNull(focusedRow)?.key) { listState.smoothScrollToItem(focusedRow, motion) }
        val rowSpec = LocalBringIntoViewSpec.current
        CompositionLocalProvider(LocalBringIntoViewSpec provides StayPut) {
            LazyColumn(Modifier.fillMaxSize().focusRestorer(), state = listState,
                contentPadding = PaddingValues(top = SkyBand, bottom = 420.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                itemsIndexed(shown, key = { _, row -> row.key.value }) { index, row ->
                    // Rows the child has gone past leave the sky to Pip and the greeting.
                    val present by animateFloatAsState(if (index < focusedRow) 0f else 1f, tween(320), label = "row")
                    CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                        Box(Modifier.graphicsLayer { alpha = present }) { KidsRow(row, art, first = index == 0, focused = index == focusedRow,
                            onFocused = { title, label, isNew -> focusedRow = index; focusedTitle = title; focusedLabel = label; focusedNew = isNew },
                            onMoved = { bounds -> focusedCard.value = bounds.translate(-origin) },
                            onPlay = play, onApp = onApp) }
                    }
                }
            }
        }

        val say = when {
            bedtime == Bedtime.ASLEEP -> stringResource(R.string.kids_pip_asleep)
            rows == null -> stringResource(R.string.kids_loading)
            shown.isEmpty() -> stringResource(R.string.kids_empty)
            // He says hello himself, in his bubble beside him: up by the avatar his hat would cover it.
            welcome -> greeting
            bedtime == Bedtime.SOON -> focusedTitle?.let { stringResource(R.string.kids_bedtime_soon_with, it) } ?: stringResource(R.string.kids_bedtime_soon)
            else -> focusedTitle
        }
        Header(profile, greeting, (welcome || rows.isNullOrEmpty()) && say != greeting, onProfile)

        val pipShown = profile.pip.shown
        if (pipShown) PipFigure(mood, outfit, pip, part, threeD, Modifier.offset { IntOffset(pip.x.roundToInt(), pip.y.roundToInt()) }.size(pipSize))
        // He says it with his light mouth too, whether or not his voice is on.
        LaunchedEffect(say) { if (say != null) { delay(200); pip.say(say) } }
        // The row's name goes small above the title, since Pip stands where the row's own label was.
        val label = focusedLabel.takeIf { say == focusedTitle && say != null }
        if (say != null) Bubble(label, say, pip, pipW, pipH, screen.x, if (pipShown) null else focusedCard)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val StayPut = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
}

/** What they were watching first, then their apps, then everything else in the order it came. */
private fun kidsOrder(rows: List<HomeRowUi>): List<HomeRowUi> {
    val first = rows.filter { it is HomeRowUi.Media && it.kind == MediaRowKind.CONTINUE }
    val apps = rows.filterIsInstance<HomeRowUi.Apps>()
    return first + apps + (rows - first.toSet() - apps.toSet())
}

private fun HomeRowUi.isKidsRow(): Boolean = when (this) {
    is HomeRowUi.Media -> items.isNotEmpty()
    is HomeRowUi.Apps -> tiles.any { it is FavoriteTile.App }
    is HomeRowUi.WatchNext -> false
}

@Composable
private fun greetingFor(part: DayPart, bedtime: Bedtime, name: String): String = stringResource(when {
    bedtime == Bedtime.ASLEEP -> R.string.kids_hello_night
    part == DayPart.MORNING -> R.string.kids_hello_morning
    part == DayPart.EVENING || part == DayPart.NIGHT -> R.string.kids_hello_evening
    else -> R.string.kids_hello_day
}, name)

@Composable
private fun Header(profile: Profile, greeting: String, showGreeting: Boolean, onProfile: () -> Unit) {
    val alpha by animateFloatAsState(if (showGreeting) 1f else 0f, tween(700), label = "greeting")
    Row(Modifier.padding(start = StovaLayout.TvSafeEdge, top = 28.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val source = remember { MutableInteractionSource() }
        var focused by remember { mutableStateOf(false) }
        val label = stringResource(R.string.nav_profile)
        Box(Modifier.size(64.dp).graphicsLayer { scaleX = if (focused) 1.1f else 1f; scaleY = scaleX }
            .border(if (focused) 4.dp else 0.dp, KidsColors.Focus, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(source, indication = null, role = Role.Button, onClick = onProfile)
            .semantics { text = androidx.compose.ui.text.AnnotatedString(label) }, contentAlignment = Alignment.Center) {
            AvatarBadge(profile, 58.dp)
        }
        if (alpha > 0f) Text(greeting, Modifier.graphicsLayer { this.alpha = alpha }.background(KidsColors.Label, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 10.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = KidsColors.LabelText)
    }
    Box(Modifier.fillMaxWidth().padding(end = StovaLayout.TvSafeEdge, top = 34.dp), contentAlignment = Alignment.TopEnd) {
        Box(Modifier.background(KidsColors.Label, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
            app.stova.core.ui.Clock()
        }
    }
}

/** What Pip says, beside him: big, dark on white, never more than two lines. */
@Composable
private fun Bubble(label: String?, text: String, pip: PipMotion, pipW: Float, pipH: Float, screenW: Float, card: State<Rect>?) {
    val appear = remember(text) { Animatable(0f) }
    LaunchedEffect(text) { delay(180); appear.animateTo(1f, tween(220)) }
    val gap = with(LocalDensity.current) { 6.dp.toPx() }

    val lift = with(LocalDensity.current) { 10.dp.toPx() }
    Column(Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = minOf(constraints.maxWidth, 420.dp.roundToPx())))
            layout(placeable.width, placeable.height) {
                val anchor = card?.value
                if (anchor != null && anchor != Rect.Zero) {
                    // Without Pip, the name sits over the card itself.
                    placeable.place(anchor.left.roundToInt(), (anchor.top - placeable.height - lift).roundToInt())
                } else {
                    // Beside his head: he is about three quarters of his view wide.
                    val centre = pip.x + pipW / 2f
                    val right = centre + pipW * 0.38f + gap
                    val x = if (right + placeable.width <= screenW - gap) right else centre - pipW * 0.38f - placeable.width - gap
                    placeable.place(x.roundToInt(), (pip.y + pipH * 0.25f).roundToInt())
                }
            }
        }
        .graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 8.dp.toPx() }
        .background(KidsColors.Bubble, RoundedCornerShape(22.dp))
        .padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = KidsColors.BubbleText.copy(alpha = 0.6f), maxLines = 1)
        Text(text, Modifier.testTag("pip-says"), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = KidsColors.BubbleText,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun KidsRow(row: HomeRowUi, art: AppArtCache?, first: Boolean, focused: Boolean, onFocused: (String, String, Boolean) -> Unit, onMoved: (Rect) -> Unit,
    onPlay: (FocusedTarget.Media) -> Unit, onApp: (InstalledApp) -> Unit) {
    val start = remember { FocusRequester() }
    // The first card takes focus as soon as the row has laid it out.
    if (first) LaunchedEffect(Unit) {
        repeat(30) { withFrameNanos { }; if (runCatching { start.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val (icon, label) = rowLook(row)
        // The row in focus lends its name to Pip's bubble; the others keep theirs.
        val labelAlpha by animateFloatAsState(if (focused) 0f else 1f, tween(260), label = "row-label")
        Row(Modifier.padding(start = StovaLayout.TvSafeEdge).graphicsLayer { alpha = labelAlpha }.background(KidsColors.Label, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = KidsColors.NewBadge)
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = KidsColors.LabelText)
        }
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = StovaLayout.TvSafeEdge, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            when (row) {
                is HomeRowUi.Media -> itemsIndexed(row.items, key = { _, item -> item.id }) { i, item ->
                    val title = item.seriesName?.takeIf { item.type == ItemType.EPISODE } ?: item.title
                    KidsCard(PosterWidth, PosterHeight, title, { onPlay(FocusedTarget.Media(item, row.account)) }, { onFocused(title, label, item.isNew) }, onMoved,
                        if (i == 0) Modifier.focusRequester(start) else Modifier, place = i) { Poster(item) }
                }
                is HomeRowUi.Apps -> itemsIndexed(row.tiles.filterIsInstance<FavoriteTile.App>(), key = { _, tile -> tile.key }) { i, tile ->
                    KidsCard(AppWidth, AppHeight, tile.app.label, { onApp(tile.app) }, { onFocused(tile.app.label, label, false) }, onMoved,
                        if (i == 0) Modifier.focusRequester(start) else Modifier, place = i) { AppArt(tile.app, art) }
                }
                is HomeRowUi.WatchNext -> Unit
            }
        }
    }
}

@Composable
private fun rowLook(row: HomeRowUi): Pair<ImageVector, String> = when (row) {
    is HomeRowUi.Apps -> StovaIcons.Apps to stringResource(R.string.kids_row_apps)
    is HomeRowUi.Media -> when (row.kind) {
        MediaRowKind.CONTINUE -> StovaIcons.Play to stringResource(R.string.row_continue)
        MediaRowKind.FAVORITES -> StovaIcons.Star to stringResource(R.string.row_favorites)
        MediaRowKind.MOST_PLAYED -> StovaIcons.Sun to stringResource(R.string.row_most_played)
        MediaRowKind.LATEST_MOVIES -> StovaIcons.Image to stringResource(R.string.kids_row_movies)
        MediaRowKind.LATEST_SERIES -> StovaIcons.Tv to stringResource(R.string.kids_row_series)
        MediaRowKind.CHRISTMAS -> StovaIcons.Snowflake to stringResource(R.string.theme_christmas)
        MediaRowKind.HALLOWEEN -> StovaIcons.Pumpkin to stringResource(R.string.theme_halloween)
        else -> StovaIcons.Star to app.stova.home.rowLabel(row.key)
    }
    is HomeRowUi.WatchNext -> StovaIcons.Play to ""
}

/** A card the size of a child's attention: it grows and gets a white frame in focus, and tells Pip where it is. */
@Composable
private fun KidsCard(width: Dp, height: Dp, title: String, onClick: () -> Unit, onFocused: () -> Unit, onMoved: (Rect) -> Unit,
    modifier: Modifier, place: Int = 0, content: @Composable BoxScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    // A card in focus that changes place (back from Spole, the title just watched goes first) is brought
    // back into sight, and Pip with it; the row keeps its scroll by the first card it shows otherwise.
    val sight = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val density = LocalDensity.current
    LaunchedEffect(place) {
        if (!focused) return@LaunchedEffect
        // With the TV's safe edge either side, as the row's own padding leaves it.
        val edge = with(density) { StovaLayout.TvSafeEdge.toPx() }
        sight.bringIntoView(Rect(-edge, 0f, with(density) { width.toPx() } + edge, with(density) { height.toPx() }))
    }
    val scale by animateFloatAsState(if (focused) 1.09f else 1f, spring(dampingRatio = 0.6f, stiffness = 380f), label = "card")
    val source = remember { MutableInteractionSource() }
    Box(modifier.size(width, height)
        .bringIntoViewRequester(sight)
        .onFocusChanged { focused = it.isFocused }
        .onGloballyPositioned { if (focused) onMoved(scaledBounds(it.boundsInRoot(), 1.09f)) }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = title }) {
        LaunchedEffect(focused) { if (focused) onFocused() }
        Box(Modifier.fillMaxSize().clip(CardShape).background(KidsColors.CardBack), content = content)
        if (focused) Box(Modifier.fillMaxSize().border(5.dp, KidsColors.Focus, CardShape))
    }
}

private fun scaledBounds(bounds: Rect, scale: Float): Rect {
    val c = bounds.center
    return Rect(c.x - bounds.width * scale / 2, c.y - bounds.height * scale / 2, c.x + bounds.width * scale / 2, c.y + bounds.height * scale / 2)
}

@Composable
private fun BoxScope.Poster(item: MediaItem) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val url = item.art.poster ?: item.art.wide
    val request = remember(url) {
        ImageRequest.Builder(context).data(url).size(with(density) { PosterWidth.roundToPx() }, with(density) { PosterHeight.roundToPx() }).build()
    }
    if (url != null) AsyncImage(request, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else Text(item.title, Modifier.align(Alignment.Center).padding(12.dp), style = MaterialTheme.typography.titleMedium, color = KidsColors.LabelText)
    // New and not yet seen: a yellow star tag in the corner.
    if (item.isNew) Row(Modifier.align(Alignment.TopStart).padding(10.dp).background(KidsColors.NewBadge, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(StovaIcons.Star, null, Modifier.size(14.dp), tint = KidsColors.NewBadgeText)
        Text(stringResource(R.string.kids_new), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = KidsColors.NewBadgeText)
    }
    item.progress?.takeIf { it > 0.02f }?.let { progress ->
        Box(Modifier.align(Alignment.BottomStart).padding(10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(KidsColors.Label)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(KidsColors.Progress))
        }
    }
}

@Composable
private fun BoxScope.AppArt(app: InstalledApp, art: AppArtCache?) {
    val banner by produceState<ImageBitmap?>(null, app.key, art) { value = art?.banner(app) ?: art?.icon(app) }
    banner?.let { androidx.compose.foundation.Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        ?: Text(app.label, Modifier.align(Alignment.Center).padding(12.dp), style = MaterialTheme.typography.titleLarge, color = KidsColors.LabelText)
}
