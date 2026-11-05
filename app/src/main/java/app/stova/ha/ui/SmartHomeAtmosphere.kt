package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.stova.core.theme.HaTokens
import app.stova.core.theme.stovaMotion

/**
 * A quiet surface belonging to the house. Only its own weather adds a restrained sky tint;
 * movie artwork is never loaded here, even when Home passes its previous backdrop.
 */
@Composable
internal fun SmartHomeAtmosphere(backdrop: String?, weather: String?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.background
    val sky = animateColorAsState(weather?.let(HaTokens::sky) ?: ink.copy(alpha = 0f),
        stovaMotion.fade(stovaMotion.ambient), label = "smarthome-weather")
    Box(modifier.fillMaxSize().background(ink).clearAndSetSemantics { }) {
        Box(Modifier.fillMaxSize().testTag("smarthome-sky:${weather ?: "none"}").drawBehind {
            val tint = sky.value
            drawRect(Brush.verticalGradient(listOf(tint.copy(alpha = tint.alpha * .2f), tint.copy(alpha = tint.alpha * .06f), Color.Transparent)))
        })
    }
}
