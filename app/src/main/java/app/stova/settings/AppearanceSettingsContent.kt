package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.theme.*
import app.stova.core.ui.SettingsGroup
import app.stova.profiles.*
import app.stova.profiles.Profile

@Composable
fun AppearanceSettingsContent(
    p: AppearancePrefs,
    packs: List<app.stova.apps.IconPack> = emptyList(),
    onPickBackground: () -> Unit = {},
    onEditRows: () -> Unit = {},
    includePreview: Boolean = true,
    previewEntry: app.stova.media.SpotlightEntry? = null,
    homePrefs: HomePrefs = HomePrefs(),
    others: List<Profile> = emptyList(),
    onReset: (() -> Unit)? = null,
    onCopy: (String) -> Unit = {},
    onChange: (AppearancePrefs) -> Unit,
) {
    if (includePreview) ThemePreview(p, previewEntry, homePrefs)
    Text(stringResource(R.string.appearance_choice_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    SettingsGroup(stringResource(R.string.theme_group)) {
        GroupHint(R.string.custom_theme_hint)
        // Plain moods only: browsing them never flips the season (Jul and Halloween are in Sesong).
        val mood = p.visualTheme.takeIf { it in app.stova.profiles.PLAIN_MOODS } ?: VisualTheme.FOREST
        Choice(R.string.theme_mood, mood, app.stova.profiles.PLAIN_MOODS, app.stova.profiles.PLAIN_MOODS.map { themeLabels[it.ordinal] },
            preview = { ThemeSwatches(mood) }) { theme -> onChange(p.withMood(theme)) }
        Choice(R.string.appearance_accent, p.accent, Accent.entries, accentLabels,
            preview = { AccentSwatches(p.accent) }) { onChange(p.copy(accent = it)) }
        Choice(R.string.theme_season, p.seasonChoice, SeasonChoice.entries,
            listOf(R.string.theme_none,R.string.theme_calendar,R.string.theme_christmas,R.string.theme_halloween)) { onChange(it.applyTo(p)) }
        Choice(R.string.theme_decor, p.seasonalDecor, SeasonalDecor.entries,
            listOf(R.string.appearance_off,R.string.theme_calm,R.string.theme_full)) { onChange(p.copy(seasonalDecor=it)) }
    }
    SettingsGroup(stringResource(R.string.custom_home_hero)) {
        GroupHint(R.string.custom_hero_hint)
        Choice(R.string.appearance_hero_size, p.heroSize, HeroSize.entries, listOf(R.string.appearance_off, R.string.appearance_compact, R.string.appearance_large)) { onChange(p.copy(heroSize = it)) }
        Choice(R.string.custom_hero_title, p.heroTitle, HeroTitle.entries, listOf(R.string.custom_logo, R.string.custom_always_title)) { onChange(p.copy(heroTitle = it)) }
        Toggle(R.string.appearance_overview, p.showOverview) { onChange(p.copy(showOverview = it)) }
        Toggle(R.string.appearance_genres, p.showGenres) { onChange(p.copy(showGenres = it)) }
        Toggle(R.string.custom_hero_year, p.showHeroYear) { onChange(p.copy(showHeroYear = it)) }
        Toggle(R.string.custom_hero_age, p.showHeroAge) { onChange(p.copy(showHeroAge = it)) }
        Toggle(R.string.custom_hero_ratings, p.showHeroRatings) { onChange(p.copy(showHeroRatings = it)) }
        Toggle(R.string.custom_hero_new_season, p.showNewSeason) { onChange(p.copy(showNewSeason = it)) }
        Choice(R.string.appearance_spotlight_time, p.spotlightTime, SpotlightTime.entries, listOf(R.string.appearance_8s, R.string.appearance_12s, R.string.appearance_20s)) { onChange(p.copy(spotlightTime = it)) }
    }
    SettingsGroup(stringResource(R.string.custom_cards_rows)) {
        GroupHint(R.string.custom_cards_hint)
        Choice(R.string.appearance_card_size, p.cardSize, ArtworkSize.entries, listOf(R.string.appearance_small, R.string.appearance_medium, R.string.appearance_large)) { onChange(p.copy(cardSize = it)) }
        Toggle(R.string.appearance_titles, p.showTitles) { onChange(p.copy(showTitles = it)) }
        Choice(R.string.appearance_app_style, p.appStyle, AppStyle.entries, listOf(R.string.appearance_banner, R.string.appearance_icon)) { onChange(p.copy(appStyle = it)) }
        Choice(R.string.row_latest_movies, p.movieStyle, MovieStyle.entries, listOf(R.string.appearance_poster, R.string.appearance_wide)) { onChange(p.copy(movieStyle = it)) }
        Choice(R.string.appearance_corners, p.cornerRadius, CornerRadius.entries, listOf(R.string.appearance_sharp, R.string.custom_soft, R.string.appearance_round, R.string.custom_extra_round)) { onChange(p.copy(cornerRadius = it)) }
        Choice(R.string.custom_card_spacing, p.cardSpacing, CardSpacing.entries, listOf(R.string.appearance_tight, R.string.appearance_normal, R.string.appearance_airy)) { onChange(p.copy(cardSpacing = it)) }
        Choice(R.string.appearance_row_spacing, p.rowSpacing, RowSpacing.entries, listOf(R.string.appearance_tight, R.string.appearance_normal, R.string.appearance_airy)) { onChange(p.copy(rowSpacing = it)) }
        Choice(R.string.custom_progress, p.progressStyle, ProgressStyle.entries, listOf(R.string.custom_thin, R.string.custom_thick, R.string.appearance_hidden)) { onChange(p.copy(progressStyle = it)) }
        app.stova.core.ui.SettingsNavRow(stringResource(R.string.custom_edit_rows), onClick = onEditRows)
    }
    SettingsGroup(stringResource(R.string.appearance_background)) {
        GroupHint(R.string.custom_background_hint)
        Choice(R.string.custom_background_source, p.backgroundSource, BackgroundSource.entries,
            listOf(R.string.custom_spotlight, R.string.custom_fixed_image, R.string.custom_theme_surface)) { onChange(p.copy(backgroundSource = it)) }
        if (p.backgroundSource == BackgroundSource.FIXED) app.stova.core.ui.SettingsNavRow(
            stringResource(R.string.custom_pick_background), value = p.fixedBackground?.title, onClick = onPickBackground)
        Choice(R.string.custom_spotlight_source, p.spotlightSource, SpotlightSource.entries,
            listOf(R.string.custom_whole_library, R.string.custom_only_movies, R.string.custom_only_series, R.string.custom_favorites)) { onChange(p.copy(spotlightSource = it)) }
        Choice(R.string.appearance_dimming, p.backdropDimming, BackdropDimming.entries, listOf(R.string.appearance_light, R.string.appearance_medium, R.string.appearance_dark)) { onChange(p.copy(backdropDimming = it)) }
        Toggle(R.string.custom_background_motion, p.backgroundMotion) { onChange(p.copy(backgroundMotion = it)) }
    }
    SettingsGroup(stringResource(R.string.custom_focus)) {
        GroupHint(R.string.custom_focus_hint)
        Choice(R.string.appearance_focus_effect, p.focusEffect.resolved(),
            listOf(FocusEffect.LIFT, FocusEffect.ZOOM, FocusEffect.LIGHT_UP, FocusEffect.OUTLINE, FocusEffect.RING_ZOOM),
            listOf(R.string.custom_focus_lift, R.string.appearance_zoom, R.string.custom_light_up, R.string.appearance_ring, R.string.custom_ring_zoom)) { onChange(p.copy(focusEffect = it)) }
        if (p.focusEffect.resolved() == FocusEffect.OUTLINE || p.focusEffect == FocusEffect.RING_ZOOM)
            Choice(R.string.custom_ring_width, p.ringWidth, RingWidth.entries, listOf(R.string.custom_thin, R.string.appearance_normal, R.string.custom_thick)) { onChange(p.copy(ringWidth = it)) }
        Toggle(R.string.appearance_reduce_motion, p.reduceMotion) { onChange(p.copy(reduceMotion = it)) }
    }
    SettingsGroup(stringResource(R.string.custom_top_bar)) {
        GroupHint(R.string.custom_top_hint)
        Choice(R.string.appearance_clock_format, p.clock, ClockMode.entries, listOf(R.string.appearance_24h, R.string.appearance_12h, R.string.appearance_hidden)) { onChange(p.copy(clock = it)) }
        Choice(R.string.custom_clock_size, p.clockSize, ClockSize.entries, listOf(R.string.appearance_normal, R.string.appearance_large)) { onChange(p.copy(clockSize = it)) }
        Toggle(R.string.custom_show_date, p.showDate) { onChange(p.copy(showDate = it)) }
        Toggle(R.string.custom_show_chips, p.showHaChips) { onChange(p.copy(showHaChips = it)) }
        Toggle(R.string.custom_show_countdown, p.showCountdown) { onChange(p.copy(showCountdown = it)) }
        Toggle(R.string.custom_show_apps, p.showAppsButton) { onChange(p.copy(showAppsButton = it)) }
        Toggle(R.string.custom_show_smart_home, p.showSmartHomeButton) { onChange(p.copy(showSmartHomeButton = it)) }
        Toggle(R.string.custom_show_profile, p.showProfile) { onChange(p.copy(showProfile = it)) }
    }
    IconPackSettingsContent(p, packs, stringResource(R.string.custom_icons), stringResource(R.string.custom_icons_hint), onChange)
    AppearanceProfileActions(others, onReset ?: { onChange(AppearancePrefs()) }, onCopy)
}

@Composable
private fun GroupHint(id: Int) {
    Text(stringResource(id), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Toggle(title: Int, value: Boolean, onChange: (Boolean) -> Unit) =
    Choice(title, value, listOf(false, true), listOf(R.string.appearance_off, R.string.appearance_on), onChange = onChange)

@Composable
private fun <T> Choice(title: Int, value: T, entries: List<T>, labels: List<Int>, preview: (@Composable () -> Unit)? = null, onChange: (T) -> Unit) {
    val index = entries.indexOf(value)
    app.stova.core.ui.SettingsChoiceRow(stringResource(title), stringResource(labels[index]), preview) { step ->
        onChange(entries[Math.floorMod(index + step, entries.size)])
    }
}
