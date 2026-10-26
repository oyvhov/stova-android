package app.stova.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.stova.R
import kotlin.math.roundToInt

/** One focus stop. OK enters arrow adjustment; Back/OK leave it, Up/Down move normally.
 * The track also accepts direct taps/drags and exposes a single accessible progress action. */
@Composable
fun SettingsSliderRow(title: String, value: Int, range: IntRange, display: String,
    modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focus = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var okDown by remember { mutableStateOf(false) }
    var backDown by remember { mutableStateOf(false) }
    val current = value.coerceIn(range)
    val latestChange by rememberUpdatedState(onChange)
    val input = remember { SliderInput(current) }
    SideEffect { input.value = current }
    fun change(next: Int) {
        val clamped = next.coerceIn(range)
        if (clamped != input.value) { input.value = clamped; latestChange(clamped) }
    }
    val hint = stringResource(if (editing) R.string.slider_adjust_hint else R.string.slider_start_hint)
    BackHandler(editing) { editing = false }
    Column(modifier.fillMaxWidth().focusRequester(focus)
        .onFocusChanged { if (!it.isFocused) { editing = false; okDown = false; backDown = false } }
        .focusOutline(source, MaterialTheme.shapes.medium).clip(MaterialTheme.shapes.medium)
        .semantics(mergeDescendants = true) {
            stateDescription = display
            progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), range.first.toFloat()..range.last.toFloat(),
                (range.last - range.first - 1).coerceAtLeast(0))
            setProgress { if (it.isFinite()) { change(it.roundToInt()); true } else false }
        }.onPreviewKeyEvent { event ->
            when {
                event.key in listOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter) -> {
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) okDown = true
                    if (event.type == KeyEventType.KeyUp && okDown) { okDown = false; editing = !editing }
                    true
                }
                (editing || backDown) && event.key in listOf(Key.Back, Key.Escape) -> {
                    backDown = event.type == KeyEventType.KeyDown
                    if (event.type == KeyEventType.KeyUp) editing = false
                    true
                }
                editing && event.key in listOf(Key.DirectionLeft, Key.DirectionRight) -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val step = if (event.nativeKeyEvent.repeatCount >= 5) 5 else 1
                        change(input.value + if (event.key == Key.DirectionRight) step else -step)
                    }
                    true
                }
                event.key in listOf(Key.DirectionUp, Key.DirectionDown) -> { editing = false; false }
                else -> false
            }
        }.clickable(source, indication = null) { editing = !editing }
        .padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(display, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val track = MaterialTheme.colorScheme.outlineVariant
        val fill = if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        val thumb = if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        Canvas(Modifier.fillMaxWidth().height(22.dp).clearAndSetSemantics {}
            .pointerInput(range) {
                detectTapGestures { point ->
                    focus.requestFocus(); editing = true
                    val radius = 7.dp.toPx()
                    change((range.first + ((point.x - radius) / (size.width - 2 * radius).coerceAtLeast(1f)) * (range.last - range.first)).roundToInt())
                }
            }.pointerInput(range) {
                fun update(x: Float) {
                    val radius = 7.dp.toPx()
                    change((range.first + ((x - radius) / (size.width - 2 * radius).coerceAtLeast(1f)) * (range.last - range.first)).roundToInt())
                }
                detectHorizontalDragGestures(onDragStart = { focus.requestFocus(); editing = true; update(it.x) }) { change, _ ->
                    change.consume(); update(change.position.x)
                }
            }) {
            val radius = 7.dp.toPx()
            val y = size.height / 2
            val x = radius + (size.width - radius * 2) * (current - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)
            drawLine(track, Offset(radius, y), Offset(size.width - radius, y), 3.dp.toPx(), StrokeCap.Round)
            drawLine(fill, Offset(radius, y), Offset(x, y), 3.dp.toPx(), StrokeCap.Round)
            drawCircle(thumb, if (editing) radius else 5.dp.toPx(), Offset(x, y))
        }
        Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class SliderInput(var value: Int)
