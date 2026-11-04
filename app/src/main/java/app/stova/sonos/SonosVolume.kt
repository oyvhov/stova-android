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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onAction: (SonosAction) -> Unit, modifier: Modifier = Modifier, prominent: Boolean = false) {
    var draft by remember(id) { mutableIntStateOf(volume) }
    var editing by remember(id) { mutableStateOf(false) }
    val send by rememberUpdatedState(onAction)
    val shownRoom by rememberUpdatedState(id)
    LaunchedEffect(id, volume, editing) { if (!editing) draft = volume }
    LaunchedEffect(id, draft, editing) {
        if (editing) {
            val forRoom = id; val value = draft
            delay(250)
            // Sent only to the room it was made for, even if another room is now shown.
            if (shownRoom == forRoom) send(SonosAction.Volume(value))
            delay(1500); editing = false
        }
    }
    fun change(to: Int) { if (enabled && !busy) { draft = to.coerceIn(0, 100); editing = true } }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val motion = stovaMotion
    val level = animateFloatAsState(draft / 100f, motion.settle(), label = "sonos-volume")
    val thumb = animateFloatAsState(if (focused) 1f else 0f, motion.fade(motion.focusRing), label = "sonos-volume-thumb")
    val colors = MaterialTheme.colorScheme
    val value = stringResource(R.string.ha_percent, draft)
    Row(modifier.height(48.dp).testTag("slider-sonos-$id").focusOutline(source, CircleShape)
        .semantics {
            contentDescription = label
            stateDescription = value
            progressBarRangeInfo = ProgressBarRangeInfo(draft.toFloat(), 0f..100f, steps = 49)
            if (!enabled) disabled()
            setProgress { if (enabled && !busy) { change(it.toInt()); true } else false }
        }.onPreviewKeyEvent { event ->
            if (event.key != Key.DirectionLeft && event.key != Key.DirectionRight) false else {
                if (event.type == KeyEventType.KeyDown) {
                    val delta = 2 * percentageStep(event.nativeKeyEvent.repeatCount)
                    change(draft + if (event.key == Key.DirectionRight) delta else -delta)
                }
                true
            }
        }.focusable(interactionSource = source).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val tint = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = .5f)
        Icon(StovaIcons.Speaker, null, Modifier.size(20.dp), tint = tint)
        // Only the draw phase reads the animated values: no layout or recomposition per frame.
        Canvas(Modifier.weight(1f).height(18.dp)) {
            val y = size.height / 2
            val line = (if (prominent) 4.dp else 3.dp).toPx()
            drawLine(colors.onSurface.copy(alpha = if (prominent) .45f else .18f), Offset(0f, y), Offset(size.width, y), line, StrokeCap.Round)
            val x = size.width * level.value.coerceIn(0f, 1f)
            if (x > 0f) drawLine(tint, Offset(0f, y), Offset(x, y), line, StrokeCap.Round)
            drawCircle(tint, (4.dp + 3.dp * thumb.value).toPx(), Offset(x, y))
        }
    }
}
