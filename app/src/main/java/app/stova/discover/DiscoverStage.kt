package app.stova.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.stova.background.BackdropState
import app.stova.background.BackdropLayer
import app.stova.core.ui.stepEntrance

/** Owns the artwork for the whole Oppdag stack; only the controls change with the step. */
@Composable
internal fun DiscoverStage(step: Any, depth: Int, reading: Boolean, backdrop: BackdropState,
    content: @Composable (Modifier, ((String?) -> Unit)?) -> Unit) {
    var requested by remember {mutableStateOf(backdrop.shown)}
    Box(Modifier.fillMaxSize()) {
        // Keeping only the URL is insufficient: a new AsyncImage still has an empty first frame,
        // including on a memory-cache hit. The painter itself must survive every step change.
        BackdropLayer(requested,state=backdrop,reading=reading)
        content(Modifier.stepEntrance(step, depth), {requested=it})
    }
}
