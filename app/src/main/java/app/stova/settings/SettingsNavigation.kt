package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.focusOutline
import app.stova.core.ui.StovaIcons

/** The title stays put while the section list scrolls, including at font scale 2. */
@Composable
internal fun SettingsNavigation(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    onSystemSettings: () -> Unit,
    sectionFocus: Map<SettingsSection, FocusRequester> = emptyMap(),
    onEnterContent: ((SettingsSection) -> Unit)? = null,
    onCancelEntry: () -> Unit = {},
    onApps: (() -> Unit)? = null,
) {
    val fallbackFocus = remember { SettingsSection.entries.associateWith { FocusRequester() } }
    val tabs = sectionFocus.ifEmpty { fallbackFocus }
    val apps = remember { FocusRequester() }
    val system = remember { FocusRequester() }
    // Apps and Android open their own pages, so they follow the sections instead of selecting on focus.
    val requesters = settingsOrder.map { tabs.getValue(it) } + listOfNotNull(apps.takeIf { onApps != null }, system)
    Column(Modifier.width(StovaLayout.SettingsNavigationWidth), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown &&
                (event.key == Key.DirectionLeft || event.key == Key.DirectionUp || event.key == Key.DirectionDown))
                onCancelEntry()
            false
        }.focusProperties {
            onExit = { if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) cancelFocusChange() }
        }.focusGroup().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            settingsOrder.forEachIndexed { index, entry ->
                settingsGroups.firstOrNull { it.second.first() == entry }?.let { group ->
                    Text(stringResource(group.first), Modifier.padding(start = 12.dp, top = if (index == 0) 0.dp else 8.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionTab(stringResource(entry.label), entry == selected, { onSelect(entry) },
                    modifier = Modifier.focusRequester(requesters[index]).focusProperties {
                        up = requesters.getOrNull(index - 1) ?: FocusRequester.Cancel
                        down = requesters[index + 1]
                        left = FocusRequester.Cancel
                    }.onPreviewKeyEvent { event ->
                        if (event.key != Key.DirectionRight) false else {
                            if (event.type == KeyEventType.KeyDown) onEnterContent?.invoke(entry)
                            true
                        }
                    }, icon = entry.icon, iconTint = entry.tone, iconTag = "settings-icon-${entry.name}") {
                    if (onEnterContent != null) onEnterContent(entry) else onSelect(entry)
                }
            }
            if (onApps != null) SectionTab(stringResource(R.string.nav_apps), false,
                modifier = Modifier.focusRequester(apps).focusProperties {
                    up = requesters[settingsOrder.lastIndex]
                    down = system
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                }, icon = StovaIcons.Apps, iconTag = "settings-icon-apps", onClick = onApps)
            SectionTab(stringResource(R.string.nav_system_settings), false,
                modifier = Modifier.focusRequester(system).focusProperties {
                    up = requesters[requesters.lastIndex - 1]
                    down = FocusRequester.Cancel
                    left = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                }, onClick = onSystemSettings)
        }
    }
}

@Composable
private fun SectionTab(label: String, selected: Boolean, onFocused: (() -> Unit)? = null, modifier: Modifier = Modifier,
    icon: ImageVector = StovaIcons.Gear, iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconTag: String = "settings-icon-system", onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Row(modifier.fillMaxWidth().heightIn(min = StovaLayout.SettingsNavigationMinHeight)
        .onFocusChanged { if (it.isFocused) onFocused?.invoke() }
        .focusOutline(source, shape).clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .selectable(selected, source, indication = null, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp).testTag(iconTag), tint = iconTint)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
