package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.first

@Composable
fun AppearanceSettings(container: AppContainer, profile: Profile, pinnedPreview: Boolean = false, onEditRows: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val packs by container.iconPacks.packs.collectAsStateWithLifecycle()
    val profiles by container.profiles.file.collectAsStateWithLifecycle(initialValue = ProfilesFile())
    var picking by remember(profile.id) { mutableStateOf(false) }
    var draft by remember(profile.id) { mutableStateOf(profile.appearance) }
    var pending by remember(profile.id) { mutableIntStateOf(0) }
    LaunchedEffect(profile.appearance, pending) { if (pending == 0) draft = profile.appearance }
    val previewFlow = remember(profile.id, draft.fixedBackground, draft.backgroundSource) {
        if (draft.backgroundSource == BackgroundSource.FIXED) container.media.fixedBackground(profile.id, draft.fixedBackground)
        else container.media.cachedPreview(profile.id)
    }
    val preview by previewFlow.collectAsStateWithLifecycle(initialValue = null)
    val contents: @Composable () -> Unit = {
        AppearanceSettingsContent(draft, packs, onPickBackground = { picking = true }, onEditRows = onEditRows,
            includePreview = !pinnedPreview, previewEntry = preview, homePrefs = profile.home,
            others = profiles.profiles.filter { it.id != profile.id },
            onReset = { scope.launch { container.profiles.resetAppearance(profile.id) } },
            onCopy = { sourceId -> scope.launch {
                val source = profiles.profiles.firstOrNull { it.id == sourceId } ?: return@launch
                val accounts = container.mediaAccounts.all.first()
                val sourceAccounts = accounts.filter { it.profileId == sourceId }
                val targetAccounts = accounts.filter { it.profileId == profile.id }
                val keys = (source.home.rowOrder + source.home.hiddenRows + source.home.rowFormats.keys).distinct()
                val mapping = keys.mapNotNull { key ->
                    val prefix = key.value.substringBefore(':')
                    val sourceAccount = sourceAccounts.firstOrNull { it.id == key.value.substringAfter(':', "") }
                    val target = sourceAccount?.let { from -> targetAccounts.firstOrNull { it.serverId == from.serverId && it.kind == from.kind } }
                    target?.let { key to app.stova.home.HomeRowKey("$prefix:${it.id}") }
                }.toMap()
                container.profiles.copyAppearance(profile.id, sourceId, mapping)
            } }) { prefs ->
                draft = prefs; pending++
                scope.launch {
                    try { container.profiles.update(profile.id) { it.copy(appearance = prefs) } }
                    finally { pending-- }
                }
            }
    }
    if (pinnedPreview) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemePreview(draft, preview, profile.home)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) { contents() }
    } else contents()
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
    }
    SettingsGroup(stringResource(R.string.custom_home_hero)) {
        GroupHint(R.string.custom_hero_hint)
        Choice(R.string.appearance_hero_size, p.heroSize, HeroSize.entries, listOf(R.string.appearance_off, R.string.appearance_compact, R.string.appearance_large)) { onChange(p.copy(heroSize = it)) }
        Choice(R.string.custom_hero_title, p.heroTitle, HeroTitle.entries, listOf(R.string.custom_logo, R.string.custom_always_title)) { onChange(p.copy(heroTitle = it)) }
        Toggle(R.string.appearance_overview, p.showOverview) { onChange(p.copy(showOverview = it)) }
        Toggle(R.string.appearance_genres, p.showGenres) { onChange(p.copy(showGenres = it)) }
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
            stringResource(R.string.custom_pick_background), p.fixedBackground?.title, onPickBackground)
        Choice(R.string.custom_spotlight_source, p.spotlightSource, SpotlightSource.entries,
            listOf(R.string.custom_whole_library, R.string.custom_only_movies, R.string.custom_only_series, R.string.custom_favorites)) { onChange(p.copy(spotlightSource = it)) }
        Choice(R.string.appearance_dimming, p.backdropDimming, BackdropDimming.entries, listOf(R.string.appearance_light, R.string.appearance_medium, R.string.appearance_dark)) { onChange(p.copy(backdropDimming = it)) }
        Toggle(R.string.custom_background_motion, p.backgroundMotion) { onChange(p.copy(backgroundMotion = it)) }
    }
    SettingsGroup(stringResource(R.string.custom_focus)) {
        GroupHint(R.string.custom_focus_hint)
        Choice(R.string.appearance_focus_effect, p.focusEffect, FocusEffect.entries, listOf(R.string.appearance_ring, R.string.appearance_zoom, R.string.custom_ring_zoom, R.string.custom_light_up)) { onChange(p.copy(focusEffect = it)) }
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
