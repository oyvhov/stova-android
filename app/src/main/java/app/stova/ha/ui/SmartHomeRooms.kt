package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.HaRegistry
import app.stova.ha.cards.CardConfig

internal sealed interface SmartHomeRoom {
    data object All : SmartHomeRoom
    data object Favourites : SmartHomeRoom
    data class Named(val name: String) : SmartHomeRoom
    data object Other : SmartHomeRoom
}

internal fun smartHomeRooms(cards: List<CardConfig>, registry: HaRegistry): List<SmartHomeRoom> = buildList {
    add(SmartHomeRoom.All)
    if (cards.any { it.favorite }) add(SmartHomeRoom.Favourites)
    val names = cards.map { registry.entities[it.entityId]?.room.orEmpty().trim() }
    names.filter { it.isNotBlank() }.distinct().sorted().forEach { add(SmartHomeRoom.Named(it)) }
    if (names.any { it.isBlank() }) add(SmartHomeRoom.Other)
}

internal fun roomCards(cards: List<CardConfig>, registry: HaRegistry, room: SmartHomeRoom): List<CardConfig> =
    cards.filter { card -> when (room) {
        SmartHomeRoom.All -> true
        SmartHomeRoom.Favourites -> card.favorite
        SmartHomeRoom.Other -> registry.entities[card.entityId]?.room.isNullOrBlank()
        is SmartHomeRoom.Named -> registry.entities[card.entityId]?.room?.trim() == room.name
    } }

@Composable internal fun roomLabel(room: SmartHomeRoom): String = when (room) {
    SmartHomeRoom.All -> stringResource(R.string.ha_all_rooms)
    SmartHomeRoom.Favourites -> stringResource(R.string.ha_v2_favorites)
    SmartHomeRoom.Other -> stringResource(R.string.ha_v2_unassigned)
    is SmartHomeRoom.Named -> room.name
}

/** A real TV navigation rail. Choosing a room only changes this view, never the saved dashboard. */
@Composable internal fun SmartHomeRoomRail(rooms: List<SmartHomeRoom>, selected: SmartHomeRoom,
    counts: Map<SmartHomeRoom, Int>, selectedFocus: FocusRequester, onChoose: (SmartHomeRoom) -> Unit,
    modifier: Modifier = Modifier, header: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = header)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rooms.forEach { room ->
                val source = remember(room) { MutableInteractionSource() }
                val chosen = room == selected
                val shape = MaterialTheme.shapes.medium
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .then(if (chosen) Modifier.focusRequester(selectedFocus) else Modifier)
                    .semantics { this.selected = chosen }
                    .focusOutline(source, shape).clip(shape)
                    .background(if (chosen) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.DirectionRight) false else {
                            if (event.type == KeyEventType.KeyDown) onChoose(room)
                            true
                        }
                    }
                    .clickable(source, indication = null, role = Role.Tab) { onChoose(room) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(when (room) { SmartHomeRoom.All -> StovaIcons.Home; SmartHomeRoom.Favourites -> StovaIcons.Star
                        else -> StovaIcons.Rows }, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(roomLabel(room), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                        color = if (chosen) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(counts[room].toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable internal fun DashboardTool(label: String, icon: ImageVector, onClick: () -> Unit,
    modifier: Modifier = Modifier, active: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    Column(modifier.widthIn(min = 56.dp, max = 104.dp).heightIn(min = 52.dp).focusOutline(source, shape).clip(shape)
        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .clickable(source, indication = null, role = Role.Button, onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
