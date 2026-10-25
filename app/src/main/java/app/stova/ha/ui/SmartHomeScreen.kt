package app.stova.ha.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.StovaButton
import app.stova.ha.*
import app.stova.ha.cards.*
import app.stova.profiles.Profile
import kotlinx.coroutines.launch

@Composable fun SmartHomeScreen(container:AppContainer,profile:Profile) {
    val cards by container.haRepository.cards.collectAsStateWithLifecycle()
    val entities by container.haRepository.entities.collectAsStateWithLifecycle()
    val status by container.haSocket.status.collectAsStateWithLifecycle()
    val household by container.household.data.collectAsStateWithLifecycle(initialValue=null)
    val scope=rememberCoroutineScope()
    var adding by remember(profile.id) { mutableStateOf(false) }
    var dialog by remember(profile.id) { mutableStateOf<CardDialog?>(null) }
    val images=household?.haBaseUrl?.let { HaImageSource(it,container.haAuth,container.haImages) }
    SmartHomeContent(profile,cards,entities,if(household?.haBaseUrl==null) HaStatus.NOT_CONFIGURED else status,
        images=images,onAdd={adding=true},
        onPerform={call,id->scope.launch {container.haRepository.perform(call,id,profile)}},
        onDialog={dialog=it},
        onMove={card,delta->scope.launch {container.haRepository.move(card.id,delta)}},
        onWide={card->scope.launch {container.haRepository.setWide(card.id,!card.wide)}},
        onHome={card->container.haRepository.setOnHome(card.id,!card.onHome)},
        onRemove={card->scope.launch {container.haRepository.remove(card.id)}})
    if(adding && !profile.kid) Dialog({adding=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(StovaLayout.TvSafeEdge)) {
            AddCardFlow(container) {adding=false}
        }
    }
    CardDialogHost(dialog,entities,container.haSocket,images,!canControl(profile),{call,id->scope.launch {container.haRepository.perform(call,id,profile)}},{dialog=null})
}

/** State-driven surface keeps layout and remote interactions independent of the transport. */
@Composable fun SmartHomeContent(
    profile:Profile,cards:List<CardConfig>,entities:Map<String,HaEntity>,status:HaStatus,
    images:HaImageSource?=null,onAdd:()->Unit={},onPerform:(ServiceCall,String)->Unit={_,_->},
    onDialog:(CardDialog)->Unit={},onMove:(CardConfig,Int)->Unit={_,_->},onWide:(CardConfig)->Unit={},
    onHome:suspend (CardConfig)->Boolean={true},onRemove:(CardConfig)->Unit={},
) {
    var editing by remember(profile.id) {mutableStateOf(false)}
    var menuId by remember(profile.id) {mutableStateOf<String?>(null)}
    var limit by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val grid=rememberLazyGridState()
    val visible by remember {derivedStateOf {grid.layoutInfo.visibleItemsInfo.map {it.key}.toSet()}}
    BackHandler(enabled=editing) {editing=false;menuId=null}
    Column(Modifier.fillMaxSize().padding(StovaLayout.TvSafeEdge),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.nav_smart_home),style=MaterialTheme.typography.headlineMedium)
            HaStatusLabel(status)
        }
        if(status==HaStatus.NOT_CONFIGURED) {
            Text(stringResource(R.string.ha_setup_instruction));return@Column
        }
        if(!profile.kid) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            StovaButton(stringResource(R.string.ha_add_card),onAdd)
            if(cards.isNotEmpty()) StovaButton(stringResource(if(editing) R.string.ha_done else R.string.ha_edit),{editing=!editing})
        }
        if(cards.isEmpty()) Text(stringResource(R.string.ha_empty))
        LazyVerticalGrid(GridCells.Fixed(3),state=grid,modifier=Modifier.widthIn(max=752.dp).fillMaxWidth().weight(1f),
            horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            items(cards,key={it.id},span={GridItemSpan(if(it.wide) 2 else 1)}) {card->
                val entity=entities[card.entityId]
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    HaCard(card,entity,!canControl(profile),{onPerform(it,card.entityId)},onDialog,images,card.id in visible,
                        onEdit=if(editing) {{menuId=card.id;limit=false}} else null)
                    if(editing && !profile.kid) StovaButton(stringResource(R.string.ha_card_menu,entity?.let(::friendlyName) ?: card.entityId),{menuId=card.id;limit=false})
                }
            }
        }
    }
    val menu=cards.find {it.id==menuId}
    if(menu!=null && !profile.kid) Dialog({menuId=null}) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(entities[menu.entityId]?.let(::friendlyName) ?: menu.entityId)
            StovaButton(stringResource(R.string.ha_move_left),{onMove(menu,-1);menuId=null})
            StovaButton(stringResource(R.string.ha_move_right),{onMove(menu,1);menuId=null})
            StovaButton(stringResource(if(menu.wide) R.string.ha_narrow else R.string.ha_wide),{onWide(menu);menuId=null})
            StovaButton(stringResource(if(menu.onHome) R.string.ha_hide_home else R.string.ha_show_home),{scope.launch {
                if(onHome(menu)) menuId=null else limit=true
            }})
            if(limit) Text(stringResource(R.string.ha_home_limit),color=MaterialTheme.colorScheme.onSurfaceVariant)
            StovaButton(stringResource(R.string.ha_remove),{onRemove(menu);menuId=null})
            StovaButton(stringResource(R.string.ha_close),{menuId=null})
        }
    }
}
