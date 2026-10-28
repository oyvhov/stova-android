package app.stova.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleOut
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.stova.profiles.season
import app.stova.profiles.ProfileGate
import app.stova.profiles.leavingNeedsAdultPin
import app.stova.settings.SettingsScreen

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
    val pictures by remember(container) { container.mediaAccounts.pictures }.collectAsStateWithLifecycle(initialValue = emptyMap())
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
    var current by rememberSaveable { mutableStateOf(Destination.HOME) }
    var smartHomeCardId by remember(profile.id) {mutableStateOf<String?>(null)}
    val file by container.profiles.file.collectAsStateWithLifecycle(initialValue = null)
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    var verifiedAdultId by remember { mutableStateOf<String?>(null) }
    var askAdultPin by remember { mutableStateOf(false) }

    // Apps opened from Settings returns there; everything else walks home, and stops there.
    var appsFromSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = current != Destination.HOME) {
        current = if (current == Destination.APPS && appsFromSettings) Destination.SETTINGS else Destination.HOME
        appsFromSettings = false
    }
    LaunchedEffect(container) { container.homeRequests.collect { appsFromSettings = false; current = Destination.HOME } }
    val switchProfile = {
        val profiles = file
        if (leavingNeedsAdultPin(profiles, profile)) {
            askAdultPin = true
        } else {
            container.session.leave()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val home: HomeViewModel = viewModel { HomeViewModel(container.session, container.homeSources) { container.media.spotlight(it.id, it.appearance.season(), it.appearance.spotlightSource,it.appearance.backgroundFilter) } }
    val date = app.stova.core.theme.LocalSeasonDate.current
    LaunchedEffect(home, date) { home.setCalendarDate(date) }
    // Kids time switches the hero on the hour; nothing ticks when it is off or the app is stopped.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(home, profile.appearance.kidsHours.enabled, lifecycle) {
        if (!profile.appearance.kidsHours.enabled) return@LaunchedEffect
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
    // Where the next page grows from: the top bar icon that opened it, or the middle of the screen.
    var origin by remember { mutableStateOf(TransformOrigin.Center) }
    val motion = stovaMotion
    // No side rail: home is the whole screen, and Apps and Settings are pages that Back closes.
    Box(Modifier.fillMaxSize().testTag("screen-${current.name}")) {
        // A short fade between pages, the arriving one settling from 0.98. Sizes never animate: every
        // page fills the screen. The leaving page cannot take focus while it fades, so a stray press
        // never lands on it, and the arriving page places focus as it always did.
        AnimatedContent(
            current,
            transitionSpec = {
                // A page opened from an icon grows out of it and shrinks back into it on Back;
                // Home itself only fades, so the picture behind never jumps.
                val home = targetState == Destination.HOME
                ContentTransform(
                    fadeIn(motion.enter()) + if (home) scaleIn(motion.enter(), initialScale = PAGE_FROM)
                        else scaleIn(motion.enter(), initialScale = PAGE_GROW_FROM, transformOrigin = origin),
                    fadeOut(motion.exit()) + if (home) scaleOut(motion.exit(), targetScale = PAGE_SHRINK_TO, transformOrigin = origin)
                        else ExitTransition.None,
                    sizeTransform = null,
                )
            },
            label = "page",
        ) { destination ->
            val leaving = transition.targetState != EnterExitState.Visible
            Box(Modifier.fillMaxSize().then(if (leaving) Modifier.focusProperties { canFocus = false } else Modifier)) {
                // Only the pages that show the backdrop take its tone.
                val toned = destination == Destination.HOME || destination == Destination.SMART_HOME
                androidx.compose.runtime.CompositionLocalProvider(app.stova.core.theme.LocalArtTone provides artTone.takeIf { toned }) {
                when (destination) {
                    Destination.HOME -> {
                        // Collected only on Home: the spotlight's rotation ticks while Home is on screen, nowhere else.
                        val spotlight by home.spotlight.collectAsStateWithLifecycle()
                        val fixedFlow = remember(profile.id, profile.appearance.fixedBackground, profile.appearance.backgroundSource,profile.appearance.backgroundFilter) {
                            if (profile.appearance.backgroundSource == app.stova.profiles.BackgroundSource.FIXED)
                                container.media.fixedBackground(profile.id, profile.appearance.fixedBackground,profile.appearance.backgroundFilter)
                            else kotlinx.coroutines.flow.flowOf(null)
                        }
                        val fixed by fixedFlow.collectAsStateWithLifecycle(initialValue = null)
                        val shift = remember { app.stova.home.HomeScrollShift() }
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
                        }
                        androidx.compose.runtime.CompositionLocalProvider(app.stova.home.LocalHomeScrollShift provides shift,
                            app.stova.home.LocalHomeArrival provides arrival) {
                        HomeScreen(
                            home, container.appArt, container.appCatalog,
                            onOpenApps = { current = Destination.APPS },
                            profile = profile, apps = container.apps,
                            onHomeChange = { prefs -> scope.launch { container.profiles.update(profile.id) { it.copy(home = prefs) } } },
                            onRefresh = { container.media.refresh(profile.id) },
                            onPlaybackOpened = { container.media.playbackOpened(profile.id) },
                            haRepository = container.haRepository,
                            haConnection = container.haSocket,
                            topBar = { chips ->
                                TopBar(
                                    profile = profile,
                                    onSettings = { current = Destination.SETTINGS },
                                    onPageOrigin = { origin = it },
                                    onProfile = switchProfile,
                                    onSmartHome = if(household?.haBaseUrl!=null) {{smartHomeCardId=null;current=Destination.SMART_HOME}} else null,
                                    // Android's own settings are for adults only, as Settings is.
                                    systemMenu = if (profile.kid) null else rememberSystemMenu(),
                                    leading = {
                                        val openCard: (String) -> Unit = {id->smartHomeCardId=id;origin=TransformOrigin.Center;current=Destination.SMART_HOME}
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
                    Destination.APPS -> AppsScreen(container.apps, profile, container.appArt, container.appCatalog, container.shortcuts, container.haSocket)
                    Destination.SMART_HOME -> app.stova.ha.ui.SmartHomeScreen(container,profile,initialCardId=smartHomeCardId,backdrop=backdrop.shown)
                    Destination.SETTINGS -> SettingsScreen(container, profile, home, switchProfile,
                        onOpenApps = { appsFromSettings = true; origin = TransformOrigin.Center; current = Destination.APPS })
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
