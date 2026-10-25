package app.stova.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.SettingsChoiceRow
import app.stova.core.ui.StovaButton
import app.stova.profiles.*

internal val rowFormatLabels = listOf(R.string.appearance_poster, R.string.appearance_wide,
    R.string.custom_large_wide, R.string.custom_small_wide, R.string.appearance_icon)
internal val sizeLabels = listOf(R.string.appearance_small, R.string.appearance_medium, R.string.appearance_large)

@Composable
fun RowStyleChoices(style: RowStyle, defaultFormat: RowFormat, globalSize: ArtworkSize, onChange: (RowStyle) -> Unit) {
    val format = style.format ?: defaultFormat
    val size = style.size ?: globalSize
    SettingsChoiceRow(stringResource(R.string.custom_row_format), stringResource(rowFormatLabels[format.ordinal]),
        preview = { RowFormatPreview(format) }) { step ->
        onChange(style.copy(format = RowFormat.entries[Math.floorMod(format.ordinal + step, RowFormat.entries.size)]))
    }
    SettingsChoiceRow(stringResource(R.string.custom_row_size), stringResource(sizeLabels[size.ordinal])) { step ->
        onChange(style.copy(size = ArtworkSize.entries[Math.floorMod(size.ordinal + step, ArtworkSize.entries.size)]))
    }
    SettingsChoiceRow(stringResource(R.string.custom_row_title), stringResource(if (style.showTitle) R.string.appearance_on else R.string.appearance_off)) {
        onChange(style.copy(showTitle = !style.showTitle))
    }
    if (style.format != null || style.size != null || !style.showTitle) {
        StovaButton(stringResource(R.string.custom_row_inherit), { onChange(RowStyle()) })
    }
}

@Composable
internal fun RowFormatPreview(format: RowFormat) {
    val d = artworkDimensions(format, ArtworkSize.MEDIUM)
    Box(Modifier.size((d.width * 0.14f).dp, (d.height * 0.14f).dp)
        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small))
}

@Composable
internal fun RowMenuSection(key: HomeRowKey, prefs: HomePrefs, onChange: (HomePrefs) -> Unit, onMove: () -> Unit, onHide: () -> Unit) {
    Text(stringResource(R.string.custom_row_section), style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    StovaButton(stringResource(R.string.custom_move_row), onMove, Modifier.fillMaxWidth())
    StovaButton(stringResource(R.string.custom_hide_row), onHide, Modifier.fillMaxWidth())
    val p = app.stova.core.theme.LocalAppearance.current
    RowStyleChoices(prefs.rowFormats[key] ?: RowStyle(), defaultRowFormat(key, p), p.cardSize) { style ->
        onChange(prefs.copy(rowFormats = if (style == RowStyle()) prefs.rowFormats - key else prefs.rowFormats + (key to style)))
    }
}
