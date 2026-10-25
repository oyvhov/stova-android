package app.stova.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.stova.AppContainer
import app.stova.R
import app.stova.apps.AppsScreen
import app.stova.home.HomeScreen
import app.stova.home.HomeViewModel
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.PinDialog
import app.stova.profiles.Profile
import app.stova.profiles.ProfileGate
import app.stova.profiles.leavingNeedsAdultPin
import app.stova.settings.SettingsScreen

/** The root: the profile gate when nobody is signed in, otherwise the full-screen destination. */
@Composable
fun StovaApp(container: AppContainer) {
    val profile by container.session.active.collectAsStateWithLifecycle()
    val active = profile
    StovaTheme(
        reduceMotion = active?.appearance?.reduceMotion ?: false,
        strongFocus = active?.appearance?.strongFocus ?: false,
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (active == null) {
                Box(Modifier.fillMaxSize().testTag("screen-gate")) { ProfileGate(container.session, container.profiles) }
            } else {
                MainShell(container, active)
            }
        }
    }
}

@Composable
private fun MainShell(container: AppContainer, profile: Profile) {
    var current by rememberSaveable { mutableStateOf(Destination.HOME) }
    var smartHomeCardId by remember(profile.id) {mutableStateOf<String?>(null)}
    val file by container.profiles.file.collectAsStateWithLifecycle(initialValue = null)
    val household by container.household.data.collectAsStateWithLifecycle(initialValue = null)
    var verifiedAdultId by remember { mutableStateOf<String?>(null) }
    var askAdultPin by remember { mutableStateOf(false) }

    // Back walks home, and stops there: a launcher has nowhere further back to go.
    BackHandler(enabled = current != Destination.HOME) { current = Destination.HOME }
    LaunchedEffect(container) { container.homeRequests.collect { current = Destination.HOME } }
    val switchProfile = {
        val profiles = file
        if (leavingNeedsAdultPin(profiles, profile)) {
            askAdultPin = true
        } else {
            container.session.leave()
        }
    }
    val home: HomeViewModel = viewModel { HomeViewModel(container.session, container.homeSources) { container.media.spotlight(it.id) } }
    val spotlight by home.spotlight.collectAsStateWithLifecycle()
    // No side rail: home is the whole screen, and Apps and Settings are pages that Back closes.
    Box(Modifier.fillMaxSize().testTag("screen-${current.name}")) {
        if (current == Destination.HOME) app.stova.background.BackdropLayer(spotlight?.item?.art?.backdrop)
        when (current) {
            Destination.HOME -> HomeScreen(
                home, container.appArt, container.appCatalog,
                onOpenApps = { current = Destination.APPS },
                onRefresh = { container.media.refresh(profile.id) },
                haRepository = container.haRepository,
                haConnection = container.haSocket,
                topBar = { chips ->
                    TopBar(
                        profile = profile,
                        onApps = { current = Destination.APPS },
                        onSettings = { current = Destination.SETTINGS },
                        onProfile = switchProfile,
                        onSmartHome = if(household?.haBaseUrl!=null) {{smartHomeCardId=null;current=Destination.SMART_HOME}} else null,
                        leading = { app.stova.ha.ui.HomeStatusChips(chips,onOpenCard={id->smartHomeCardId=id;current=Destination.SMART_HOME}) },
                    )
                },
            )
            Destination.APPS -> AppsScreen(container.apps, profile, container.appArt, container.appCatalog)
            Destination.SMART_HOME -> app.stova.ha.ui.SmartHomeScreen(container,profile,initialCardId=smartHomeCardId)
            Destination.SETTINGS -> SettingsScreen(container, profile, home, switchProfile)
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
