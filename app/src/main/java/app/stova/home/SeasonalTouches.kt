package app.stova.home

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.*
import app.stova.core.ui.StovaIcons
import app.stova.profiles.*

/** The season's own glyph in its own warm or cold light, at the top bar's left edge. */
@Composable
fun SeasonalMark(season: Season, modifier: Modifier = Modifier) {
    if (season == Season.NONE) return
    Icon(if (season == Season.CHRISTMAS) StovaIcons.Snowflake else StovaIcons.Pumpkin,
        null, modifier.size(28.dp).testTag("season-mark"),
        tint = (if (season == Season.CHRISTMAS) StovaColors.Snow else StovaColors.Ember).copy(alpha = 0.9f))
}

@Composable
fun SeasonalCountdown(season: Season) {
    if (season != Season.CHRISTMAS || !LocalAppearance.current.showCountdown) return
    val days = season.daysUntilPeak(LocalSeasonDate.current) ?: return
    val label = when (days) {
        0 -> stringResource(R.string.theme_peak_christmas)
        else -> pluralStringResource(R.plurals.theme_countdown_christmas, days, days)
    }
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
