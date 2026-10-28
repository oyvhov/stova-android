package app.stova.ha.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.TopDropdown
import app.stova.ha.HaEntity
import app.stova.ha.HaRepository
import app.stova.ha.text
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The chips that drop a small control; the others open their card in Smarthus. */
internal val QuickTypes = setOf(CardType.LIGHT, CardType.MEDIA_PLAYER, CardType.CLIMATE, CardType.TOGGLE)

/**
 * What a chip's dropdown changes: [value] moves with Left and Right in [step]s within [range] and
 * is sent with [set]; [ok] is what OK does. A null [value] has nothing to slide, as a plain switch.
 */
internal class QuickControl(
    @param:StringRes val label: Int,
    val value: Double?,
    val range: ClosedFloatingPointRange<Double> = 0.0..100.0,
    val step: Double = 5.0,
    val degrees: Boolean = false,
    val ok: ServiceCall? = null,
    @param:StringRes val okLabel: Int? = null,
    val set: (Double) -> ServiceCall,
) {
    /** One press; a held key moves twice as far, except for a temperature. */
    fun nudge(from: Double, right: Boolean, held: Boolean): Double {
        val delta = step * (if (held && !degrees) 2 else 1) * (if (right) 1 else -1)
        val next = (from + delta).coerceIn(range)
        return if (degrees) round(next * 2) / 2 else next
    }
}

internal fun quickControl(type: CardType, entity: HaEntity): QuickControl? {
    val attrs = entity.attributes
    val on = entity.state == "on"
    val domain = entity.id.substringBefore('.')
    val switch = ServiceCall(domain, if (on) "turn_off" else "turn_on")
    val switchLabel = if (on) R.string.ha_action_turn_off else R.string.ha_action_turn_on
    return when (type) {
        CardType.LIGHT -> {
            val dimmable = attrs.number("brightness") != null || attrs.strings("supported_color_modes").any { it != "onoff" }
            val percent = ((attrs.number("brightness") ?: 255.0) / 255 * 100).roundToInt().coerceIn(1, 100).toDouble()
            // All the way down is off, so Right from off turns the light on dimmed.
            QuickControl(R.string.ha_brightness, if (!dimmable) null else if (on) percent else 0.0, ok = switch, okLabel = switchLabel,
                set = { if (it <= 0.0) ServiceCall("light", "turn_off") else brightnessCall(it.roundToInt()) })
        }
        CardType.MEDIA_PLAYER -> QuickControl(R.string.ha_action_volume, attrs.number("volume_level")?.let { (it * 100).roundToInt().toDouble() },
            step = 2.0, ok = ServiceCall("media_player", "media_play_pause").takeIf { entity.state in setOf("playing", "paused") },
            okLabel = if (entity.state == "playing") R.string.ha_pause else R.string.ha_play,
            set = { ServiceCall("media_player", "volume_set", buildJsonObject { put("volume_level", it / 100) }) })
        CardType.CLIMATE -> {
            val min = attrs.number("min_temp") ?: 5.0
            val max = (attrs.number("max_temp") ?: 35.0).coerceAtLeast(min)
            QuickControl(R.string.ha_target, attrs.number("temperature")?.coerceIn(min, max), min..max, 0.5, degrees = true,
                set = { ServiceCall("climate", "set_temperature", buildJsonObject { put("temperature", it) }) })
        }
        CardType.TOGGLE -> {
            val speed = domain == "fan" && ((attrs.number("supported_features") ?: 0.0).toInt() and 1) != 0
            QuickControl(R.string.ha_action_speed, if (speed) attrs.number("percentage") ?: 0.0 else null, step = 10.0, ok = switch, okLabel = switchLabel,
                set = { ServiceCall("fan", "set_percentage", buildJsonObject { put("percentage", it.roundToInt()) }) })
        }
        else -> null
    }
}

/**
 * A chip's dropdown wired to Home Assistant. The card and its entity are read only while it is
 * open, so Home Assistant's events never reach the top bar otherwise. [sendScope] outlives it, so
 * a change made just before Back still reaches the house.
 */
@Composable
fun ChipDropdownHost(chip: ChipContent, repository: HaRepository, profile: Profile, sendScope: CoroutineScope, onDismiss: () -> Unit) {
    val cards by repository.homeCards.collectAsStateWithLifecycle()
    val entities by remember(repository) { repository.entities }.collectAsStateWithLifecycle(initialValue = null)
    val live = entities ?: return
    val config = cards.firstOrNull { it.id == chip.cardId }
    val entity = config?.let { live[it.entityId] }
    TopDropdown(onDismiss, DropdownWidth) {
        ChipDropdown(chip.icon, config?.name ?: entity?.let(::friendlyName).orEmpty(), entity, canControl(profile),
            send = { call -> config != null && repository.perform(call, config.entityId, profile) }, sendScope = sendScope)
    }
}

/**
 * Inside the dropdown: the name and state, the value large with a slim bar Left and Right move,
 * and what OK does. Smarthus has its own button beside the clock. A kid without control only reads it.
 */
@Composable
internal fun ChipDropdown(type: CardType, name: String, entity: HaEntity?, enabled: Boolean, send: suspend (ServiceCall) -> Boolean,
    sendScope: CoroutineScope) {
    val scope = rememberCoroutineScope()
    val latestSend by rememberUpdatedState(send)
    val state = remember { PanelState(scope, entity, sendScope) { latestSend(it) } }
    SideEffect { state.live = entity }
    DisposableEffect(state) { onDispose { state.close() } }
    val shown = state.shown
    val control = shown?.takeIf { stateText(it) != StateText.Unavailable }?.let { quickControl(type, it) }
    val active = enabled && control != null
    val value = control?.value
    val source = remember { MutableInteractionSource() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { first.requestFocus() } }
    // The card is the only thing in it that takes focus, so it draws no focus ring.
    Column(Modifier.fillMaxWidth().focusRequester(first)
        .onPreviewKeyEvent { event ->
            if (event.key != Key.DirectionLeft && event.key != Key.DirectionRight) return@onPreviewKeyEvent false
            val c = control
            val now = c?.value
            if (active && c != null && now != null && event.type == KeyEventType.KeyDown)
                state.submit(c.set(c.nudge(now, event.key == Key.DirectionRight, event.nativeKeyEvent.repeatCount >= 5)), delayed = true)
            true
        }
        .clickable(source, indication = null, role = Role.Button) { control?.ok?.takeIf { active }?.let { state.submit(it) } }
        .padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val glow = if (type == CardType.LIGHT) lightGlow(shown).takeIf { it.alpha > 0f } else null
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(cardIcon(type, shown), null, Modifier.size(20.dp), tint = glow?.color ?: MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entityStateLabel(shown), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (type == CardType.MEDIA_PLAYER) shown?.attributes?.text("media_title")?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (control != null && value != null) {
            val locale = LocalConfiguration.current.locales[0]
            Text(when {
                control.degrees -> formatNumber(value, locale) + "°"
                type == CardType.LIGHT && value <= 0.0 -> stringResource(R.string.ha_off)
                else -> stringResource(R.string.ha_percent, value.roundToInt())
            }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            val fraction = ((value - control.range.start) / (control.range.endInclusive - control.range.start).coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
            LevelBar(fraction, glow?.color ?: MaterialTheme.colorScheme.primary, showArrows = active)
        }
        // One line is always kept, so a change never makes the card jump.
        Text(when {
            state.failed -> stringResource(R.string.ha_v2_failed)
            else -> control?.okLabel?.takeIf { active && control.ok != null }?.let { stringResource(R.string.ha_drop_ok, stringResource(it)) }.orEmpty()
        }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 1, maxLines = 1)
    }
}

/** A slim bar with the level filled; the fill scales rather than resizes, so it only moves in a graphics layer. */
@Composable
private fun LevelBar(fraction: Float, fill: androidx.compose.ui.graphics.Color, showArrows: Boolean) {
    val motion = stovaMotion
    val shown by androidx.compose.animation.core.animateFloatAsState(fraction, motion.fade())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val arrow = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(StovaIcons.ChevronLeft, null, Modifier.size(16.dp).graphicsLayer { alpha = if (showArrows) 1f else 0f }, tint = arrow)
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)) {
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = shown; transformOrigin = TransformOrigin(0f, .5f) }.background(fill))
        }
        Icon(StovaIcons.ChevronRight, null, Modifier.size(16.dp).graphicsLayer { alpha = if (showArrows) 1f else 0f }, tint = arrow)
    }
}

private val DropdownWidth = 320.dp
