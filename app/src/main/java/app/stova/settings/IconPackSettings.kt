package app.stova.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.apps.IconPack
import app.stova.core.ui.SettingsChoiceRow
import app.stova.core.ui.SettingsGroup
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.IconColor
import app.stova.profiles.IconFill

@Composable
fun IconPackSettingsContent(p: AppearancePrefs, packs: List<IconPack>, onChange: (AppearancePrefs) -> Unit) {
    SettingsGroup(stringResource(R.string.icon_pack_group)) {
        val choices = listOf<String?>(null) + packs.map { it.packageName } + listOfNotNull(p.iconPack?.takeIf { saved -> packs.none { it.packageName == saved } })
        val index = choices.indexOf(p.iconPack)
        val none = stringResource(R.string.icon_pack_none)
        val unavailable = stringResource(R.string.icon_pack_unavailable, p.iconPack.orEmpty())
        val label = packs.firstOrNull { it.packageName == p.iconPack }?.label ?: if (p.iconPack == null) none else unavailable
        SettingsChoiceRow(stringResource(R.string.icon_pack), label) { step ->
            onChange(p.copy(iconPack = choices[Math.floorMod(index + step, choices.size)]))
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
