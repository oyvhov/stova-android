package app.stova.sonos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.stovaMotion
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline
import app.stova.ha.cards.percentageStep
import kotlinx.coroutines.delay

/**
 * Volume as a quiet rail: a speaker mark, a thin line, a small thumb, no words on screen. The whole
 * row is the TV focus and hit area; TalkBack still hears [label], the value and the range.
 * Held keys are coalesced; while [busy] nothing changes and nothing is sent, and focus stays.
 * The draft belongs to one room ([id]): a pending change never follows to the next one.
 */
@Composable internal fun SonosVolumeRail(id: String, volume: Int, enabled: Boolean, busy: Boolean, label: String,
    onAction: (SonosAction) -> Unit, modifier: Modifier = Modifier) {
    val draft = rememberSonosVolumeDraft(id, volume, enabled, busy, onAction)
    SonosVolumeSlider(id, draft, enabled, busy, label, modifier)
}

internal class SonosVolumeDraft(val value: Int, val change: ((Int) -> Int) -> Unit, val flush: () -> Unit)

@Composable
internal fun rememberSonosVolumeDraft(id: String, volume: Int, enabled: Boolean, busy: Boolean,
    onAction: (SonosAction) -> Unit): SonosVolumeDraft {
    var local by remember(id) { mutableStateOf<Int?>(null) }
    var pending by remember(id) { mutableStateOf<Int?>(null) }
    val send by rememberUpdatedState(onAction)
    val shownRoom by rememberUpdatedState(id)
    fun flush() {
        val value = pending ?: return
        pending = null
        if (shownRoom == id) send(SonosAction.Volume(value))
    }
    LaunchedEffect(id, pending) {
        if (pending != null) { delay(250); flush() }
    }
    LaunchedEffect(id, local, pending) {
        if (local != null && pending == null) { delay(1500); local = null }
    }
    return SonosVolumeDraft(local ?: volume, { adjust ->
        if (enabled && !busy) {
            val current = local ?: volume
            val next = adjust(current).coerceIn(0, 100)
            if (next != current) { local = next; pending = next }
        }
    }, ::flush)
}

@Composable
internal fun SonosVolumeSlider(id: String, draft: SonosVolumeDraft, enabled: Boolean, busy: Boolean, label: String,
    modifier: Modifier = Modifier, vertical: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val motion = stovaMotion
    val level = animateFloatAsState(draft.value / 100f, motion.settle(), label = "sonos-volume")
    val thumb = animateFloatAsState(if (focused) 1f else 0f, motion.fade(motion.focusRing), label = "sonos-volume-thumb")
    val colors = MaterialTheme.colorScheme
    val value = stringResource(R.string.ha_percent, draft.value)
    val up = if (vertical) Key.DirectionUp else Key.DirectionRight
    val down = if (vertical) Key.DirectionDown else Key.DirectionLeft
    val control = modifier.testTag("slider-sonos-$id").focusOutline(source, CircleShape)
        .semantics {
            contentDescription = label
            stateDescription = value
            progressBarRangeInfo = ProgressBarRangeInfo(draft.value.toFloat(), 0f..100f, steps = 49)
            if (!enabled) disabled()
            setProgress { target -> if (enabled && !busy) { draft.change { target.toInt() }; true } else false }
        }.onPreviewKeyEvent { event ->
            if (event.key != up && event.key != down) false else {
                if (event.type == KeyEventType.KeyDown) {
                    val delta = 2 * percentageStep(event.nativeKeyEvent.repeatCount)
                    draft.change { it + if (event.key == up) delta else -delta }
                }
                true
            }
        }.focusable(interactionSource = source)
    val tint = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = .5f)
    val track = colors.onSurface.copy(alpha = if (vertical) .42f else .18f)
    val drawRail: DrawScope.() -> Unit = {
        val progress = level.value.coerceIn(0f, 1f)
        val from = if (vertical) Offset(size.width / 2, size.height) else Offset(0f, size.height / 2)
        val to = if (vertical) Offset(size.width / 2, 0f) else Offset(size.width, size.height / 2)
        val point = from + (to - from) * progress
        val line = (if (vertical) 4.dp else 3.dp).toPx()
        drawLine(track, from, to, line, StrokeCap.Round)
        if (progress > 0f) drawLine(tint, from, point, line, StrokeCap.Round)
        drawCircle(tint, (4.dp + 3.dp * thumb.value).toPx(), point)
    }
    if (vertical) Column(control.widthIn(min = 56.dp).padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Canvas(Modifier.width(20.dp).height(92.dp), onDraw = drawRail)
    } else Row(control.height(48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(StovaIcons.Speaker, null, Modifier.size(20.dp), tint = tint)
        Canvas(Modifier.weight(1f).height(18.dp), onDraw = drawRail)
    }
}
