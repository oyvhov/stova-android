package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaIcons
import app.stova.home.SeasonalScene
import app.stova.home.rememberSeasonalClock

/**
 * The music on its own: the sleeve edge to edge (when the profile allows background art), the
 * theme's own ink fading in where words sit, title and artist, icon-only transport and the quiet
 * volume rail. The room stays named at the top so the target is never in doubt. No rooms, library,
 * grouping or settings here. TV audio and lost speakers never show album art, not even fading out.
 */
@Composable internal fun SonosImmersive(room: SonosRoom, native: SonosRoom?, player: SonosRoom?, busy: Boolean, actionFailed: Boolean,
    backgroundArt: Boolean, art: String?, load: SonosArtLoader, onAction: (SonosAction) -> Unit, onExit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.background
    val tv = room.tvAudio || native?.tvAudio == true || player?.tvAudio == true
    val playFocus = remember { FocusRequester() }
    val exitFocus = remember { FocusRequester() }
    val entry = remember { FocusRequester() }
    val canPlay = room.available && player?.available == true && player.supports(if (player.playing) 1 else 16384)
    val largeType = LocalDensity.current.fontScale >= 1.5f
    LaunchedEffect(Unit) { withFrameNanos { }; if (canPlay) playFocus.requestFocus() else exitFocus.requestFocus() }
    // SonosContent owns the shared backdrop so entering and leaving never reloads it.
    Box(Modifier.fillMaxSize().testTag("sonos-immersive")) {
        // Theme ink where the words and controls sit; the middle of the sleeve stays clear.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to ink.copy(alpha = .62f), .22f to ink.copy(alpha = .08f),
            .5f to ink.copy(alpha = .18f), 1f to ink.copy(alpha = .94f))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to ink.copy(alpha = .55f), .6f to Color.Transparent)))
        val seasonClock = rememberSeasonalClock()
        SeasonalScene(Modifier.matchParentSize(), clock = seasonClock)
        // Without full-bleed art the sleeve, or the room's own speaker, stands to the right as the
        // picture; words and controls keep the left and the foot, as with art.
        if (art != null && !backgroundArt) key(room.id, player?.id) {
            SonosCover(art, Modifier.align(Alignment.CenterEnd).padding(end = StovaLayout.TvSafeEdge)
                .size(if (largeType) 180.dp else 300.dp), load)
        }
        else if (art == null) SonosSpeakerArt(room.speaker, rememberSonosVisual(room, SonosState(listOfNotNull(room, native).distinct(), loading = false)),
            Modifier.align(Alignment.CenterEnd).padding(end = StovaLayout.TvSafeEdge).width(360.dp).height(if (largeType) 120.dp else 200.dp),
            "sonos-immersive-speaker", room.available, waves = true)
        Column(Modifier.fillMaxSize().padding(horizontal = StovaLayout.TvSafeEdge, vertical = 28.dp)
            .then(if (largeType) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(StovaIcons.Sonos, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                Text(listOfNotNull(room.name, room.speaker.label).joinToString(" · "), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                SonosSeasonMark()
                SonosIconButton(StovaIcons.Collapse, stringResource(R.string.sonos_fullscreen_exit), onExit,
                    Modifier.focusRequester(exitFocus).testTag("sonos-fullscreen-exit"))
            }
            if (!largeType) Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(.6f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SonosTrackText(room, player, tv, largeType, immersive = true)
                if (!tv && player != null && player.duration > 0) SonosProgress(player)
            }
            if (actionFailed) Text(stringResource(R.string.sonos_action_failed), color = colors.error)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                SonosTransport(room, player, onAction, entry, exitFocus, playFocus, large = true)
                Spacer(Modifier.weight(1f))
                room.volume?.let { volume ->
                    SonosVolumeRail(room.id, volume, room.available && room.supports(4), busy,
                        stringResource(R.string.sonos_volume_room, room.name), onAction, Modifier.width(340.dp))
                }
            }
        }
        SeasonalScene(Modifier.matchParentSize(), front = true, clock = seasonClock)
    }
}
