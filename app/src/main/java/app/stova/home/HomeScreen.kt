package app.stova.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.stova.R
import app.stova.apps.*
import app.stova.core.theme.LocalReduceMotion
import app.stova.ha.HaConnection
import app.stova.ha.HaRepository
import app.stova.ha.ui.ChipContent
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
    onPlaybackOpened: () -> Unit = {},
    /** A title changed through Spole (watched, out of «Hald fram»): the rows ask again. */
    onItemChanged: () -> Unit = {},
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
    val scope = rememberCoroutineScope()
    fun open(target: FocusedTarget.Media, action: SpoleAction, fromStart: Boolean = false) {
        when (handoff.open(target.account, target.item, action, fromStart)) {
            HandoffResult.Opened -> onPlaybackOpened()
            HandoffResult.OpenedWithoutLink -> {
                onPlaybackOpened()
                Toast.makeText(context, R.string.spole_update_for_links, Toast.LENGTH_LONG).show()
            }
            HandoffResult.SpoleMissing -> missing = true
            HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
        }
    }
    // Spole does what the menu asks with its own account, so both apps agree afterwards.
    fun act(target: FocusedTarget.Media, action: app.stova.media.ui.MediaItemAction) {
        if (action == app.stova.media.ui.MediaItemAction.PLAY_FROM_START) { open(target, SpoleAction.PLAY, fromStart = true); return }
        val spole = if (action == app.stova.media.ui.MediaItemAction.MARK_WATCHED) app.stova.media.SpoleItemAction.WATCHED
            else app.stova.media.SpoleItemAction.REMOVE_FROM_CONTINUE
        scope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.stova.media.SpoleProfiles(context).itemAction(target.account.kind, target.account.userId, target.item.id, spole)
            }
            val message = when (result) {
                app.stova.media.SpoleActionResult.DONE -> {
                    onItemChanged()
                    if (spole == app.stova.media.SpoleItemAction.WATCHED) R.string.item_marked_watched else R.string.item_removed_from_continue
                }
                app.stova.media.SpoleActionResult.FAILED -> R.string.item_action_failed
                app.stova.media.SpoleActionResult.UNKNOWN_ACCOUNT -> R.string.item_action_unknown_account
                app.stova.media.SpoleActionResult.NOT_SUPPORTED -> R.string.item_action_update_spole
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val openShortcut = rememberShortcutOpener(haConnection, profile, onPlaybackOpened) { missing = true }
    HomeContent(rows, focused, initial, art, catalog, onOpenApps, vm::focus,
        onMediaClick = { open(it, SpoleAction.DETAILS) }, onMediaLongClick = { actions = it }, topBar = { HomeChips(haRepository, topBar) },
        spotlight = if(profile==null) spotlight else backgroundForProfile(spotlight,profile.id,profile.appearance.backgroundFilter), appIconOverrides = profile?.apps?.iconOverrides.orEmpty(),
        onAppLongClick = { app -> if (profile?.kid == false) appMenu = app },
        homePrefs = profile?.home ?: HomePrefs(),
        onHomeChange = onHomeChange, canEditRows = profile?.kid == false, onRowFocus = vm::focusRow,
        onMediaPlay = { open(it, SpoleAction.PLAY) },
        onMediaAction = if (profile?.kid == false) ::act else null,
        appActions = { app, rowActions, dismiss ->
            if (profile != null && apps != null) AppMenuHost(app, apps, profile, art, catalog, rowActions, dismiss)
        },
        onShortcutClick = openShortcut,
        shortcutActions = { tile, rowActions, dismiss ->
            if (profile != null && apps != null) ShortcutMenuHost(tile, apps, profile, openShortcut, rowActions, dismiss)
        })
    appMenu?.let { app -> if (profile != null && apps != null) AppMenuHost(app, apps, profile, art, catalog) { appMenu = null } }
    actions?.let { target ->
        ItemActionsMenu(target.item,
            onPlay = { actions = null; open(target, SpoleAction.PLAY) },
            onDetails = { actions = null; open(target, SpoleAction.DETAILS) },
            onDismiss = { actions = null },
            onAction = if (profile?.kid == false) { action -> actions = null; act(target, action) } else null)
    }
    if (missing) SpoleMissingDialog { missing = false }
}

