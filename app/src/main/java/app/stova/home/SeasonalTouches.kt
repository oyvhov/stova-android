package app.stova.home

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.*
import app.stova.core.ui.StovaIcons
import app.stova.profiles.*

@Composable
fun SeasonalMark(season: Season, modifier: Modifier = Modifier) {
    if (season == Season.NONE) return
    Icon(if (season == Season.CHRISTMAS) StovaIcons.Snowflake else StovaIcons.Pumpkin,
        null, modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SeasonalCountdown(season: Season) {
    val days = season.daysUntilPeak(LocalSeasonDate.current) ?: return
    val christmas = season == Season.CHRISTMAS
    val label = when (days) {
        0 -> stringResource(if (christmas) R.string.theme_peak_christmas else R.string.theme_peak_halloween)
        else -> pluralStringResource(if (christmas) R.plurals.theme_countdown_christmas else R.plurals.theme_countdown_halloween, days, days)
    }
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
