package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import kotlinx.coroutines.*

private enum class CardGroup(val label:Int,val type:CardType?=null) {
    ALL(R.string.ha_v2_all), LIGHTS(R.string.ha_group_lights,CardType.LIGHT),
    TOGGLES(R.string.ha_group_toggles,CardType.TOGGLE), CLIMATE(R.string.ha_group_climate,CardType.CLIMATE),
    SENSORS(R.string.ha_group_sensors,CardType.SENSOR), WEATHER(R.string.ha_group_weather,CardType.WEATHER),
    MEDIA(R.string.ha_group_media,CardType.MEDIA_PLAYER), SCENES(R.string.ha_group_scenes,CardType.SCENE),
    CAMERAS(R.string.ha_group_cameras,CardType.CAMERA), ENERGY(R.string.ha_energy_price,CardType.ENERGY_PRICE),
    COVERS(R.string.ha_group_covers,CardType.COVER), LOCKS(R.string.ha_group_locks,CardType.LOCK)
}

@Composable fun AddCardFlow(container:AppContainer,onDone:()->Unit) {
    val cards by container.haRepository.cards.collectAsStateWithLifecycle()
    AddCardFlow(container.haSocket,{entity,type->container.haRepository.add(entity,type)},onDone,cards.map {it.entityId}.toSet(),container.haRepository::loadRegistry)
}

@Composable fun AddCardFlow(connection:HaConnection,onAdd:suspend (HaEntity,CardType)->Unit,onDone:()->Unit,existing:Set<String> = emptySet(),registryLoader:suspend ()->HaRegistry? = { connection.registry() }) {
    androidx.lifecycle.compose.LifecycleResumeEffect(connection) {
        connection.hold()
        onPauseOrDispose { connection.release() }
    }
    val status by connection.status.collectAsStateWithLifecycle()
    var catalog by remember(connection) { mutableStateOf(emptyList<HaCatalogItem>()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var completedLoads by remember { mutableIntStateOf(0) }
    var restoreAfterRetry by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(CardGroup.ALL) }
    var showAll by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf<HaEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    LaunchedEffect(connection,status,retry) {
        if(status!=HaStatus.CONNECTED) {loading=status==HaStatus.CONNECTING;return@LaunchedEffect}
        loading=true;failed=false
        try {coroutineScope {
            val states=async {connection.states()};val registry=async {registryLoader()}
            // A thousand-entity home: build the catalogue off the main thread. No rooms beat no picker.
            val found=states.await();val rooms=registry.await() ?: HaRegistry()
            catalog=withContext(Dispatchers.Default) {entityCatalog(found,rooms)}
        }} catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true}
        finally {loading=false;if(currentCoroutineContext().isActive) completedLoads++}
    }
    fun add(entity:HaEntity,type:CardType) {
        if(adding || entity.id in existing) return
        adding=true
        scope.launch { try {onAdd(entity,type);onDone()} finally {adding=false} }
    }
    val filtered=remember(catalog,query,group,showAll) {catalog.filter { item ->
        (showAll || !item.hidden) && (group.type==null || group.type in cardTypesFor(item.entity)) &&
            (item.name.contains(query,true) || item.entity.id.contains(query,true) || item.room?.contains(query,true)==true)
    }}
    val grid=rememberLazyGridState()
    val categories=rememberLazyListState()
    val searchFocus=remember {FocusRequester()}
    val showAllFocus=remember {FocusRequester()}
    val retryFocus=remember {FocusRequester()}
    val categoryFocus=remember {CardGroup.entries.associateWith {FocusRequester()}}
    val entityFocus=remember {mutableMapOf<String,FocusRequester>()}
    var searchOrigin by remember {mutableStateOf(searchFocus)}
    var lastCategory by remember {mutableStateOf<CardGroup?>(null)}
    var retryCategory by remember {mutableStateOf<CardGroup?>(null)}
    var lastEntity by remember {mutableStateOf<String?>(null)}
    var pendingEntity by remember {mutableStateOf<String?>(null)}
    var navigation by remember {mutableStateOf<Job?>(null)}
    val cells=remember(filtered,existing,adding,loading,failed,status) {
        if(loading || failed || adding || status!=HaStatus.CONNECTED) emptyList()
        else filtered.mapIndexedNotNull {index,item ->
            if(item.entity.id in existing) null
            else CardFocusCell(item.entity.id,index%3,index/3,1,1,index)
        }
    }
    val latestCells by rememberUpdatedState(cells)
    fun requestCategory(target:CardGroup=lastCategory ?: group,afterLayout:Boolean=false) {
        navigation?.cancel();pendingEntity=null
        navigation=scope.launch {
            if(afterLayout) withFrameNanos {}
            categories.scrollToItem(target.ordinal)
            repeat(8) {
                if(runCatching {categoryFocus.getValue(target).requestFocus()}.getOrDefault(false)) return@launch
                withFrameNanos {}
            }
        }
    }
    fun requestEntity(id:String) {
        val target=cells.firstOrNull {it.id==id} ?: return
        navigation?.cancel()
        pendingEntity=id
        navigation=scope.launch {
            grid.scrollToItem(target.listIndex)
            // Scrolling can compose the destination after this coroutine resumes.
            repeat(8) {
                if(latestCells.none {it.id==id}) return@launch
                if(runCatching {entityFocus[id]?.requestFocus()}.getOrNull()==true) return@launch
                withFrameNanos {}
            }
            if(pendingEntity==id) pendingEntity=null
        }
    }
    fun requestRetry(afterLayout:Boolean=false) {
        navigation?.cancel();pendingEntity=null
        navigation=scope.launch {
            if(afterLayout) withFrameNanos {}
            repeat(8) {
                if(runCatching {retryFocus.requestFocus()}.getOrDefault(false)) return@launch
                withFrameNanos {}
            }
        }
    }
    fun navigateCategories(event:KeyEvent):Boolean {
        when(event.key) {
            Key.DirectionDown -> if(event.type==KeyEventType.KeyDown) {
                if(failed) requestRetry()
                else if(cells.isNotEmpty()) requestEntity(lastEntity?.takeIf {id->cells.any {it.id==id}} ?: cells.first().id)
            }
            Key.DirectionUp -> if(event.type==KeyEventType.KeyDown) {
                navigation?.cancel();pendingEntity=null
                searchOrigin.requestFocus()
            }
            else -> {
                if(event.type==KeyEventType.KeyDown) {navigation?.cancel();pendingEntity=null}
                return false
            }
        }
        // An empty/loading grid must not turn Down into a horizontal category move.
        return true
    }
    fun navigateEntities(event:KeyEvent):Boolean {
        val direction=when(event.key) {
            Key.DirectionUp->CardFocusDirection.UP
            Key.DirectionDown->CardFocusDirection.DOWN
            Key.DirectionLeft->CardFocusDirection.LEFT
            Key.DirectionRight->CardFocusDirection.RIGHT
            else->return false
        }
        if(event.type!=KeyEventType.KeyDown) return true
        val id=pendingEntity ?: lastEntity ?: return true
        val next=cardFocusNeighbor(cells,id,direction)
        if(next!=null) requestEntity(next)
        else if(direction==CardFocusDirection.UP) requestCategory()
        return true
    }
    LaunchedEffect(completedLoads) {
        if(restoreAfterRetry) {
            restoreAfterRetry=false
            // Let removal of the focused Retry button finish before restoring its origin.
            if(failed) requestRetry(afterLayout=true)
            else if(status==HaStatus.CONNECTED) requestCategory(retryCategory ?: lastCategory ?: group,afterLayout=true)
        }
    }
    LaunchedEffect(cells) {if(pendingEntity!=null && cells.none {it.id==pendingEntity}) {navigation?.cancel();pendingEntity=null}}
    DisposableEffect(Unit) {onDispose {navigation?.cancel()}}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ha_add_card),style=MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.ha_v2_choose_hint),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HaStatusLabel(status)
            StovaButton(stringResource(R.string.ha_close),onDone)
        }
        Row(Modifier.onPreviewKeyEvent {event ->
            if(event.key==Key.DirectionDown) {
                if(event.type==KeyEventType.KeyDown) requestCategory()
                true
            } else {
                if(event.type==KeyEventType.KeyDown) {navigation?.cancel();pendingEntity=null}
                false
            }
        },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            RemoteTextField(query,{query=it},stringResource(R.string.ha_v2_search),Modifier.weight(1f).focusRequester(searchFocus)
                .onFocusChanged {if(it.isFocused) {searchOrigin=searchFocus;navigation?.cancel();pendingEntity=null}})
            StovaButton(stringResource(if(showAll) R.string.ha_v2_hide_extra else R.string.ha_v2_show_all),{showAll=!showAll},
                Modifier.focusRequester(showAllFocus).onFocusChanged {if(it.isFocused) {searchOrigin=showAllFocus;navigation?.cancel();pendingEntity=null}})
        }
        LazyRow(Modifier.onPreviewKeyEvent(::navigateCategories),state=categories,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(CardGroup.entries,key={it.name}) {entry ->
                val requester=categoryFocus.getValue(entry)
                StovaButton(stringResource(entry.label),{group=entry},Modifier.focusRequester(requester)
                    .onFocusChanged {if(it.isFocused) {lastCategory=entry;navigation?.cancel();pendingEntity=null}}.semantics {selected=group==entry},primary=group==entry)
            }
        }
        if(failed) Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.ha_v2_failed_load))
            StovaButton(stringResource(R.string.ha_v2_retry),{retryCategory=lastCategory ?: group;restoreAfterRetry=true;retry++},Modifier.focusRequester(retryFocus).onPreviewKeyEvent {event ->
                when(event.key) {
                    Key.DirectionUp -> {if(event.type==KeyEventType.KeyDown) requestCategory();true}
                    Key.DirectionDown -> true
                    else -> false
                }
            })
        }
        if(!loading && !failed && status==HaStatus.CONNECTED) Text(pluralStringResource(R.plurals.ha_v2_entities,filtered.size,filtered.size),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(GridCells.Fixed(3),Modifier.weight(1f).onPreviewKeyEvent(::navigateEntities),state=grid,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(loading) items(6) { EntitySkeleton() }
            else if(status==HaStatus.CONNECTED && !failed) {
                items(filtered,key={it.entity.id}) {item ->
                    val requester=remember(item.entity.id) {entityFocus.getOrPut(item.entity.id) {FocusRequester()}}
                    EntityTile(item,item.entity.id in existing,!adding,Modifier.focusRequester(requester).onFocusChanged {
                        if(it.isFocused) {lastEntity=item.entity.id;if(pendingEntity==item.entity.id) pendingEntity=null}
                    }) {
                    val types=cardTypesFor(item.entity)
                    if(types.size==1) add(item.entity,types.single()) else choosing=item.entity
                }}
                if(filtered.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(stringResource(if(catalog.isEmpty()) R.string.ha_no_entities else R.string.ha_v2_no_matches))}
            }
        }
    }
    choosing?.let {entity -> StovaDialog({choosing=null}) {
        Column(Modifier.widthIn(max=480.dp).background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ha_choose_type),style=MaterialTheme.typography.titleLarge)
            cardTypesFor(entity).forEach {type -> StovaButton(stringResource(cardTypeLabel(type)),{add(entity,type)})}
            StovaButton(stringResource(R.string.ha_close),{choosing=null})
        }
    }}
}

@Composable private fun EntityTile(item:HaCatalogItem,added:Boolean,enabled:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    val source=remember {MutableInteractionSource()};val shape=MaterialTheme.shapes.large
    val type=cardTypesFor(item.entity).first()
    Column(modifier.fillMaxWidth().heightIn(min=128.dp).focusOutline(source,shape).clip(shape).background(MaterialTheme.colorScheme.surface)
        .clickable(source,null,enabled=enabled && !added,role=Role.Button,onClick=onClick).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(cardIcon(type,item.entity),null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Text(item.room ?: stringResource(R.string.ha_v2_unassigned),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(entityStateLabel(item.entity),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            if(added) Text(stringResource(R.string.ha_v2_added),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun EntitySkeleton() {
    Column(Modifier.fillMaxWidth().height(128.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        repeat(3) {Box(Modifier.fillMaxWidth(if(it==1) .55f else .8f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(4.dp)))}
    }
}

internal fun cardTypeLabel(type:CardType):Int = when(type) {
    CardType.LIGHT -> R.string.ha_group_lights; CardType.TOGGLE -> R.string.ha_group_toggles
    CardType.CLIMATE -> R.string.ha_group_climate; CardType.SENSOR -> R.string.ha_sensor
    CardType.WEATHER -> R.string.ha_group_weather; CardType.ENERGY_PRICE -> R.string.ha_energy_price
    CardType.MEDIA_PLAYER -> R.string.ha_group_media; CardType.SCENE -> R.string.ha_group_scenes
    CardType.CAMERA -> R.string.ha_group_cameras; CardType.COVER -> R.string.ha_group_covers; CardType.LOCK -> R.string.ha_group_locks
}
