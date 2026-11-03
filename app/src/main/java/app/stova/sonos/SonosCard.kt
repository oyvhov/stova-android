package app.stova.sonos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.apps.ShortcutKey
import app.stova.core.ui.StovaIcons
import app.stova.profiles.BackgroundSource
import app.stova.profiles.Profile

enum class SonosCardStatus { PLAYING, PAUSED, BUFFERING, TV, OFFLINE, IDLE, NONE }

/** What the Sonos favourite shows. [artwork], [title] and [artist] are only ever music: never the TV, never a lost speaker. */
data class SonosCard(val room: SonosRoom?, val status: SonosCardStatus, val title: String? = null, val artist: String? = null,
    val artwork: String? = null) {
    companion object { val Empty = SonosCard(null, SonosCardStatus.NONE) }
}

/**
 * The room the card follows: the last one chosen, else one playing music, else the first that
 * answers. Its own native name and model; the music may come from its group or a linked MA queue.
 */
fun sonosCard(prefs: SonosPrefs, state: SonosState): SonosCard {
    val musicNow = { room: SonosRoom -> !room.tvAudio && sonosPlaybackRoom(room, state)?.let { it.playing && !it.tvAudio } == true }
    val room = state.rooms.find { it.id == prefs.preferredRoom } ?: state.rooms.firstOrNull(musicNow)
        ?: state.rooms.firstOrNull { it.available } ?: state.rooms.firstOrNull() ?: return SonosCard.Empty
    val native = coordinator(room, state.rooms)
    val tv = room.tvAudio || native?.tvAudio == true
    val player = if (!room.available || tv) null else sonosPlaybackRoom(room, state)
    val status = when {
        !room.available || native?.available == false -> SonosCardStatus.OFFLINE
        tv -> SonosCardStatus.TV
        player == null || !player.available -> SonosCardStatus.OFFLINE
        player.entity.state == "playing" -> SonosCardStatus.PLAYING
        player.entity.state == "paused" -> SonosCardStatus.PAUSED
        player.entity.state == "buffering" -> SonosCardStatus.BUFFERING
        else -> SonosCardStatus.IDLE
    }
    val music = status in setOf(SonosCardStatus.PLAYING, SonosCardStatus.PAUSED, SonosCardStatus.BUFFERING)
    return SonosCard(room, status, player?.title?.takeIf { music && it.isNotBlank() }, player?.artist?.takeIf { music && it.isNotBlank() },
        player?.artwork?.takeIf { music && it.isNotBlank() })
}

/** The card is a favourite the profile saved. */
fun sonosCardFavorite(profile: Profile) = ShortcutKey.Sonos.key.let { it in profile.apps.favorites && it in profile.apps.shortcuts }

/** Home's one Sonos session runs for the widget, the opt-in backdrop or a Sonos favourite; never for a kid or without HA. */
fun sonosHomeWanted(prefs: SonosPrefs, profile: Profile, address: String?, cardFavorite: Boolean) =
    prefs.enabled && address != null && !profile.kid &&
        (prefs.showNowPlaying || prefs.homeArtwork && profile.appearance.backgroundSource != BackgroundSource.THEME || cardFavorite)

/** Loads one HA picture for the card through the safe HA image loader. */
typealias SonosCardImage = @Composable (path: String, modifier: Modifier, onLoad: () -> Unit, onError: () -> Unit) -> Unit

/** The live card and how to draw its picture; null away from Home or without the favourite. */
class SonosCardSource(val card: SonosCard, val image: SonosCardImage)
val LocalSonosCard = compositionLocalOf<SonosCardSource?> { null }

/**
 * Album art edge to edge, a theme-coloured fade at the foot for the words, a quiet Sonos mark.
 * Without music, or when a picture fails, the room's own speaker stands on a calm plate.
 * The card holds no buttons: pressing it opens Sonos, so it never starts or stops anything.
 */
@Composable
fun SonosFavoriteCard(card: SonosCard, modifier: Modifier, compact: Boolean = false, image: SonosCardImage) {
    val colors = MaterialTheme.colorScheme
    val path = card.artwork
    var loaded by remember(path) { mutableStateOf(false) }
    var failed by remember(path) { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (!loaded) Box(Modifier.fillMaxSize().testTag("sonos-card-fallback")
            .background(Brush.verticalGradient(listOf(colors.surfaceVariant, colors.surface))), contentAlignment = Alignment.Center) {
            val speaker = card.room?.speaker
            if (speaker != null) SonosSpeakerArt(speaker, null, Modifier.fillMaxWidth(.5f).fillMaxHeight(.62f).align(Alignment.TopCenter)
                .padding(top = 10.dp), "sonos-card-speaker", card.status != SonosCardStatus.OFFLINE)
            else Icon(StovaIcons.Sonos, null, Modifier.size(36.dp), tint = colors.onSurfaceVariant)
        }
        if (path != null && !failed) image(path, Modifier.fillMaxSize(), { loaded = true }, { failed = true; loaded = false })
        if (!compact) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to colors.background.copy(alpha = 0f),
                .45f to colors.background.copy(alpha = 0f), 1f to colors.background.copy(alpha = .92f))))
            if (card.room != null) Icon(StovaIcons.Sonos, null, Modifier.align(Alignment.TopStart).padding(10.dp).size(18.dp), tint = colors.onSurface.copy(alpha = .8f))
            val detail = card.room?.let { room ->
                    stringResource(R.string.sonos_card_status, room.displayName, stringResource(when (card.status) {
                        SonosCardStatus.PLAYING -> R.string.sonos_card_playing
                        SonosCardStatus.PAUSED -> R.string.sonos_paused
                        SonosCardStatus.BUFFERING -> R.string.sonos_card_buffering
                        SonosCardStatus.TV -> R.string.sonos_tv
                        SonosCardStatus.OFFLINE -> R.string.sonos_unavailable
                        else -> R.string.sonos_ready
                    }))
            }
            SonosLabels(card.title ?: stringResource(R.string.sonos_title), detail, MaterialTheme.typography.titleSmall,
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp))
        }
    }
}
