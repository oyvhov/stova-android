package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.CategoryIcon
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline

/** A small, stable front door; navigation and shortcuts open exactly the same destination. */
@Composable
internal fun SettingsOverview(onOpen: (SettingsSection) -> Unit) {
    val sections = listOf(SettingsSection.HOME, SettingsSection.APPEARANCE, SettingsSection.SMART_HOME,
        SettingsSection.ACCOUNTS, SettingsSection.PROFILE, SettingsSection.HOME_BUTTON)
    val columns = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2
    val returnedFrom = LocalSettingsReturnedFrom.current
    val cards = remember { sections.associateWith { FocusRequester() } }
    // Back from a section lands on the card that opened it, not the first card.
    Column(Modifier.focusProperties { onEnter = { returnedFrom?.let(cards::get)?.requestFocus() } }.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.chunked(columns).forEach { band ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                band.forEach { section ->
                    val source = remember { MutableInteractionSource() }
                    val shape = MaterialTheme.shapes.medium
                    Row(Modifier.weight(1f).fillMaxHeight().focusRequester(cards.getValue(section)).focusOutline(source, shape).clip(shape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(source, indication = null, role = Role.Button) { onOpen(section) }
                        .padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(section.icon, tint = section.tone)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(if (section == SettingsSection.APPEARANCE) R.string.settings_overview_appearance else section.pageTitle),
                                style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(section.hint), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(StovaIcons.ChevronRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
