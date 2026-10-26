package app.stova.ha.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.cards.*

@Composable
internal fun DashboardLayoutDialog(prefs: DashboardPrefs, onChange: (DashboardPrefs) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(prefs) }
    fun change(value: DashboardPrefs) { draft = value; onChange(value) }
    StovaDialog(onDismiss) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 440.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageHeader(stringResource(R.string.ha_layout), StovaIcons.Rows)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsChoiceRow(stringResource(R.string.ha_grouping), stringResource(
                    if (draft.grouping == DashboardGrouping.NONE) R.string.ha_group_none else R.string.ha_group_room)) {
                    change(draft.copy(grouping = if (draft.grouping == DashboardGrouping.NONE) DashboardGrouping.ROOM else DashboardGrouping.NONE))
                }
                Text(stringResource(R.string.ha_density), style = MaterialTheme.typography.titleSmall)
                DashboardDensity.entries.forEach { density ->
                    LayoutChoice(stringResource(when (density) {
                        DashboardDensity.COMPACT -> R.string.ha_density_compact
                        DashboardDensity.BALANCED -> R.string.ha_density_balanced
                        DashboardDensity.AIRY -> R.string.ha_density_airy
                    }), draft.density == density, columns = when (density) {
                        DashboardDensity.COMPACT -> 5; DashboardDensity.BALANCED -> 4; DashboardDensity.AIRY -> 3
                    }, rows = 2) { change(draft.copy(density = density)) }
                }
                Text(stringResource(R.string.ha_layout_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StovaButton(stringResource(R.string.ha_done), onDismiss, compact = true)
        }
    }
}

/** A vector silhouette of the footprint, not another abstract S/M/L switch. */
@Composable
internal fun LayoutChoice(label: String, chosen: Boolean, columns: Int, rows: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().semantics { selected = chosen }.focusOutline(interaction, shape).clip(shape)
        .background(if (chosen) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
        .clickable(interaction, indication = null, role = Role.RadioButton, onClick = onClick).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(72.dp, 32.dp)) {
            val gap = 3.dp.toPx()
            val unit = minOf((size.width - (columns - 1) * gap) / columns, (size.height - (rows - 1) * gap) / rows)
            val start = (size.width - (unit * columns + gap * (columns - 1))) / 2
            for (r in 0 until rows) for (c in 0 until columns) drawRoundRect(ink.copy(alpha = 0.6f),
                Offset(start + c * (unit + gap), r * (unit + gap)), Size(unit, unit), CornerRadius(3.dp.toPx()))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        if (chosen) Icon(StovaIcons.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
