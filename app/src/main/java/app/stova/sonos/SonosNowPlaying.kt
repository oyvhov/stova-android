package app.stova.sonos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.theme.StovaMotion
import app.stova.core.ui.*
import app.stova.navigation.rememberPageAnchor
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Two independent D-pad targets: the music opens its page; the small button controls playback. */
@Composable fun SonosNowPlaying(room: SonosRoom, player: SonosRoom, busy: Boolean, failed: Boolean,
    onOpen: () -> Unit, onPlayPause: () -> Unit, modifier: Modifier = Modifier,
    artwork: @Composable (String, Modifier) -> Unit = { _, _ -> },
    loadArtwork: SonosArtLoader? = null) {
    val source = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val tv = room.tvAudio || player.tvAudio
    if (tv) return
    val shape = MaterialTheme.shapes.medium
    val anchor = rememberPageAnchor("sonos-widget")
    val title = player.title ?: stringResource(R.string.sonos_playing)
    val subtitle = if (failed) stringResource(R.string.sonos_action_failed) else
        listOfNotNull(room.displayName, if (!player.playing) stringResource(R.string.sonos_paused) else player.artist).joinToString(" · ")
    val colors = MaterialTheme.colorScheme
    Row(modifier.widthIn(max = 360.dp).background(colors.surface.copy(alpha = .72f), shape)
        .border(1.dp, colors.outline.copy(alpha = .35f), shape), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).then(anchor.modifier).focusOutline(source, shape).clickable(source, null, role = Role.Button, onClick = { anchor.open(onOpen) })
            .padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(34.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(StovaIcons.Play, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                key(room.id, player.artwork == null) {
                    SonosArtworkFade(player.artwork, backdrop = false, load = loadArtwork ?: { path, artModifier, ready, _ ->
                        artwork(path, artModifier)
                        SideEffect { ready() }
                    })
                }
            }
            SonosLabels(title, subtitle, MaterialTheme.typography.titleSmall, Modifier.weight(1f),
                detailColor = if (failed) colors.error else colors.onSurfaceVariant)
            if (player.playing) SonosPlayingBars()
        }
        // Busy keeps the button focused and enabled; a second press while HA answers does nothing.
        Box(Modifier.size(44.dp).focusOutline(playSource, shape).clickable(playSource, null,
            enabled = room.available && player.available && player.supports(if (player.playing) 1 else 16384),
            role = Role.Button) { if (!busy) onPlayPause() }, contentAlignment = Alignment.Center) {
            Icon(if (player.playing) StovaIcons.Pause else StovaIcons.Play,
                stringResource(if (player.playing) R.string.ha_pause else R.string.ha_play), Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** A playback indicator with no audio analysis. Its clock rests when the widget scrolls away. */
@Composable
private fun SonosPlayingBars() {
    var visible by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) }
    val color = MaterialTheme.colorScheme.onSurface
    val still = stovaMotion.still
    LifecycleResumeEffect(Unit) { resumed = true; onPauseOrDispose { resumed = false } }
    LaunchedEffect(visible, resumed, still) {
        if (visible && resumed && !still) while (true) {
            delay(StovaMotion.SpeakerFrameMillis)
            phase = (phase + 1) % 36
        }
    }
    Canvas(Modifier.size(23.dp, 18.dp).onVisibilityChanged { visible = it }) {
        val width = size.width / 7
        repeat(4) { index ->
            val wave = sin(phase * (2.0 * Math.PI / 36) + index * 1.7).toFloat()
            val height = size.height * (.57f + wave * .32f)
            drawRoundRect(color, Offset(width * index * 2, (size.height - height) / 2), Size(width, height), CornerRadius(width / 2))
        }
    }
}
