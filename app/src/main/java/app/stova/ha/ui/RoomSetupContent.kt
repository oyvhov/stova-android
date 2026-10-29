package app.stova.ha.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.HaCatalogItem
import app.stova.ha.cards.*

/** Suggestions only. Selecting a room never adds a card or sends a device command. */
internal fun roomSuggestions(catalog: List<HaCatalogItem>, room: String, existing: Set<String>): List<HaCatalogItem> =
    catalog.filter { !it.hidden && it.room == room && it.entity.id !in existing && suggestedType(it) != null }

internal fun suggestedType(item: HaCatalogItem): CardType? = cardTypesFor(item.entity).firstOrNull {
    it in setOf(CardType.LIGHT, CardType.CLIMATE, CardType.MEDIA_PLAYER, CardType.TOGGLE, CardType.COVER, CardType.WEATHER, CardType.ENERGY_PRICE)
}

/** Shared with the enclosing dialog, which receives Back before its content does. */
@Stable class RoomSetupState internal constructor() {
    internal val pages=SheetPages()
    internal var busy=false
    internal fun back():Boolean = if(busy) true else pages.back()
}

@Composable internal fun RoomSetupContent(catalog: List<HaCatalogItem>, existing: Set<String>, busy: Boolean, failed: Boolean,
    onAdd: (List<HaCatalogItem>) -> Unit, onBrowse: () -> Unit, onClose: () -> Unit,
    state:RoomSetupState=remember {RoomSetupState()}) {
    val room=state.pages.current
    SideEffect {state.busy=busy}
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val rooms = remember(catalog, existing) { catalog.mapNotNull { it.room?.takeIf(String::isNotBlank) }.distinct()
        .filter { roomSuggestions(catalog, it, existing).isNotEmpty() }.sortedWith(String.CASE_INSENSITIVE_ORDER) }
    val candidates = remember(catalog, room, existing) { room?.let { roomSuggestions(catalog, it, existing) }.orEmpty() }
    val first = remember { FocusRequester() }
    val list=rememberLazyListState()
    LaunchedEffect(room, rooms) {
        val returning=state.pages.returning;state.pages.returning=null
        list.scrollToItem(if(room==null && returning!=null) rooms.indexOf(returning).coerceAtLeast(0) else 0)
        withFrameNanos { }
        val restored=room==null && returning!=null && runCatching {state.pages.opener(returning).requestFocus()}.getOrDefault(false)
        if(!restored) runCatching {first.requestFocus()}
    }
    fun back() { state.back() }
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        if (room != null && event.key in setOf(Key.Back,Key.Escape)) {
            if (event.type == KeyEventType.KeyUp) back()
            true
        } else false
    }, verticalArrangement=Arrangement.spacedBy(16.dp)) {
        PageHeader(stringResource(if(room==null) R.string.ha_room_choose else R.string.ha_room_review),StovaIcons.Home,
            subtitle=room ?: stringResource(R.string.ha_room_intro))
        if (room != null) Text(stringResource(R.string.ha_room_review_hint),style=MaterialTheme.typography.bodyMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().focusRequester(first).focusGroup(),state=list,
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if (room == null) {
                items(rooms,key={it}) { name ->
                    SheetAction(name,StovaIcons.Home,{
                        state.pages.open(name);selected=roomSuggestions(catalog,name,existing).map {it.entity.id}.toSet()
                    },Modifier.focusRequester(state.pages.opener(name)),value=stringResource(R.string.ha_room_count,roomSuggestions(catalog,name,existing).size),opensPage=true)
                }
                if(rooms.isEmpty()) item {Text(stringResource(R.string.ha_room_none),style=MaterialTheme.typography.bodyLarge)}
                item {SheetAction(stringResource(R.string.ha_room_browse),StovaIcons.Apps,onBrowse,opensPage=true)}
            } else items(candidates,key={it.entity.id}) { item ->
                SettingsSwitchRow(item.name,item.entity.id in selected,{on->if(!busy) selected=if(on) selected+item.entity.id else selected-item.entity.id},
                    subtitle=stringResource(cardTypeLabel(suggestedType(item)!!)))
            }
        }
        if(failed) Text(stringResource(R.string.ha_room_failed),color=MaterialTheme.colorScheme.error)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(room!=null) {
                StovaButton(stringResource(if(busy) R.string.ha_v2_pending else R.string.ha_room_add, selected.intersect(candidates.map {it.entity.id}.toSet()).size),
                    {if(!busy) onAdd(candidates.filter {it.entity.id in selected})},primary=true,
                    enabled=busy || candidates.any {it.entity.id in selected})
                StovaButton(stringResource(R.string.custom_back),::back,quiet=true)
            } else StovaButton(stringResource(R.string.ha_close),onClose,quiet=true)
        }
    }
}
