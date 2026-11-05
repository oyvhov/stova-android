package app.stova.apps

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R

/** Reorder only the chosen slots. Hidden, newly added and temporarily unavailable favourites survive. */
internal fun mergeFavoriteOrder(current: List<String>, ordered: List<String>): List<String> {
    val chosen = ordered.distinct().filter { it in current }
    val keys = chosen.toSet()
    val next = chosen.iterator()
    return current.map { if (it in keys) next.next() else it }
}

/** A draft until OK. Key identity follows the tile, even across equal labels and repeated arrows. */
@Stable
internal class FavoriteMoveState {
    var key by mutableStateOf<String?>(null); private set
    var focusKey by mutableStateOf<String?>(null); private set
    var revision by mutableIntStateOf(0); private set
    private var draft by mutableStateOf(emptyList<String>())
    private var original = emptyList<String>()
    private var awaitingSave = false
    private var confirmPressed = false

    fun begin(key: String, keys: List<String>) {
        if (key !in keys) return
        original = keys; draft = keys; this.key = key; focusKey = key
        awaitingSave = false; confirmPressed = false; revision++
    }
    fun ordered(keys: List<String>) = if (draft.isEmpty()) keys else mergeFavoriteOrder(keys, draft)
    fun move(delta: Int) {
        val moving = key ?: return
        draft = moveFavorite(draft, moving, delta); revision++
    }
    fun finish(): List<String>? {
        if (key == null) return null
        key = null; awaitingSave = true; confirmPressed = false; revision++
        return draft
    }
    fun cancel() {
        key = null; draft = emptyList(); awaitingSave = false; confirmPressed = false; revision++
    }
    fun reconcile(keys: List<String>) {
        if (key != null && key !in keys) cancel()
        if (awaitingSave && keys != original) { draft = emptyList(); awaitingSave = false }
        if (focusKey !in keys) focusKey = null
    }
    fun handle(event: KeyEvent, save: (List<String>) -> Unit): Boolean {
        if (key == null) return false
        when (event.key) {
            Key.DirectionLeft, Key.DirectionRight -> if (event.type == KeyEventType.KeyDown)
                move(if (event.key == Key.DirectionLeft) -1 else 1)
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) confirmPressed = true
                if (event.type == KeyEventType.KeyUp && confirmPressed) finish()?.let(save)
            }
            Key.Back, Key.Escape -> if (event.type == KeyEventType.KeyUp) cancel()
        }
        return true
    }
}

internal fun Modifier.favoriteMoveKeys(state: FavoriteMoveState, save: (List<String>) -> Unit) =
    onPreviewKeyEvent { state.handle(it, save) }

@Composable
internal fun FavoriteMoveBackHandler(state: FavoriteMoveState) {
    BackHandler(enabled = state.key != null) { state.cancel() }
}

@Composable
internal fun FavoriteMoveHint(state: FavoriteMoveState, modifier: Modifier = Modifier) {
    if (state.key != null) Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Text(stringResource(R.string.favorite_move_hint), Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
