package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import app.stova.R
import app.stova.apps.IconPack
import app.stova.core.ui.SettingsChoiceRow
import app.stova.core.ui.SettingsGroup
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.IconColor
import app.stova.profiles.IconFill
import app.stova.profiles.appBackgroundPercent
import app.stova.core.ui.SettingsSliderRow

@Composable
fun IconPackSettingsContent(p: AppearancePrefs, packs: List<IconPack>, groupTitle: String? = null, description: String? = null, firstControl: Modifier = Modifier, showHeading: Boolean = true, onChange: (AppearancePrefs) -> Unit) {
    SettingsGroup(groupTitle ?: stringResource(R.string.icon_pack_group), showHeading) {
        if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val choices = listOf<String?>(null) + packs.map { it.packageName } + listOfNotNull(p.iconPack?.takeIf { saved -> packs.none { it.packageName == saved } })
        val index = choices.indexOf(p.iconPack)
        val none = stringResource(R.string.icon_pack_none)
        val unavailable = stringResource(R.string.icon_pack_unavailable, p.iconPack.orEmpty())
        val label = packs.firstOrNull { it.packageName == p.iconPack }?.label ?: if (p.iconPack == null) none else unavailable
        SettingsChoiceRow(stringResource(R.string.icon_pack), label, modifier = firstControl) { step ->
            val selected = choices[Math.floorMod(index + step, choices.size)]
            onChange(if (selected == app.stova.apps.ProjectivyIcons.PACKAGE)
                p.copy(iconPack = selected, appTileBackground = false, appTileBackgroundOpacity = 0)
            else p.copy(iconPack = selected))
        }
        SettingsSliderRow(stringResource(R.string.icon_tile_background), p.appBackgroundPercent, 0..100,
            stringResource(R.string.slider_percent, p.appBackgroundPercent)) {
            onChange(p.copy(appTileBackground = it > 0, appTileBackgroundOpacity = it))
        }
        SettingsChoiceRow(stringResource(R.string.icon_color), stringResource(if (p.iconColor == IconColor.ORIGINAL) R.string.icon_original else R.string.icon_accent)) {
            onChange(p.copy(iconColor = if (p.iconColor == IconColor.ORIGINAL) IconColor.ACCENT else IconColor.ORIGINAL))
        }
        SettingsChoiceRow(stringResource(R.string.icon_fill), stringResource(if (p.iconFill == IconFill.CENTER) R.string.icon_center else R.string.icon_crop)) {
            onChange(p.copy(iconFill = if (p.iconFill == IconFill.CENTER) IconFill.CROP else IconFill.CENTER))
        }
        Text(stringResource(R.string.icon_pack_recommended), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
