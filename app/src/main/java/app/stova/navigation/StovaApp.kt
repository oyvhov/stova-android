package app.stova.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.core.theme.StovaTheme
import app.stova.profiles.Profile

/** The root: the profile gate when nobody is signed in, otherwise side rail plus destination. */
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
                Box(Modifier.fillMaxSize().testTag("screen-gate"))
            } else {
                MainShell(container, active)
            }
        }
    }
}

@Composable
private fun MainShell(container: AppContainer, profile: Profile) {
    var current by rememberSaveable { mutableStateOf(Destination.HOME) }
    // Back walks home, and stops there: a launcher has nowhere further back to go.
    BackHandler(enabled = current != Destination.HOME) { current = Destination.HOME }
    Row(Modifier.fillMaxSize()) {
        SideRail(
            destinations = Destination.entries,
            current = current,
            profile = profile,
            onSelect = { current = it },
            onProfile = { container.session.leave() },
        )
        Box(Modifier.weight(1f).fillMaxHeight().testTag("screen-${current.name}"))
    }
}
