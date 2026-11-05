package app.stova.sonos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaIcons

/**
 * The music on its own: the sleeve edge to edge (when the profile allows background art), the
 * theme's own ink fading in where words sit, title and artist, icon-only transport and the quiet
 * speaker volume button, with the room named beneath it. No rooms, library,
 * grouping or settings here. TV audio and lost speakers never show album art, not even fading out.
 */
@Composable internal fun SonosImmersive(room: SonosRoom, native: SonosRoom?, player: SonosRoom?, busy: Boolean, actionFailed: Boolean,
    backgroundArt: Boolean, art: String?, load: SonosArtLoader, onAction: (SonosAction) -> Unit, onExit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tv = room.tvAudio || native?.tvAudio == true || player?.tvAudio == true
    val playFocus = remember { FocusRequester() }
    val exitFocus = remember { FocusRequester() }
    val entry = remember { FocusRequester() }
    val volumeFocus = remember { FocusRequester() }
    val canPlay = room.available && player?.available == true && player.supports(if (player.playing) 1 else 16384)
    val canAdjustVolume = room.volume != null && room.available && room.supports(4)
    val largeType = LocalDensity.current.fontScale >= 1.5f
    LaunchedEffect(Unit) { withFrameNanos { }; if (canPlay) playFocus.requestFocus() else exitFocus.requestFocus() }
    // SonosContent owns the shared backdrop so entering and leaving never reloads it.
    Box(Modifier.fillMaxSize().testTag("sonos-immersive")) {
        Column(Modifier.fillMaxSize().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 28.dp)
            .then(if (largeType) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SonosIconButton(StovaIcons.Collapse, stringResource(R.string.sonos_fullscreen_exit), onExit,
                    Modifier.focusRequester(exitFocus).testTag("sonos-fullscreen-exit"), quiet = true)
                Spacer(Modifier.weight(1f))
                SonosSeasonMark()
                SonosVolumePopup(room, room.volume, busy, onAction, volumeFocus,
                    Modifier.focusProperties { if (canPlay) down = playFocus })
            }
            if (!largeType) Spacer(Modifier.weight(1f))
            if (art != null && !backgroundArt) key(room.id, player?.id) {
                SonosCover(art, Modifier.align(Alignment.CenterHorizontally).size(if (largeType) 132.dp else 180.dp), load)
            }
            Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SonosTrackText(room, player, tv, largeType, immersive = true)
                if (!tv && player != null && player.duration > 0) {
                    SonosProgress(player, prominent = true, modifier = Modifier.fillMaxWidth(.85f))
                }
                if (actionFailed) Text(stringResource(R.string.sonos_action_failed), color = colors.error)
                SonosTransport(room, player, onAction, entry, exitFocus, playFocus, large = true,
                    volumeFocus = if (canAdjustVolume) volumeFocus else FocusRequester.Default)
            }
        }
    }
}
