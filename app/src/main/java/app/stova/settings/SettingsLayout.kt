package app.stova.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.softEntrance

internal val LocalSettingsSectionFocus = staticCompositionLocalOf<FocusRequester?> { null }
internal val LocalSettingsPaneEntry = staticCompositionLocalOf<() -> Unit> { {} }
/** The section Back just came from, so the page that linked to it can focus that link again. */
internal val LocalSettingsReturnedFrom = staticCompositionLocalOf<SettingsSection?> { null }

/**
 * Two explicit focus areas: arrows never choose a neighbouring section by screen geometry.
 * Back goes one step at a time: a section opened from a card or link returns there, content
 * returns to the section list, and only the list leaves Settings. Subpages handle their own Back first.
 */
@Composable
internal fun SettingsLayout(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    onSystemSettings: () -> Unit,
    onApps: (() -> Unit)? = null,
    content: @Composable ColumnScope.(SettingsSection, (SettingsSection) -> Unit) -> Unit,
) {
    val tabs = remember { SettingsSection.entries.associateWith { FocusRequester() } }
    val pane = remember { FocusRequester() }
    var enterSection by remember { mutableStateOf<SettingsSection?>(null) }
    var history by remember { mutableStateOf(emptyList<SettingsSection>()) }
    var returnedFrom by remember { mutableStateOf<SettingsSection?>(null) }
    var paneFocused by remember { mutableStateOf(false) }
    // Choosing a section in the list starts afresh: there is nothing to go back to.
    fun select(section: SettingsSection) { enterSection = null; history = emptyList(); returnedFrom = null; onSelect(section) }
    fun enter(section: SettingsSection) {
        // Cross-links can remove the currently focused control. Park focus on the stable
        // destination tab first; otherwise Compose falls back to the first tab (Profile),
        // whose onFocused selects Profile before the new pane has attached.
        tabs.getValue(section).requestFocus()
        onSelect(section)
        enterSection = section
    }
    // Parking focus on the tab selects it and clears the history, so both are set afterwards.
    fun follow(section: SettingsSection) {
        val from = selected
        val trail = history
        enter(section)
        if (section != from) history = trail + from
    }
    BackHandler(enabled = history.isNotEmpty() || paneFocused) {
        val from = selected
        val trail = history
        if (trail.isEmpty()) tabs.getValue(from).requestFocus()
        else {
            enter(trail.last())
            history = trail.dropLast(1)
            returnedFrom = from
        }
    }

    LaunchedEffect(Unit) { tabs.getValue(selected).requestFocus() }
    LaunchedEffect(enterSection, selected) {
        if (enterSection != selected) return@LaunchedEffect
        // A quick Right may arrive in the same frame that selected the new section.
        repeat(8) {
            withFrameNanos { }
            if (enterSection != selected) return@LaunchedEffect
            if (runCatching { pane.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) {
                enterSection = null
                return@LaunchedEffect
            }
        }
        enterSection = null // About has no controls: keep focus on its tab.
    }

    Row(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        SettingsNavigation(selected, ::select, onSystemSettings, tabs, ::enter,
            onCancelEntry = { enterSection = null }, onApps = onApps)
        // Keep only the selected pane in the focus tree. An outgoing animated pane must not
        // compete for Up/Down while the user is already selecting the next section.
        key(selected) {
            Column(
                Modifier.weight(1f).fillMaxHeight().softEntrance(selected)
                    .onFocusChanged { paneFocused = it.hasFocus }
                    .focusRequester(pane)
                    .focusProperties {
                        onExit = {
                            when (requestedFocusDirection) {
                                FocusDirection.Left -> tabs.getValue(selected).requestFocus()
                                FocusDirection.Up, FocusDirection.Down -> cancelFocusChange()
                                else -> Unit
                            }
                        }
                    }
                    .focusGroup()
                    .then(if (selected == SettingsSection.APPEARANCE) Modifier else Modifier.verticalScroll(rememberScrollState())),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                CompositionLocalProvider(LocalSettingsSectionFocus provides tabs.getValue(selected),
                    LocalSettingsPaneEntry provides { enter(selected) }, LocalSettingsReturnedFrom provides returnedFrom) {
                    content(selected, ::follow)
                }
            }
        }
    }
}
