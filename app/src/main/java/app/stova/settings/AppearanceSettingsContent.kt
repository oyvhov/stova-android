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
import app.stova.core.ui.SettingsSliderRow
import app.stova.core.ui.labelRes
import app.stova.profiles.*
import app.stova.profiles.Profile

@Composable
fun AppearanceSettingsContent(
    p: AppearancePrefs,
    packs: List<app.stova.apps.IconPack> = emptyList(),
    onPickBackground: () -> Unit = {},
    onPickLibraries: () -> Unit = {},
    onPickKidsLibraries: () -> Unit = {},
    libraryControl: Modifier = Modifier,
    others: List<Profile> = emptyList(),
    onReset: (() -> Unit)? = null,
    onCopy: (String) -> Unit = {},
    category: AppearanceCategory? = null,
    firstControl: Modifier = Modifier,
    onChange: (AppearancePrefs) -> Unit,
) {
    if (category == null) Text(stringResource(R.string.appearance_choice_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (category == null || category == AppearanceCategory.THEME) SettingsGroup(stringResource(R.string.theme_group), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_theme_hint)
        // Plain moods only: browsing them never flips the season (Jul and Halloween are in Sesong).
        val mood = p.visualTheme.takeIf { it in app.stova.profiles.PLAIN_MOODS } ?: VisualTheme.FOREST
        Choice(R.string.theme_mood, mood, app.stova.profiles.PLAIN_MOODS, app.stova.profiles.PLAIN_MOODS.map { themeLabels[it.ordinal] }, modifier = firstControl,
            preview = { ThemeSwatches(mood) }) { theme -> onChange(p.withMood(theme)) }
        Choice(R.string.appearance_accent, p.accent, Accent.entries, accentLabels,
            preview = { AccentSwatches(p.accent) }) { onChange(p.copy(accent = it)) }
        Choice(R.string.theme_season, p.seasonChoice, SeasonChoice.entries,
            listOf(R.string.theme_none,R.string.theme_calendar,R.string.theme_christmas,R.string.theme_halloween)) { onChange(it.applyTo(p)) }
        Choice(R.string.theme_decor, p.seasonalDecor, SeasonalDecor.entries,
            listOf(R.string.appearance_off,R.string.theme_calm,R.string.theme_full)) { onChange(p.copy(seasonalDecor=it)) }
    }
    if (category == null || category == AppearanceCategory.HERO) SettingsGroup(stringResource(R.string.custom_home_hero), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_hero_hint)
        Choice(R.string.home_visible_rows, p.homeRows, HomeRows.entries,
            listOf(R.string.home_rows_one, R.string.home_rows_two, R.string.home_rows_scrolling), modifier = firstControl) { onChange(p.copy(homeRows = it)) }
        GroupHint(R.string.home_rows_hint)
        if (p.homeRows != HomeRows.SCROLLING) Choice(R.string.home_row_inset, p.homeRowInset, HomeRowInset.entries,
            listOf(R.string.home_row_low, R.string.home_row_balanced, R.string.home_row_high)) { onChange(p.copy(homeRowInset = it)) }
        Choice(R.string.appearance_hero_size, p.heroSize, HeroSize.entries, listOf(R.string.appearance_off, R.string.appearance_compact, R.string.appearance_large)) { onChange(p.copy(heroSize = it)) }
        Choice(R.string.custom_hero_title, p.heroTitle, HeroTitle.entries, listOf(R.string.custom_logo, R.string.custom_always_title)) { onChange(p.copy(heroTitle = it)) }
        Toggle(R.string.appearance_overview, p.showOverview) { onChange(p.copy(showOverview = it)) }
        Toggle(R.string.appearance_genres, p.showGenres) { onChange(p.copy(showGenres = it)) }
        Toggle(R.string.custom_hero_year, p.showHeroYear) { onChange(p.copy(showHeroYear = it)) }
        Toggle(R.string.custom_hero_age, p.showHeroAge) { onChange(p.copy(showHeroAge = it)) }
        Toggle(R.string.custom_hero_ratings, p.showHeroRatings) { onChange(p.copy(showHeroRatings = it)) }
        Toggle(R.string.custom_hero_new_season, p.showNewSeason) { onChange(p.copy(showNewSeason = it)) }
        SettingsSliderRow(stringResource(R.string.appearance_spotlight_time), (p.spotlightMillis / 1000).toInt(), 5..60,
            stringResource(R.string.slider_seconds, p.spotlightMillis / 1000)) { onChange(p.copy(spotlightSeconds = it)) }
    }
    if (category == null || category == AppearanceCategory.CARDS) SettingsGroup(stringResource(R.string.custom_cards_rows), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_cards_hint)
        Choice(R.string.appearance_card_size, p.cardSize, ArtworkSize.entries, ArtworkSize.entries.map { it.labelRes }, modifier = firstControl) { onChange(p.copy(cardSize = it)) }
        Toggle(R.string.appearance_titles, p.showTitles) { onChange(p.copy(showTitles = it)) }
        Choice(R.string.appearance_app_style, p.appStyle, AppStyle.entries, listOf(R.string.appearance_banner, R.string.appearance_icon)) { onChange(p.copy(appStyle = it)) }
        Choice(R.string.row_latest_movies, p.movieStyle, MovieStyle.entries, listOf(R.string.appearance_poster, R.string.appearance_wide)) { onChange(p.copy(movieStyle = it)) }
        SettingsSliderRow(stringResource(R.string.appearance_corners), p.cornerDp, 0..32,
            stringResource(R.string.slider_units, p.cornerDp)) { onChange(p.copy(cornerRadiusDp = it)) }
        SettingsSliderRow(stringResource(R.string.custom_card_spacing), p.spacingDp, 0..40,
            stringResource(R.string.slider_units, p.spacingDp)) { onChange(p.copy(cardSpacingDp = it)) }
        if (category == null || p.homeRows != HomeRows.ONE) Choice(R.string.appearance_row_spacing, p.rowSpacing, RowSpacing.entries, listOf(R.string.appearance_tight, R.string.appearance_normal, R.string.appearance_airy)) { onChange(p.copy(rowSpacing = it)) }
        Choice(R.string.custom_progress, p.progressStyle, ProgressStyle.entries, listOf(R.string.custom_thin, R.string.custom_thick, R.string.appearance_hidden)) { onChange(p.copy(progressStyle = it)) }
    }
    if (category == null || category == AppearanceCategory.BACKGROUND) SettingsGroup(stringResource(R.string.appearance_background), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_background_hint)
        Choice(R.string.custom_background_source, p.backgroundSource, BackgroundSource.entries,
            listOf(R.string.custom_spotlight, R.string.custom_fixed_image, R.string.custom_theme_surface), modifier = firstControl) { onChange(p.copy(backgroundSource = it)) }
        app.stova.core.ui.SettingsNavRow(stringResource(R.string.background_libraries),libraryControl,
            value=librarySelectionLabel(p.backgroundFilter.libraries),onClick=onPickLibraries)
        BackgroundAgeSetting(p.backgroundFilter.maxAge) {onChange(p.copy(backgroundFilter=p.backgroundFilter.copy(maxAge=it)))}
        if(p.backgroundFilter.maxAge!=null) GroupHint(R.string.background_age_hint)
        Toggle(R.string.kids_hours, p.kidsHours.enabled) { onChange(p.copy(kidsHours = p.kidsHours.copy(enabled = it))) }
        if (p.kidsHours.enabled) {
            app.stova.core.ui.SettingsNavRow(stringResource(R.string.kids_hours_libraries),
                value = librarySelectionLabel(p.kidsHours.libraries), onClick = onPickKidsLibraries)
            fun hourLabel(h: Int) = "%02d:00".format(h)
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_hours_from), hourLabel(p.kidsHours.startHour)) { step ->
                onChange(p.copy(kidsHours = p.kidsHours.copy(startHour = Math.floorMod(p.kidsHours.startHour + step, 24))))
            }
            app.stova.core.ui.SettingsChoiceRow(stringResource(R.string.kids_hours_to), hourLabel(p.kidsHours.endHour)) { step ->
                onChange(p.copy(kidsHours = p.kidsHours.copy(endHour = Math.floorMod(p.kidsHours.endHour + step, 24))))
            }
            GroupHint(R.string.kids_hours_hint)
            if (p.backgroundFilter.libraries == null) GroupHint(R.string.kids_hours_need_libraries)
        }
        if (p.backgroundSource == BackgroundSource.FIXED) app.stova.core.ui.SettingsNavRow(
            stringResource(R.string.custom_pick_background), value = p.fixedBackground?.title, onClick = onPickBackground)
        Choice(R.string.custom_spotlight_source, p.spotlightSource, SpotlightSource.entries,
            listOf(R.string.custom_whole_library, R.string.custom_only_movies, R.string.custom_only_series, R.string.custom_favorites)) { onChange(p.copy(spotlightSource = it)) }
        SettingsSliderRow(stringResource(R.string.appearance_dimming), p.dimmingPercent, 0..100,
            stringResource(R.string.slider_percent, p.dimmingPercent)) { onChange(p.copy(backdropDimmingPercent = it)) }
        Toggle(R.string.custom_background_motion, p.backgroundMotion) { onChange(p.copy(backgroundMotion = it)) }
    }
    if (category == null || category == AppearanceCategory.FOCUS) SettingsGroup(stringResource(R.string.custom_focus), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_focus_hint)
        Choice(R.string.appearance_focus_effect, p.focusEffect.resolved(),
            listOf(FocusEffect.LIFT, FocusEffect.ZOOM, FocusEffect.LIGHT_UP, FocusEffect.OUTLINE, FocusEffect.RING_ZOOM),
            listOf(R.string.custom_focus_lift, R.string.appearance_zoom, R.string.custom_light_up, R.string.appearance_ring, R.string.custom_ring_zoom), modifier = firstControl) { onChange(p.copy(focusEffect = it)) }
        if (p.focusEffect.resolved() == FocusEffect.OUTLINE || p.focusEffect == FocusEffect.RING_ZOOM)
            Choice(R.string.custom_ring_width, p.ringWidth, RingWidth.entries, listOf(R.string.custom_thin, R.string.appearance_normal, R.string.custom_thick)) { onChange(p.copy(ringWidth = it)) }
        Toggle(R.string.appearance_reduce_motion, p.reduceMotion) { onChange(p.copy(reduceMotion = it)) }
    }
    if (category == null || category == AppearanceCategory.TOP_BAR) SettingsGroup(stringResource(R.string.custom_top_bar), showHeading = category == null) {
        if (category == null) GroupHint(R.string.custom_top_hint)
        Choice(R.string.appearance_clock_format, p.clock, ClockMode.entries, listOf(R.string.appearance_24h, R.string.appearance_12h, R.string.appearance_hidden), modifier = firstControl) { onChange(p.copy(clock = it)) }
        Choice(R.string.custom_clock_style, p.clockStyle, ClockStyle.entries,
            listOf(R.string.clock_style_standard, R.string.clock_style_thin, R.string.clock_style_side),
            preview = { ClockStylePreview(p.clockStyle) }) { onChange(p.copy(clockStyle = it)) }
        Choice(R.string.custom_clock_size, p.clockSize, ClockSize.entries, listOf(R.string.appearance_normal, R.string.appearance_large)) { onChange(p.copy(clockSize = it)) }
        // The thin and side clocks always carry the date.
        if (p.clockStyle == ClockStyle.STANDARD) Toggle(R.string.custom_show_date, p.showDate) { onChange(p.copy(showDate = it)) }
        Toggle(R.string.custom_show_chips, p.showHaChips) { onChange(p.copy(showHaChips = it)) }
        if (p.showHaChips) Choice(R.string.custom_chip_placement, p.chipPlacement, ChipPlacement.entries,
            listOf(R.string.chip_placement_clock, R.string.chip_placement_left)) { onChange(p.copy(chipPlacement = it)) }
        Toggle(R.string.custom_show_countdown, p.showCountdown) { onChange(p.copy(showCountdown = it)) }
        Toggle(R.string.custom_show_smart_home, p.showSmartHomeButton) { onChange(p.copy(showSmartHomeButton = it)) }
        Toggle(R.string.custom_show_profile, p.showProfile) { onChange(p.copy(showProfile = it)) }
    }
    if (category == null || category == AppearanceCategory.ICONS)
        IconPackSettingsContent(p, packs, stringResource(R.string.custom_icons), stringResource(R.string.custom_icons_hint), firstControl = firstControl, showHeading = category == null, onChange = onChange)
    if (category == null || category == AppearanceCategory.PROFILE)
        AppearanceProfileActions(others, onReset ?: { onChange(AppearancePrefs()) }, onCopy, firstControl, showHeading = category == null)
}

@Composable
private fun GroupHint(id: Int) {
    Text(stringResource(id), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Toggle(title: Int, value: Boolean, onChange: (Boolean) -> Unit) =
    Choice(title, value, listOf(false, true), listOf(R.string.appearance_off, R.string.appearance_on), onChange = onChange)

/** The style's own weight on a sample time, so the choice can be seen before it is made. */
@Composable
private fun ClockStylePreview(style: ClockStyle) {
    val weight = when (style) {
        ClockStyle.THIN -> androidx.compose.ui.text.font.FontWeight.Thin
        ClockStyle.SIDE -> androidx.compose.ui.text.font.FontWeight.Light
        ClockStyle.STANDARD -> androidx.compose.ui.text.font.FontWeight.Medium
    }
    Text("18:45", style = MaterialTheme.typography.titleMedium.copy(fontWeight = weight), color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun <T> Choice(title: Int, value: T, entries: List<T>, labels: List<Int>, preview: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier, onChange: (T) -> Unit) {
    val index = entries.indexOf(value)
    app.stova.core.ui.SettingsChoiceRow(stringResource(title), stringResource(labels[index]), preview, modifier) { step ->
        onChange(entries[Math.floorMod(index + step, entries.size)])
    }
}
