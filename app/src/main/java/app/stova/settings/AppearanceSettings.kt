package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import app.stova.AppContainer
import app.stova.BuildConfig
import app.stova.R
import app.stova.core.theme.*
import app.stova.core.ui.SettingsGroup
import app.stova.core.ui.SettingsSwitchRow
import app.stova.profiles.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

@Composable
fun AppearanceSettings(container: AppContainer, profile: Profile) {
    val scope = rememberCoroutineScope()
    val packs by container.iconPacks.packs.collectAsStateWithLifecycle()
    var picking by remember(profile.id) { mutableStateOf(false) }
    AppearanceSettingsContent(profile.appearance, packs, onPickBackground = { picking = true }) { prefs ->
        scope.launch { container.profiles.update(profile.id) { it.copy(appearance = prefs) } }
    }
    if (picking) BackgroundPicker({ container.media.searchBackgrounds(profile.id, it) }, { entry ->
        picking = false
        scope.launch {
            container.media.rememberBackground(profile.id, entry)
            container.profiles.update(profile.id) { it.copy(appearance = it.appearance.copy(backgroundSource = BackgroundSource.FIXED,
                fixedBackground = BackgroundTitle(entry.account.id, entry.item.id, entry.item.title))) }
        }
    }, { picking = false })
}

@Composable
fun AppearanceSettingsContent(p: AppearancePrefs, packs: List<app.stova.apps.IconPack> = emptyList(), onPickBackground: () -> Unit = {}, onChange: (AppearancePrefs) -> Unit) {
    Text(stringResource(R.string.appearance_choice_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    SettingsGroup(stringResource(R.string.theme_group)) {
        Choice(R.string.theme_mood, p.visualTheme, VisualTheme.entries, themeLabels,
            preview = { ThemeSwatches(p.visualTheme) }) { theme ->
            onChange(when(theme) {
                VisualTheme.NOEL -> SeasonChoice.CHRISTMAS.applyTo(p)
                VisualTheme.HALLOWEEN -> SeasonChoice.HALLOWEEN.applyTo(p)
                else -> p.copy(visualTheme=theme, seasonChoice=SeasonChoice.NONE, seasonOverlay=null)
            })
        }
        Choice(R.string.appearance_accent, p.accent, Accent.entries, accentLabels,
            preview = { AccentSwatches(p.accent) }) { onChange(p.copy(accent = it)) }
        Choice(R.string.theme_season, p.seasonChoice, SeasonChoice.entries,
            listOf(R.string.theme_none,R.string.theme_calendar,R.string.theme_christmas,R.string.theme_halloween)) { onChange(it.applyTo(p)) }
        Choice(R.string.theme_decor, p.seasonalDecor, SeasonalDecor.entries,
            listOf(R.string.appearance_off,R.string.theme_calm,R.string.theme_full)) { onChange(p.copy(seasonalDecor=it)) }
        ThemePreview(p)
    }
    IconPackSettingsContent(p, packs, onChange)
    SettingsGroup(stringResource(R.string.settings_home)) {
        Choice(R.string.appearance_hero_size, p.heroSize, HeroSize.entries, listOf(R.string.appearance_off, R.string.appearance_compact, R.string.appearance_large)) { onChange(p.copy(heroSize = it)) }
        Choice(R.string.custom_hero_title, p.heroTitle, HeroTitle.entries, listOf(R.string.custom_logo, R.string.custom_always_title)) { onChange(p.copy(heroTitle = it)) }
        Toggle(R.string.appearance_overview, p.showOverview) { onChange(p.copy(showOverview = it)) }
        Toggle(R.string.appearance_genres, p.showGenres) { onChange(p.copy(showGenres = it)) }
        Choice(R.string.appearance_spotlight_time, p.spotlightTime, SpotlightTime.entries, listOf(R.string.appearance_8s, R.string.appearance_12s, R.string.appearance_20s)) { onChange(p.copy(spotlightTime = it)) }
        Choice(R.string.appearance_row_spacing, p.rowSpacing, RowSpacing.entries, listOf(R.string.appearance_tight, R.string.appearance_normal, R.string.appearance_airy)) { onChange(p.copy(rowSpacing = it)) }
    }
    SettingsGroup(stringResource(R.string.appearance_cards)) {
        Choice(R.string.appearance_card_size, p.cardSize, ArtworkSize.entries, listOf(R.string.appearance_small, R.string.appearance_medium, R.string.appearance_large)) { onChange(p.copy(cardSize = it)) }
        Toggle(R.string.appearance_titles, p.showTitles) { onChange(p.copy(showTitles = it)) }
        Choice(R.string.appearance_app_style, p.appStyle, AppStyle.entries, listOf(R.string.appearance_banner, R.string.appearance_icon)) { onChange(p.copy(appStyle = it)) }
        Choice(R.string.row_latest_movies, p.movieStyle, MovieStyle.entries, listOf(R.string.appearance_poster, R.string.appearance_wide)) { onChange(p.copy(movieStyle = it)) }
        Choice(R.string.appearance_corners, p.corners, Corners.entries, listOf(R.string.appearance_round, R.string.appearance_sharp)) { onChange(p.copy(corners = it)) }
    }
    SettingsGroup(stringResource(R.string.appearance_background)) {
        Choice(R.string.custom_background_source, p.backgroundSource, BackgroundSource.entries,
            listOf(R.string.custom_spotlight, R.string.custom_fixed_image, R.string.custom_theme_surface)) { onChange(p.copy(backgroundSource = it)) }
        if (p.backgroundSource == BackgroundSource.FIXED) app.stova.core.ui.SettingsNavRow(
            stringResource(R.string.custom_pick_background), p.fixedBackground?.title, onPickBackground)
        Choice(R.string.custom_spotlight_source, p.spotlightSource, SpotlightSource.entries,
            listOf(R.string.custom_whole_library, R.string.custom_only_movies, R.string.custom_only_series, R.string.custom_favorites)) { onChange(p.copy(spotlightSource = it)) }
        Choice(R.string.appearance_dimming, p.backdropDimming, BackdropDimming.entries, listOf(R.string.appearance_light, R.string.appearance_medium, R.string.appearance_dark)) { onChange(p.copy(backdropDimming = it)) }
        Toggle(R.string.custom_background_motion, p.backgroundMotion) { onChange(p.copy(backgroundMotion = it)) }
    }
    SettingsGroup(stringResource(R.string.appearance_color_focus)) {
        Choice(R.string.appearance_focus_effect, p.focusEffect, FocusEffect.entries, listOf(R.string.appearance_ring, R.string.appearance_zoom)) { onChange(p.copy(focusEffect = it)) }
        SettingsSwitchRow(stringResource(R.string.appearance_reduce_motion), p.reduceMotion,
            { onChange(p.copy(reduceMotion = it)) }, stringResource(R.string.appearance_reduce_motion_hint))
        SettingsSwitchRow(stringResource(R.string.appearance_strong_focus), p.strongFocus,
            { onChange(p.copy(strongFocus = it)) }, stringResource(R.string.appearance_strong_focus_hint))
    }
    SettingsGroup(stringResource(R.string.appearance_clock)) {
        Choice(R.string.appearance_clock_format, p.clock, ClockMode.entries, listOf(R.string.appearance_24h, R.string.appearance_12h, R.string.appearance_hidden)) { onChange(p.copy(clock = it)) }
    }
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

@Composable
fun AboutSettings() {
    SettingsGroup(stringResource(R.string.settings_about)) {
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
