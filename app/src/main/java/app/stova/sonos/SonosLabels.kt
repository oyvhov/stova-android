package app.stova.sonos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A brand prefix is already visible in Sonos chrome. Keep the saved name for commands and settings. */
internal val SonosRoom.displayName: String
    get() = if (name.startsWith("Sonos ", ignoreCase = true)) name.drop(6).trim().ifEmpty { name } else name

@Composable
internal fun SonosLabels(
    title: String,
    detail: String?,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier,
    titleLines: Int = 1,
    detailStyle: TextStyle = MaterialTheme.typography.bodySmall,
    detailColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = titleStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = titleLines, overflow = TextOverflow.Ellipsis)
        detail?.takeIf(String::isNotBlank)?.let {
            Text(it, style = detailStyle, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
