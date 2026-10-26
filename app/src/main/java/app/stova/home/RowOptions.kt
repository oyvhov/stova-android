package app.stova.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.SettingsChoiceRow
import app.stova.core.ui.StovaButton
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.PageHeader
import app.stova.core.ui.SectionHeading
import app.stova.core.ui.labelRes
import app.stova.profiles.*

internal val rowFormatLabels = listOf(R.string.appearance_poster, R.string.appearance_wide,
    R.string.custom_large_wide, R.string.custom_small_wide, R.string.appearance_icon)
internal val headingSizeLabels = listOf(R.string.appearance_small, R.string.appearance_medium, R.string.appearance_large)

@Composable
fun RowStyleChoices(style: RowStyle, defaultFormat: RowFormat, globalSize: ArtworkSize, defaultTitle: String = "", hasSource: Boolean = true,
    globalTitles: Boolean, onChange: (RowStyle) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val headingButton = remember { FocusRequester() }
    app.stova.core.ui.SettingsNavRow(stringResource(R.string.custom_edit_heading), value = style.heading(defaultTitle),
        onClick = { editing = true }, modifier = Modifier.focusRequester(headingButton))
    if (editing) RowHeadingEditor(style.title, defaultTitle, onSave = { title ->
        onChange(style.withHeading(title)); editing = false
        runCatching { headingButton.requestFocus() }
    }, onDismiss = { editing = false; runCatching { headingButton.requestFocus() } })
    SettingsChoiceRow(stringResource(R.string.custom_heading_size), stringResource(headingSizeLabels[style.headingSize.ordinal])) { step ->
        onChange(style.copy(headingSize = HeadingSize.entries[Math.floorMod(style.headingSize.ordinal + step, HeadingSize.entries.size)]))
    }
    val format = style.format ?: defaultFormat
    val size = style.size ?: globalSize
    SettingsChoiceRow(stringResource(R.string.custom_row_format), stringResource(rowFormatLabels[format.ordinal]),
        preview = { RowFormatPreview(format) }) { step ->
        onChange(style.copy(format = RowFormat.entries[Math.floorMod(format.ordinal + step, RowFormat.entries.size)]))
    }
    SettingsChoiceRow(stringResource(R.string.custom_row_size), stringResource(size.labelRes)) { step ->
        onChange(style.copy(size = ArtworkSize.entries[Math.floorMod(size.ordinal + step, ArtworkSize.entries.size)]))
    }
    SettingsChoiceRow(stringResource(R.string.custom_row_title), stringResource(if (style.showTitle) R.string.appearance_on else R.string.appearance_off)) {
        onChange(style.copy(showTitle = !style.showTitle))
    }
    val names = style.cardTitles ?: globalTitles
    SettingsChoiceRow(stringResource(R.string.custom_card_titles), stringResource(if (names) R.string.appearance_on else R.string.appearance_off)) {
        // Choosing the profile's own value again inherits it, so later global changes still reach the row.
        onChange(style.copy(cardTitles = (!names).takeUnless { it == globalTitles }))
    }
    if (hasSource) SettingsChoiceRow(stringResource(R.string.custom_row_source),
        stringResource(if (style.showSource) R.string.appearance_on else R.string.appearance_off)) {
        onChange(style.copy(showSource = !style.showSource))
    }
    if (style != RowStyle()) {
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
    var customizing by remember { mutableStateOf(false) }
    val customize = remember { FocusRequester() }
    SectionHeading(stringResource(R.string.custom_row_section), StovaIcons.Rows)
    StovaButton(stringResource(R.string.custom_move_row), onMove, Modifier.fillMaxWidth(), icon = StovaIcons.Rows, compact = true, quiet = true)
    StovaButton(stringResource(R.string.custom_hide_row), onHide, Modifier.fillMaxWidth(), icon = StovaIcons.Hide, compact = true, quiet = true)
    app.stova.core.ui.SettingsNavRow(stringResource(R.string.settings_customize_row), Modifier.focusRequester(customize),
        value = stringResource(R.string.settings_customize_row_hint)) { customizing = true }
    if (customizing) {
        val p = app.stova.core.theme.LocalAppearance.current
        val back = remember { FocusRequester() }
        val close = { customizing = false; runCatching { customize.requestFocus() }; Unit }
        app.stova.core.ui.StovaDialog(close) {
            Column(Modifier.width(440.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StovaButton(stringResource(R.string.custom_back), close, Modifier.focusRequester(back), icon = StovaIcons.ChevronLeft, compact = true, quiet = true)
                PageHeader((prefs.rowFormats[key] ?: RowStyle()).heading(rowLabel(key)), StovaIcons.Rows)
                RowStyleChoices(prefs.rowFormats[key] ?: RowStyle(), defaultRowFormat(key, p), p.cardSize, rowLabel(key),
                    hasSource = key != HomeRowKey.APPS && key != HomeRowKey.WATCH_NEXT, globalTitles = p.showTitles) { style ->
                    onChange(prefs.copy(rowFormats = if (style == RowStyle()) prefs.rowFormats - key else prefs.rowFormats + (key to style)))
                }
            }
            LaunchedEffect(Unit) { back.requestFocus() }
        }
    }
}
