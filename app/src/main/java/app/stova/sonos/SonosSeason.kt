package app.stova.sonos

import androidx.compose.runtime.Composable
import app.stova.core.theme.LocalAppearance
import app.stova.home.SeasonalMark
import app.stova.profiles.SeasonalDecor
import app.stova.profiles.season

/** The same profile-resolved season and decoration switch as the Home header. */
@Composable internal fun SonosSeasonMark() {
    val appearance = LocalAppearance.current
    if (appearance.seasonalDecor != SeasonalDecor.OFF) SeasonalMark(appearance.season())
}
