package app.stova.kids

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.stova.R
import app.stova.apps.AppArtCache
import app.stova.apps.AppCatalog
import app.stova.home.FocusedTarget
import app.stova.home.HomeViewModel
import app.stova.media.HandoffResult
import app.stova.media.SpoleAction
import app.stova.media.SpoleHandoff
import app.stova.media.model.ItemType
import app.stova.media.ui.SpoleMissingDialog
import app.stova.profiles.Profile
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * A child's Home, Pip's world: the same rows as Home, refreshed the same way, with the clock
 * ticking each minute for the sky and bedtime. A film or an episode plays at once in Spole; a
 * series opens there so the child can pick the episode.
 */
@Composable
fun KidsHomeScreen(
    vm: HomeViewModel,
    profile: Profile,
    art: AppArtCache,
    catalog: AppCatalog,
    onRefresh: suspend () -> Unit,
    onPlaybackOpened: () -> Unit,
    onProfile: () -> Unit,
    world: Boolean = true,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { refresh(); delay(300_000) } }
    }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = LocalTime.now(); delay((60 - now.second).coerceIn(1, 60) * 1_000L) }
        }
    }
    val rows by vm.rows.collectAsStateWithLifecycle()
    // With the films' artwork behind the rows, it turns over as it does on a grown-up's Home.
    val still = app.stova.core.theme.LocalReduceMotion.current
    LaunchedEffect(world, still) { vm.setSpotlightRotation(!world && !still) }
    val context = LocalContext.current
    val handoff = remember(context) { SpoleHandoff(context) }
    var missing by remember { mutableStateOf(false) }
    fun open(target: FocusedTarget.Media) {
        val action = if (target.item.type == ItemType.SERIES) SpoleAction.DETAILS else SpoleAction.PLAY
        when (handoff.open(target.account, target.item, action)) {
            HandoffResult.Opened, HandoffResult.OpenedWithoutLink -> onPlaybackOpened()
            HandoffResult.SpoleMissing -> missing = true
            HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
        }
    }
    val voice = rememberPipVoice(profile.pip.readAloud)
    // Debug builds can try a season or an hour: files/debug-kids-season ("CHRISTMAS") and debug-kids-time ("13:00").
    val (season, hour) = remember(context) {
        if (!app.stova.BuildConfig.DEBUG) null to null else {
            fun read(name: String) = java.io.File(context.filesDir, name).takeIf { it.exists() }?.readText()?.trim()
            read("debug-kids-season")?.let { runCatching { KidsSeason.valueOf(it) }.getOrNull() } to
                read("debug-kids-time")?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        }
    }
    KidsHomeContent(profile, rows, art, app.stova.core.theme.LocalSeasonDate.current, hour ?: now,
        onPlay = ::open, onApp = { catalog.launch(it) }, onProfile = onProfile, onSpeak = voice, world = world, seasonOverride = season)
    if (missing) SpoleMissingDialog { missing = false }
}
