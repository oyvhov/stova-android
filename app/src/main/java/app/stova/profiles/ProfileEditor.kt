package app.stova.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.color
import app.stova.core.ui.RemoteTextField
import app.stova.core.ui.SettingsSwitchRow
import app.stova.core.ui.StovaButton
import app.stova.core.ui.focusOutline

private const val MAX_NAME = 24

/** Name, colour and (when allowed) kid mode. Save is enabled once the name is 1–24 characters. */
@Composable
fun ProfileEditor(
    initial: Profile?,
    allowKid: Boolean,
    onSave: (name: String, avatar: AvatarColor, kid: Boolean) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var avatar by remember { mutableStateOf(initial?.avatar ?: AvatarColor.LIME) }
    var kid by remember { mutableStateOf(initial?.kid ?: false) }
    val valid = name.trim().length in 1..MAX_NAME

    Column(modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        RemoteTextField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME) },
            label = stringResource(R.string.profile_name),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.profile_avatar), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarColor.entries.forEach { color ->
                AvatarSwatch(color, selected = color == avatar, onClick = { avatar = color })
            }
        }
        if (allowKid) {
            SettingsSwitchRow(stringResource(R.string.profile_kid), kid, { kid = it }, stringResource(R.string.profile_kid_hint))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StovaButton(stringResource(R.string.profile_save), { onSave(name.trim(), avatar, kid) }, primary = true, enabled = valid)
            if (onCancel != null) StovaButton(stringResource(R.string.profile_cancel), onCancel)
        }
    }
}

@Composable
private fun AvatarSwatch(color: AvatarColor, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val label = stringResource(avatarName(color))
    Box(
        Modifier
            .size(48.dp)
            .focusOutline(source, CircleShape)
            .selectable(selected, source, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp)
                .background(color.color, CircleShape)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
        )
    }
}

private fun avatarName(color: AvatarColor): Int = when (color) {
    AvatarColor.LIME -> R.string.avatar_lime
    AvatarColor.OCEAN -> R.string.avatar_ocean
    AvatarColor.IRIS -> R.string.avatar_iris
    AvatarColor.CORAL -> R.string.avatar_coral
    AvatarColor.GOLD -> R.string.avatar_gold
    AvatarColor.MINT -> R.string.avatar_mint
    AvatarColor.ROSE -> R.string.avatar_rose
    AvatarColor.PEARL -> R.string.avatar_pearl
}
