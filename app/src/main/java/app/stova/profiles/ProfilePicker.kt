package app.stova.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline

/** "Who's watching?" — one large avatar per profile, a lock under the ones with a PIN. */
@Composable
fun ProfilePicker(profiles: List<Profile>, onPick: (Profile) -> Unit, onAdd: (() -> Unit)?) {
    val first = remember { FocusRequester() }
    LaunchedEffect(profiles.isNotEmpty()) { if (profiles.isNotEmpty()) runCatching { first.requestFocus() } }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Text(stringResource(R.string.profile_who), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            items(profiles, key = { it.id }) { profile ->
                val modifier = if (profile == profiles.first()) Modifier.focusRequester(first) else Modifier
                PickerItem(profile, { onPick(profile) }, modifier)
            }
            if (onAdd != null) {
                item(key = "add") { AddItem(onAdd) }
            }
        }
    }
}

@Composable
private fun PickerItem(profile: Profile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier.widthIn(max = 160.dp).clickable(source, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.focusOutline(source, CircleShape)) { AvatarBadge(profile, 120.dp) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                profile.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (profile.pin != null) {
                Icon(
                    StovaIcons.Lock,
                    contentDescription = stringResource(R.string.profile_locked),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AddItem(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Column(
        Modifier.clickable(source, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(120.dp).focusOutline(source, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(StovaIcons.Plus, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        }
        Text(stringResource(R.string.profile_add), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
