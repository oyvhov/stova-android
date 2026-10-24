package app.stova.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.StovaShapes
import app.stova.core.ui.focusOutline
import app.stova.profiles.AvatarBadge
import app.stova.profiles.Profile

/**
 * Spole's side rail: an 80 dp icon column that opens to 200 dp with labels while it holds focus.
 * Only the width changes; the icon column never moves. Each destination is a Tab whose label stays
 * in the semantics when collapsed, so a screen reader always hears its name.
 */
@Composable
fun SideRail(
    destinations: List<Destination>,
    current: Destination,
    profile: Profile,
    onSelect: (Destination) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) StovaLayout.RailExpanded else StovaLayout.RailCollapsed,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else RAIL_MILLIS, easing = FastOutSlowInEasing),
        label = "rail-width",
    )
    Column(
        modifier
            .fillMaxHeight()
            .width(width)
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { expanded = it.hasFocus }
            .focusRestorer()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RailItem(stringResource(R.string.nav_profile), selected = false, tab = false, expanded = expanded, onClick = onProfile) {
            AvatarBadge(profile, 32.dp)
        }
        Spacer(Modifier.height(16.dp))
        destinations.forEach { destination ->
            val selected = destination == current
            RailItem(stringResource(destination.label), selected, tab = true, expanded = expanded, onClick = { onSelect(destination) }) {
                Icon(
                    destination.icon(),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    label: String,
    selected: Boolean,
    tab: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(StovaShapes.Large)
    val action = if (tab) {
        Modifier.selectable(selected, source, indication = null, role = Role.Tab, onClick = onClick)
    } else {
        Modifier.clickable(source, indication = null, role = Role.Button, onClick = onClick)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .focusOutline(source, shape)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(action)
            .semantics { text = AnnotatedString(label) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { leading() }
        if (expanded) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private const val RAIL_MILLIS = 220
