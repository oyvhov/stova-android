package app.stova.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.focusOutline

/** The title stays put while the section list scrolls, including at font scale 2. */
@Composable
internal fun SettingsNavigation(selected: SettingsSection, onSelect: (SettingsSection) -> Unit, onSystemSettings: () -> Unit) {
    Column(Modifier.width(StovaLayout.SettingsNavigationWidth), verticalArrangement = Arrangement.spacedBy(StovaLayout.Gutter)) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsSection.entries.forEach { entry ->
                SectionTab(stringResource(entry.label), entry == selected, { onSelect(entry) }) { onSelect(entry) }
            }
            SectionTab(stringResource(R.string.nav_system_settings), false, onClick = onSystemSettings)
        }
    }
}

@Composable
private fun SectionTab(label: String, selected: Boolean, onFocused: (() -> Unit)? = null, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Box(Modifier.fillMaxWidth().heightIn(min = StovaLayout.SettingsRowMinHeight)
        .onFocusChanged { if (it.isFocused) onFocused?.invoke() }
        .focusOutline(source, shape).clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .selectable(selected, source, indication = null, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
