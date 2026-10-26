package app.stova.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.PageHeader
import app.stova.core.ui.CategoryIcon
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.focusOutline
import app.stova.core.ui.softEntrance

enum class AppearanceCategory(@StringRes val title: Int, @StringRes val hint: Int) {
    THEME(R.string.theme_group, R.string.settings_summary_theme),
    HERO(R.string.custom_home_hero, R.string.settings_summary_hero),
    CARDS(R.string.custom_cards_rows, R.string.settings_summary_cards),
    BACKGROUND(R.string.appearance_background, R.string.settings_summary_background),
    FOCUS(R.string.custom_focus, R.string.settings_summary_focus),
    TOP_BAR(R.string.custom_top_bar, R.string.settings_summary_top),
    ICONS(R.string.custom_icons, R.string.settings_summary_icons),
    PROFILE(R.string.settings_profile, R.string.settings_summary_profile),
}

/** One small page at a time. The persistent breadcrumb protects focus while a page is replaced. */
@Composable
internal fun AppearanceMenu(scrollable: Boolean = true, content: @Composable (AppearanceCategory, Modifier) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<AppearanceCategory?>(null) }
    var last by rememberSaveable { mutableStateOf(AppearanceCategory.THEME) }
    var transferring by remember { mutableStateOf(false) }
    val breadcrumb = remember { FocusRequester() }
    val controls = remember { FocusRequester() }
    val categories = remember { AppearanceCategory.entries.associateWith { FocusRequester() } }
    val sidebar = LocalSettingsSectionFocus.current
    fun show(category: AppearanceCategory?) {
        transferring = true
        (sidebar ?: if (selected != null) breadcrumb else categories.getValue(last)).requestFocus()
        if (category != null) last = category
        selected = category
    }
    BackHandler(selected != null) { show(null) }
    LaunchedEffect(selected, transferring) {
        if (!transferring) return@LaunchedEffect
        repeat(8) {
            withFrameNanos { }
            val target = if (selected == null) categories.getValue(last) else controls
            val accepted = runCatching { target.requestFocus() }.getOrDefault(false)
            if (accepted) {
                transferring = false
                return@LaunchedEffect
            }
        }
        transferring = false
    }
    Column(Modifier.fillMaxWidth().onKeyEvent { event ->
        if (selected != null && event.key in listOf(Key.Back, Key.Escape)) {
            if (event.type == KeyEventType.KeyUp) show(null)
            true
        } else false
    }.focusProperties {
        onEnter = {
            if (!transferring) {
                if (selected == null) categories.getValue(last).requestFocus()
                else controls.requestFocus()
            }
        }
    }.focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selected != null) {
            StovaButton(stringResource(R.string.settings_all_appearance), { show(null) },
                Modifier.focusRequester(breadcrumb), icon = StovaIcons.ChevronLeft, compact = true, quiet = true)
        }
        key(selected) {
            val scroll = rememberScrollState()
            Column(Modifier.fillMaxWidth().softEntrance(selected).focusProperties {
                onExit = { if (requestedFocusDirection == FocusDirection.Left) sidebar?.requestFocus() }
            }.focusGroup().then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val category = selected
                if (category == null) {
                    PageHeader(stringResource(R.string.settings_appearance), StovaIcons.Palette,
                        subtitle = stringResource(R.string.settings_appearance_intro), tint = AppearanceCategory.THEME.tone)
                    // Large text gets one column; no fixed heights squeeze labels or descriptions.
                    val columns = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2
                    AppearanceCategory.entries.chunked(columns).forEach { band ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            band.forEach { entry ->
                                val source = remember { MutableInteractionSource() }
                                val shape = MaterialTheme.shapes.medium
                                Row(Modifier.weight(1f).focusRequester(categories.getValue(entry))
                                    .focusOutline(source, shape).clip(shape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(source, indication = null, role = Role.Button) { show(entry) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CategoryIcon(entry.icon, Modifier.testTag("appearance-icon-${entry.name}"),
                                        entry.tone, StovaLayout.CategoryIconSize)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(entry.title), style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(entry.hint), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(StovaIcons.ChevronRight, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PageHeader(stringResource(category.title), category.icon, subtitle = stringResource(category.hint),
                            tint = category.tone, iconModifier = Modifier.testTag("appearance-header-icon-${category.name}"))
                        content(category, Modifier.focusRequester(controls))
                    }
                }
            }
        }
    }
}
