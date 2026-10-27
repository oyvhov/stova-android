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
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.color
import app.stova.core.ui.CategoryIcon
import app.stova.core.ui.focusOutline
import app.stova.core.ui.StovaIcons

/** The title stays put while the section list scrolls, including at font scale 2. */
@Composable
internal fun SettingsNavigation(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    sectionFocus: Map<SettingsSection, FocusRequester> = emptyMap(),
    onEnterContent: ((SettingsSection) -> Unit)? = null,
    onCancelEntry: () -> Unit = {},
    onApps: (() -> Unit)? = null,
) {
    val fallbackFocus = remember { SettingsSection.entries.associateWith { FocusRequester() } }
    val tabs = sectionFocus.ifEmpty { fallbackFocus }
    val apps = remember { FocusRequester() }
    // Apps opens its own page, so it does not select on focus. It sits just above System; null marks it.
    val entries: List<SettingsSection?> = settingsOrder.toMutableList<SettingsSection?>().apply {
        if (onApps != null) add(indexOf(SettingsSection.HOME_BUTTON), null)
    }
    val requesters = entries.map { entry -> entry?.let { tabs.getValue(it) } ?: apps }
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
        }.focusGroup().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEachIndexed { index, entry ->
                val order = Modifier.focusRequester(requesters[index]).focusProperties {
                    up = requesters.getOrNull(index - 1) ?: FocusRequester.Cancel
                    down = requesters.getOrNull(index + 1) ?: FocusRequester.Cancel
                    left = FocusRequester.Cancel
                    if (entry == null) right = FocusRequester.Cancel
                }
                if (entry == null) SectionTab(stringResource(R.string.nav_apps), stringResource(R.string.settings_rail_apps), false,
                    modifier = order, icon = StovaIcons.Apps, iconTint = AvatarColor.ROSE.color, iconTag = "settings-icon-apps", onClick = onApps ?: {})
                else SectionTab(stringResource(entry.label), stringResource(entry.railHint), entry == selected, { onSelect(entry) },
                    modifier = order.onPreviewKeyEvent { event ->
                        if (event.key != Key.DirectionRight) false else {
                            if (event.type == KeyEventType.KeyDown) onEnterContent?.invoke(entry)
                            true
                        }
                    }, icon = entry.icon, iconTint = entry.tone, iconTag = "settings-icon-${entry.name}") {
                    if (onEnterContent != null) onEnterContent(entry) else onSelect(entry)
                }
            }
        }
    }
}

@Composable
private fun SectionTab(label: String, hint: String, selected: Boolean, onFocused: (() -> Unit)? = null, modifier: Modifier = Modifier,
    icon: ImageVector = StovaIcons.Gear, iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconTag: String = "settings-icon-system", onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Row(modifier.fillMaxWidth().heightIn(min = StovaLayout.SettingsNavigationMinHeight)
        .onFocusChanged { if (it.isFocused) onFocused?.invoke() }
        .focusOutline(source, shape).clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .selectable(selected, source, indication = null, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // The same tinted badge as the section's page header and the look categories.
        CategoryIcon(icon, Modifier.testTag(iconTag), iconTint, StovaLayout.CategoryIconSize)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
