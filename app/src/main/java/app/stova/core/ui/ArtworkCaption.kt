package app.stova.core.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.core.theme.stovaMotion

/** One stable caption line. Only the focused card slowly reveals an overflowing name. */
@Composable
internal fun ArtworkCaption(text: String, source: InteractionSource, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall) {
    val focused by source.collectIsFocusedAsState()
    val reveal = focused && !stovaMotion.still
    Text(text, modifier.then(if (reveal) Modifier.basicMarquee(iterations = Int.MAX_VALUE,
        initialDelayMillis = 1400, repeatDelayMillis = 2200, velocity = 24.dp) else Modifier),
        style = style, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
