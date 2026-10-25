package app.stova.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaShapes
import app.stova.profiles.PinCheck
import kotlin.math.ceil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_DIGITS = 8
private const val MIN_DIGITS = 4

@Composable
fun PinDialog(
    title: String,
    onSubmit: suspend (String) -> PinCheck,
    onAccepted: () -> Unit,
    onDismiss: () -> Unit,
) {
    StovaDialog(onDismiss) {
        PinPanel(title, onSubmit, onAccepted)
    }
}

/**
 * Number pad for a remote: the digits are buttons in a 3 × 4 grid, and the number keys on the
 * remote type directly. A lockout greys out OK and counts down until another try is allowed.
 */
@Composable
fun PinPanel(
    title: String,
    onSubmit: suspend (String) -> PinCheck,
    onAccepted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lockMillis by remember { mutableStateOf(0L) }
    var lockToken by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }

    LaunchedEffect(lockToken) {
        var left = ceil(lockMillis / 1000.0).toInt()
        while (left > 0) {
            secondsLeft = left
            delay(1_000)
            left--
        }
        secondsLeft = 0
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    fun type(digit: Char) {
        if (pin.length < MAX_DIGITS) { pin += digit; wrong = false }
    }
    fun erase() { pin = pin.dropLast(1) }
    fun submit() {
        val entered = pin
        busy = true
        scope.launch {
            when (val result = onSubmit(entered)) {
                PinCheck.Accepted -> onAccepted()
                is PinCheck.Rejected -> {
                    pin = ""
                    if (result.waitMillis > 0) { lockMillis = result.waitMillis; lockToken++ } else wrong = true
                }
                is PinCheck.LockedOut -> { pin = ""; lockMillis = result.remainingMillis; lockToken++ }
            }
            busy = false
        }
    }
    val deleteLabel = stringResource(R.string.pin_delete)
    val okLabel = stringResource(R.string.pin_ok)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val labelWidth = maxOf(measurer.measure(deleteLabel, labelStyle).size.width, measurer.measure(okLabel, labelStyle).size.width)
    val keyWidth = with(LocalDensity.current) { maxOf(80.dp, labelWidth.toDp() + 40.dp) }
    val panelWidth = maxOf(320.dp, keyWidth * 3 + 24.dp)
    val canSubmit = pin.length >= MIN_DIGITS && secondsLeft == 0 && !busy

    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
            .padding(32.dp)
            .width(panelWidth)
            .verticalScroll(rememberScrollState())
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = digitFor(event.key)
                when {
                    digit != null -> { type(digit); true }
                    event.key == Key.Backspace || event.key == Key.Delete -> { erase(); true }
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        PinDots(pin.length)
        val message = when {
            secondsLeft > 0 -> stringResource(R.string.pin_wait_seconds, secondsLeft)
            wrong -> stringResource(R.string.pin_wrong)
            else -> null
        }
        Text(
            message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.heightIn(min = 20.dp),
        )
        val rows = listOf("123", "456", "789")
        rows.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { column, digit ->
                    val keyModifier = if (index == 0 && column == 0) Modifier.focusRequester(first) else Modifier
                    StovaButton(digit.toString(), { type(digit) }, keyModifier.width(keyWidth))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StovaButton(deleteLabel, ::erase, Modifier.width(keyWidth))
            StovaButton("0", { type('0') }, Modifier.width(keyWidth))
            StovaButton(okLabel, ::submit, Modifier.width(keyWidth), primary = true, enabled = canSubmit)
        }
    }
}

@Composable
private fun PinDots(count: Int) {
    val description = pluralStringResource(R.plurals.pin_digits_entered, count, count)
    Row(
        Modifier.semantics { contentDescription = description }.heightIn(min = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxOf(MIN_DIGITS, count)) { index ->
            val filled = index < count
            Box(
                Modifier.size(14.dp).then(
                    if (filled) Modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                ),
            )
        }
    }
}

private fun digitFor(key: Key): Char? = when (key) {
    Key.Zero, Key.NumPad0 -> '0'
    Key.One, Key.NumPad1 -> '1'
    Key.Two, Key.NumPad2 -> '2'
    Key.Three, Key.NumPad3 -> '3'
    Key.Four, Key.NumPad4 -> '4'
    Key.Five, Key.NumPad5 -> '5'
    Key.Six, Key.NumPad6 -> '6'
    Key.Seven, Key.NumPad7 -> '7'
    Key.Eight, Key.NumPad8 -> '8'
    Key.Nine, Key.NumPad9 -> '9'
    else -> null
}
