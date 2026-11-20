package app.stova.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleOut
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.stova.AppContainer
import app.stova.R
import app.stova.apps.AppsScreen
import app.stova.home.HomeScreen
import app.stova.home.HomeViewModel
import app.stova.core.theme.StovaTheme
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.PinDialog
import app.stova.profiles.Profile
import app.stova.profiles.hasTimedRows
import app.stova.profiles.season
import app.stova.profiles.ProfileGate
import app.stova.profiles.leavingNeedsAdultPin
import app.stova.settings.SettingsScreen
import app.stova.sonos.*

/** A page arrives from this scale: just enough to feel like it settles, never like it grows. */
private const val PAGE_FROM = 0.98f
private const val PAGE_GROW_FROM = 0.9f
private const val PAGE_SHRINK_TO = 0.94f

/** The root: the profile gate when nobody is signed in, otherwise the full-screen destination. */
@Composable
fun StovaApp(container: AppContainer) {
    val profile by container.session.active.collectAsStateWithLifecycle()
    val active = profile
    val date = app.stova.core.theme.currentSeasonDate()
    val pictures by remember(container) { container.profilePictures }.collectAsStateWithLifecycle(initialValue = emptyMap())
    StovaTheme(
        reduceMotion = active?.appearance?.reduceMotion ?: false,
        appearance = active?.appearance ?: app.stova.profiles.AppearancePrefs(),
        date = date,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(app.stova.profiles.LocalProfilePictures provides pictures) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (active == null) {
                Box(Modifier.fillMaxSize().testTag("screen-gate")) { ProfileGate(container.session, container.profiles) }
            } else {
                MainShell(container, active)
            }
        }
        }
    }
}

@Composable
private fun MainShell(container: AppContainer, profile: Profile) {
    val scope = rememberCoroutineScope()
    var trail by rememberSaveable(stateSaver = PageTrailSaver) { mutableStateOf(PageTrail()) }
    val current = trail.current.destination
    val pageOpen = remember { PageOpenContext() }
    val heldPages = rememberSaveableStateHolder()
    var sonosEntryRoom by remember(profile.id) { mutableStateOf<String?>(null) }
    var settingsEntry by remember(profile.id) { mutableStateOf(app.stova.settings.SettingsSection.APPEARANCE) }
    fun openPage(destination: Destination, roomId: String? = null) {
        if (destination == Destination.SONOS) sonosEntryRoom = roomId
        trail = trail.open(destination, pageOpen.opening)
    }
    var smartHomeCardId by remember(profile.id) {mutableStateOf<String?>(null)}
    // The title a Discover card opened the page at; null opens the page at its rows.
    var discoverStart by remember(profile.id) { mutableStateOf<app.stova.discover.DiscoverTitle?>(null) }
    val file by container.profiles.file.collectAsStateWithLifecycle(initialValue = null)
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    var verifiedAdultId by remember { mutableStateOf<String?>(null) }
    var askAdultPin by remember { mutableStateOf(false) }

    val sonosEnabled = household?.sonos?.enabled == true && household?.haBaseUrl != null && !profile.kid
    val back: () -> Unit = { trail = trail.back() }
    BackHandler(enabled = current != Destination.HOME, onBack = back)
    var homeToTop by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(container) { container.homeRequests.collect { inFront ->
        // Home pressed while Home is on screen goes back to its top.
        if (inFront && trail.current.destination == Destination.HOME) homeToTop++
        trail = trail.home()
    } }
    val switchProfile = {
        val profiles = file
        if (leavingNeedsAdultPin(profiles, profile)) {
            askAdultPin = true
        } else {
            container.session.leave()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    // A child's profile gets Pip's world. Debug builds can show it for any profile, so the
    // emulator gets real posters without a child's own account (see docs/EMULATOR.md).
    val debugPipWorld = remember(context) { app.stova.BuildConfig.DEBUG && java.io.File(context.filesDir, "debug-kids-home").exists() }
    val pipWorld = profile.kid || debugPipWorld
    val home: HomeViewModel = viewModel { HomeViewModel(container.session, container.homeSources) {
        container.spoleRows.spotlight(it, container.media.spotlight(it.id, it.appearance.season(), it.appearance.spotlightSource,it.appearance.backgroundFilter))
    } }
    val date = app.stova.core.theme.LocalSeasonDate.current
    LaunchedEffect(home, date) { home.setCalendarDate(date) }
    // Share the hour with the hero and the adult Home row order. Refresh immediately on return.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val needsHour = profile.appearance.kidsHours.enabled ||
        (profile.hasTimedRows && (current == Destination.HOME || current == Destination.SETTINGS))
    LaunchedEffect(home, needsHour, lifecycle) {
        if (!needsHour) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                val now = java.time.LocalTime.now()
                home.setHour(now.hour)
                kotlinx.coroutines.delay(((60 - now.minute) * 60_000L - now.second * 1_000L).coerceAtLeast(1_000L))
            }
        }
    }
    // Kept here, not in the layer: a return to Home shows the same picture at once, not a fade from ink.
    val backdrop = app.stova.background.rememberProfileBackdropState(profile.id,profile.appearance)
    val motion0 = stovaMotion
    val arrival = remember(profile.id) { app.stova.home.HomeArrival() }
    // The backdrop's tone, eased like the picture itself; drawn by the scrim, focus and app tiles.
    val toneTarget = backdrop.tone
    var lastTone by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Gray) }
    if (toneTarget != null) lastTone = toneTarget
    val toneColor = androidx.compose.animation.animateColorAsState(lastTone, motion0.fade(motion0.ambient), label = "art-tone")
    val toneStrength = androidx.compose.animation.core.animateFloatAsState(if (toneTarget != null) 1f else 0f,
        motion0.fade(motion0.ambient), label = "art-tone-strength")
    val artTone = remember { app.stova.core.theme.ArtToneState(toneColor, toneStrength) }
    val motion = stovaMotion
    val sonosPrefs = household?.sonos ?: SonosPrefs()
    val address = household?.haBaseUrl
    var sonosRetry by remember(profile.id) { androidx.compose.runtime.mutableIntStateOf(0) }
    val sonos = rememberSonosSession(container, profile, sonosPrefs, address,
        enabled = sonosEnabled && (current == Destination.SONOS ||
            current == Destination.HOME && sonosHomeWanted(sonosPrefs, profile, address, sonosCardFavorite(profile))),
        retry = sonosRetry)
    val widgetRoom = rememberSonosWidgetRoom(sonosPrefs, sonos.state)
    val widgetPlayer = widgetRoom?.let { sonosPlaybackRoom(it, sonos.state) }
    val musicImages = address?.let { remember(it) { app.stova.ha.cards.HaImageSource(it, container.haAuth, container.haImages) } }
    val covers = remember(profile.id, musicImages, sonosEnabled) { SonosCovers() }
    // No side rail: home is the whole screen, and Apps and Settings are pages that Back closes.
    SharedTransitionLayout(Modifier.fillMaxSize().testTag("screen-${current.name}")) {
        // A short fade between pages, the arriving one settling from 0.98. Sizes never animate: every
        // page fills the screen. The leaving page cannot take focus while it fades, so a stray press
        // never lands on it, and the arriving page places focus as it always did.
        val shared = this
        AnimatedContent(
            trail.current,
            contentKey = { it.destination },
            transitionSpec = {
                // A page opened from an icon grows out of it and shrinks back into it on Back;
                // Home itself only fades, so the picture behind never jumps.
                val returning = targetState.depth < initialState.depth
                val widget = (if (returning) initialState else targetState).opening.focusKey == SONOS_WIDGET_FOCUS
                ContentTransform(
                    if (motion.still) EnterTransition.None else fadeIn(motion.enter()) + if (widget) EnterTransition.None else if (returning) scaleIn(motion.enter(), initialScale = PAGE_FROM)
                        else scaleIn(motion.enter(), initialScale = PAGE_GROW_FROM, transformOrigin = targetState.opening.origin),
                    if (motion.still) ExitTransition.None else fadeOut(motion.exit()) + if (returning && !widget)
                        scaleOut(motion.exit(), targetScale = PAGE_SHRINK_TO, transformOrigin = initialState.opening.origin)
                        else ExitTransition.None,
                    sizeTransform = null,
                )
            },
            label = "page",
        ) { entry ->
            val visibility = this
            val destination = entry.destination
            val leaving = entry != trail.current
            Box(Modifier.fillMaxSize().then(if (leaving) Modifier.focusProperties { canFocus = false } else Modifier)) {
                // Only the pages that show the backdrop take its tone.
                val toned = destination == Destination.HOME || destination == Destination.SMART_HOME
                androidx.compose.runtime.CompositionLocalProvider(app.stova.core.theme.LocalArtTone provides artTone.takeIf { toned },
                    LocalSonosCovers provides covers,
                    LocalSonosCoverModifier provides { path -> shared.sonosCoverMotion(path, visibility) },
                    LocalPageOpenContext provides pageOpen,
                    LocalPageReturnFocus provides trail.returnFocus.takeUnless { leaving }) {
                when (destination) {
                    Destination.HOME -> if (pipWorld) {
                        // A child's Home: Pip's world, or the films' own artwork behind the same rows.
                        val art = profile.pip.background == app.stova.kids.KidsBackground.ART
                        if (art) {
                            val spotlight by home.spotlight.collectAsStateWithLifecycle()
                            app.stova.background.ProfileBackdrop(profile.id, profile.appearance, spotlight, null, state = backdrop)
                        }
                        app.stova.kids.KidsHomeScreen(home, profile, container.appArt, container.appCatalog,
                            onRefresh = { container.spoleRows.refresh(profile.id); container.media.refresh(profile.id) },
                            onPlaybackOpened = { container.media.playbackOpened(profile.id) },
                            onProfile = switchProfile, world = !art, toTop = homeToTop)
                    } else {
                        // Collected only on Home: the spotlight's rotation ticks while Home is on screen, nowhere else.
                        val spotlight by home.spotlight.collectAsStateWithLifecycle()
                        val fixedFlow = remember(profile.id, profile.appearance.fixedBackground, profile.appearance.backgroundSource,profile.appearance.backgroundFilter) {
                            if (profile.appearance.backgroundSource == app.stova.profiles.BackgroundSource.FIXED)
                                container.media.fixedBackground(profile.id, profile.appearance.fixedBackground,profile.appearance.backgroundFilter)
                            else kotlinx.coroutines.flow.flowOf(null)
                        }
                        val fixed by fixedFlow.collectAsStateWithLifecycle(initialValue = null)
                        val shift = remember { app.stova.home.HomeScrollShift() }
                        val backdropRoom = sonosHomeRoom(sonosPrefs, profile, sonos.state.rooms, sonos.state.musicAssistant)
                        val music = backdropRoom?.let { widgetPlayer?.takeIf { it.playing && !it.artwork.isNullOrBlank() } ?: it }
                        var musicArtReady by remember(music?.artwork, profile.id) { mutableStateOf(covers[music?.artwork] != null) }
                        val still = app.stova.core.theme.LocalReduceMotion.current
                        val ink = androidx.compose.material3.MaterialTheme.colorScheme.background
                        // As the rows take the screen the picture drifts up a little slower than them, grows
                        // slightly and darkens for the text. Layer and draw phases only; still under reduced motion.
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            val p = if (still) 0f else shift.progress()
                            scaleX = 1f + 0.1f * p; scaleY = scaleX
                            translationY = -size.height * 0.04f * p
                        }.drawWithContent {
                            drawContent()
                            drawRect(ink.copy(alpha = 0.35f * shift.progress()))
                        }) {
                            app.stova.background.ProfileBackdrop(profile.id,profile.appearance,spotlight,fixed,state=backdrop)
                            app.stova.sonos.SonosHomeBackdrop(music, musicImages) { musicArtReady = it }
                        }
                        // The Sonos favourite card reads the same session as the widget and backdrop; pictures only through HaImage.
                        val sonosCard = if (!app.stova.sonos.sonosCardFavorite(profile)) null else app.stova.sonos.SonosCardSource(
                            app.stova.sonos.sonosCard(sonosPrefs, sonos.state)) { path, modifier, onLoad, onError ->
                            app.stova.ha.cards.HaImage(path, musicImages, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                onLoad = onLoad, onError = onError)
                        }
                        androidx.compose.runtime.CompositionLocalProvider(app.stova.home.LocalHomeScrollShift provides shift,
                            app.stova.sonos.LocalSonosCard provides sonosCard,
                            LocalHomeSonos provides music.takeIf { musicArtReady },
                            LocalHomeSonosWidget provides (widgetRoom != null && widgetPlayer != null),
                            app.stova.home.LocalHomeArrival provides arrival) {
                        HomeScreen(
                            home, container.appArt, container.appCatalog,
                            onOpenApps = { openPage(Destination.APPS) },
                            profile = profile, apps = container.apps,
                            onHomeChange = { prefs -> scope.launch { container.profiles.update(profile.id) { it.copy(home = prefs) } } },
                            onRefresh = {
                                container.spoleRows.refresh(profile.id)
                                // Beside the media rows and never holding them up: a slow Spole keeps the refresh's pace. In the
                                // app's scope; the repository asks Spole one question at a time per profile, every ten minutes.
                                container.appScope.launch {
                                    container.discover.identity(profile, container.mediaAccounts.forProfile(profile.id).first())
                                        ?.let { container.discover.refresh(profile.id, it) }
                                }
                                container.media.refresh(profile.id)
                            },
                            onPlaybackOpened = { container.media.playbackOpened(profile.id) },
                            onOpenSonos = { if (sonosEnabled) openPage(Destination.SONOS) },
                            onItemChanged = {
                                container.spoleRows.refresh(profile.id, force = true)
                                scope.launch { container.media.refresh(profile.id, 0) }
                            },
                            haRepository = container.haRepository,
                            haConnection = container.haSocket,
                            // From a row, not a top bar icon: the page grows from the middle.
                            onOpenDiscover = { discoverStart = null; openPage(Destination.DISCOVER) },
                            onOpenDiscoverTitle = { discoverStart = it; openPage(Destination.DISCOVER) },
                            toTop = homeToTop,
                            aboveApps = if (widgetRoom == null || widgetPlayer == null) null else { modifier ->
                                app.stova.sonos.SonosNowPlaying(widgetRoom, widgetPlayer, sonos.busy, sonos.failed,
                                    onOpen = {
                                        scope.launch { container.household.update { it.copy(sonos = it.sonos.copy(preferredRoom = widgetRoom.id)) } }
                                        openPage(Destination.SONOS, widgetRoom.id)
                                    }, onPlayPause = { sonos.playPause(widgetRoom.id) }, modifier = modifier,
                                    loadArtwork = { path, artModifier, ready, failed ->
                                        SonosCoverImage(path, musicImages, artModifier, ready, failed)
                                    })
                            },
                            topBar = { chips ->
                                TopBar(
                                    profile = profile,
                                    onSettings = { openPage(Destination.SETTINGS) },
                                    onProfile = switchProfile,
                                    onSmartHome = if(household?.haBaseUrl!=null) {{smartHomeCardId=null;openPage(Destination.SMART_HOME)}} else null,
                                    onSonos = if (sonosEnabled && household?.sonos?.showHomeButton == true) ({ openPage(Destination.SONOS) }) else null,
                                    // Android's own settings are for adults only, as Settings is.
                                    systemMenu = if (profile.kid) null else rememberSystemMenu(),
                                    leading = {
                                        val openCard: (String) -> Unit = {id->smartHomeCardId=id;openPage(Destination.SMART_HOME)}
                                        app.stova.ha.ui.HomeStatusChips(chips,onOpenCard=openCard) { chip, close ->
                                            // The app's scope, so a change made just before Back still reaches the house.
                                            app.stova.ha.ui.ChipDropdownHost(chip, container.haRepository, profile, container.appScope, onDismiss = close)
                                        }
                                    },
                                )
                            },
                        )
                        }
                    }
                    Destination.APPS -> heldPages.SaveableStateProvider(Destination.APPS.name) {
                        // Only what the person may do reaches the page: Spole being asked again leaves the grid alone.
                        val discover = remember(profile.id) { container.discover.state(profile.id) }.collectAsStateWithLifecycle()
                        val discoverAccess by remember(discover) { derivedStateOf { discover.value?.access ?: app.stova.discover.SeerrAccess.NONE } }
                        AppsScreen(container.apps, profile, container.appArt, container.appCatalog, container.shortcuts, container.haSocket,
                            discoverAccess = discoverAccess,
                            onOpenSonos = if (sonosEnabled) ({ openPage(Destination.SONOS) }) else null,
                            // From a tile in the grid, as from Home's row: the page grows from the middle, and Back comes back here.
                            onOpenDiscover = { discoverStart = null; openPage(Destination.DISCOVER) })
                    }
                    Destination.SMART_HOME -> app.stova.ha.ui.SmartHomeScreen(container,profile,initialCardId=smartHomeCardId,backdrop=backdrop.shown,
                        onConnectionSettings={settingsEntry=app.stova.settings.SettingsSection.SMART_HOME;openPage(Destination.SETTINGS)})
                    Destination.SETTINGS -> SettingsScreen(container, profile, home, switchProfile, initialSection=settingsEntry,
                        onOpenSonos = { openPage(Destination.SONOS) },
                        onOpenApps = { openPage(Destination.APPS) })
                    // Opened at the title a card showed, or at the rows; Back walks the page's own steps, then to where the
                    // page was opened from. With nothing to show it leaves the same way, but only while it is the page shown.
                    Destination.DISCOVER -> app.stova.discover.DiscoverScreen(container, profile,
                        start = discoverStart?.let { app.stova.discover.DiscoverStep.Title(it.type, it.tmdbId, it) } ?: app.stova.discover.DiscoverStep.Rows,
                        onLeave = { if (current == Destination.DISCOVER) back() })
                    Destination.SONOS -> SonosScreen(container, profile, sonos, sonosPrefs, musicImages,
                        allowed = sonosEnabled || household == null, entryRoom = sonosEntryRoom,
                        onRetry = { container.haSocket.retryConnection();sonosRetry++ }, onBack = back,
                        onConnectionSettings={settingsEntry=app.stova.settings.SettingsSection.SMART_HOME;openPage(Destination.SETTINGS)})
                }
                }
            }
        }
    }

    if (askAdultPin) {
        PinDialog(
            title = stringResource(R.string.pin_any_adult),
            onSubmit = { pin -> container.profiles.verifyAnyAdultPin(pin) { verifiedAdultId = it } },
            onAccepted = {
                askAdultPin = false
                container.session.leave(verifiedAdultId)
                verifiedAdultId = null
            },
            onDismiss = { askAdultPin = false },
        )
    }
}
