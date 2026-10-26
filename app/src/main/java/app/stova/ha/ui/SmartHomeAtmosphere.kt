package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.stova.core.theme.HaTokens
import coil3.compose.AsyncImage

/**
 * The room behind the cards: Home's last picture, held still and dimmed far down, under the sky of
 * the dashboard's own weather card. It reuses what is already decoded and asks nothing of any service.
 */
@Composable
internal fun SmartHomeAtmosphere(backdrop: String?, weather: String?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(ink).clearAndSetSemantics { }) {
        if (backdrop != null) AsyncImage(backdrop, null, Modifier.fillMaxSize().testTag("smarthome-backdrop:$backdrop"),
            contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(ink.copy(alpha = if (backdrop != null) .84f else 1f)))
        val sky = weather?.let(HaTokens::sky)
        Box(Modifier.fillMaxSize().testTag("smarthome-sky:${weather ?: "none"}").background(Brush.verticalGradient(
            if (sky != null) listOf(sky.copy(alpha = .2f), sky.copy(alpha = .06f), Color.Transparent)
            else listOf(ink.copy(alpha = 0f), Color.Transparent))))
    }
}
