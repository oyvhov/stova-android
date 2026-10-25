package app.stova.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A one-line text field the remote can walk past (ported from Spole).
 *
 * The keyboard waits for OK: focus alone starts no input session, so it never opens on the way to
 * the button below, and up and down always leave the field. Built on a TextFieldState because only
 * that path honours `showKeyboardOnFocus`.
 */
@Composable
fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    isError: Boolean = false,
    supportingText: String? = null,
    onImeAction: (() -> Unit)? = null,
) {
    val state = rememberTextFieldState(value)
    // Seeded with the starting value, so the field's first reading is not sent back as a change.
    val sent = remember { ArrayDeque<String>().apply { addLast(value) } }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { typed ->
            if (typed == sent.lastOrNull()) return@collect
            sent.addLast(typed)
            while (sent.size > 8) sent.removeFirst()
            currentOnValueChange(typed)
        }
    }
    LaunchedEffect(value) {
        if (value != state.text.toString() && value !in sent) {
            sent.clear()
            sent.addLast(value)
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    // True from OK until focus leaves: the session the remote asked for.
    var typing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    TextField(
        state,
        modifier.onFocusChanged { if (!it.isFocused) typing = false }.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                    if (typing) keyboard?.show() else typing = true
                    true
                }
                else -> false
            }
        },
        outputTransformation = if (keyboardType == KeyboardType.Password) androidx.compose.foundation.text.input.OutputTransformation { replace(0, length, "•".repeat(length)) } else null,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, showKeyboardOnFocus = typing),
        onKeyboardAction = onImeAction?.let { action -> KeyboardActionHandler { action() } },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surface,
            focusedIndicatorColor = colors.onSurfaceVariant,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedLabelColor = colors.onSurface,
            cursorColor = colors.onSurface,
        ),
    )
}
