package app.stova.apps

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaConnection
import app.stova.ha.runHaShortcut
import app.stova.media.HandoffResult
import app.stova.media.SpoleHandoff
import app.stova.media.model.MediaAccount
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * What the Apps page offers besides apps. [accounts] and [haReady] are local and draw the
 * favourites; [libraries] and [scenes] ask the servers, only while the page is shown and only for
 * what is set up.
 */
class ShortcutCatalog(
    val accounts: (String) -> Flow<List<MediaAccount>> = { flowOf(emptyList()) },
    val haReady: Flow<Boolean> = flowOf(false),
    val libraries: suspend (profileId: String) -> List<FavoriteTile.Library> = { emptyList() },
    val scenes: suspend () -> List<FavoriteTile.Scene> = { emptyList() },
)

/** The name and art a shortcut keeps in the profile, so Home can draw it without asking anyone. */
fun FavoriteTile.saved(): Shortcut = Shortcut(label, (this as? FavoriteTile.Library)?.image, (this as? FavoriteTile.Scene)?.icon)

/**
 * What pressing a library or scene does, on Home and on the Apps page: a library opens in Spole, a
 * scene runs in Home Assistant, which is held only for the call.
 */
@Composable
fun rememberShortcutOpener(haConnection: HaConnection?, profile: Profile?, onPlaybackOpened: () -> Unit = {},
    onSpoleMissing: () -> Unit): (FavoriteTile) -> Unit {
    val context = LocalContext.current
    val handoff = remember(context) { SpoleHandoff(context) }
    val scope = rememberCoroutineScope()
    val opened by rememberUpdatedState(onPlaybackOpened)
    val missing by rememberUpdatedState(onSpoleMissing)
    val current by rememberUpdatedState(profile)
    // Read in composition, so a language change reaches the messages too.
    val ran by rememberUpdatedState(stringResource(R.string.shortcut_ran))
    val failed by rememberUpdatedState(stringResource(R.string.shortcut_failed))
    return remember(handoff, haConnection) { { tile ->
        when (tile) {
            is FavoriteTile.Library -> when (handoff.openLibrary(tile.account, tile.libraryId)) {
                HandoffResult.Opened -> opened()
                HandoffResult.OpenedWithoutLink -> {
                    opened()
                    Toast.makeText(context, R.string.spole_update_for_libraries, Toast.LENGTH_LONG).show()
                }
                HandoffResult.SpoleMissing -> missing()
                HandoffResult.InvalidIds -> Toast.makeText(context, R.string.invalid_media_ids, Toast.LENGTH_SHORT).show()
            }
            is FavoriteTile.Scene -> {
                val who = current
                if (haConnection != null && who != null) scope.launch {
                    val done = runHaShortcut(haConnection, tile.entityId, who)
                    Toast.makeText(context, (if (done) ran else failed).format(tile.label), Toast.LENGTH_SHORT).show()
                }
            }
            is FavoriteTile.App -> Unit
        }
    } }
}
