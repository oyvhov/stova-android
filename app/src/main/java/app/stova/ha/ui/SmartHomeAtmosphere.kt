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
 * A quiet surface belonging to the house. Only its own weather adds a restrained sky tint;
 * movie artwork is never loaded here, even when Home passes its previous backdrop.
 */
@Composable
internal fun SmartHomeAtmosphere(backdrop: String?, weather: String?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(ink).clearAndSetSemantics { }) {
        val sky = weather?.let(HaTokens::sky)
        Box(Modifier.fillMaxSize().testTag("smarthome-sky:${weather ?: "none"}").background(Brush.verticalGradient(
            if (sky != null) listOf(sky.copy(alpha = .2f), sky.copy(alpha = .06f), Color.Transparent)
            else listOf(ink.copy(alpha = 0f), Color.Transparent))))
    }
}
