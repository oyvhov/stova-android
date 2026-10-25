package app.stova.ha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    AddCardFlow(container.haSocket,{entity,type->container.haRepository.add(entity,type)},onDone,cards.map {it.entityId}.toSet())
}

@Composable fun AddCardFlow(connection:HaConnection,onAdd:suspend (HaEntity,CardType)->Unit,onDone:()->Unit,existing:Set<String> = emptySet()) {
    val status by connection.status.collectAsStateWithLifecycle()
    var catalog by remember(connection) { mutableStateOf(emptyList<HaCatalogItem>()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
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
            val states=async {connection.states()};val registry=async {connection.registry()}
            catalog=entityCatalog(states.await(),registry.await())
        }} catch(e:CancellationException) {throw e} catch(_:Exception) {failed=true}
        finally {loading=false}
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
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ha_add_card),style=MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.ha_v2_choose_hint),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HaStatusLabel(status)
            StovaButton(stringResource(R.string.ha_close),onDone)
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            RemoteTextField(query,{query=it},stringResource(R.string.ha_v2_search),Modifier.weight(1f))
            StovaButton(stringResource(if(showAll) R.string.ha_v2_hide_extra else R.string.ha_v2_show_all),{showAll=!showAll})
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(CardGroup.entries,key={it.name}) {entry ->
                StovaButton(stringResource(entry.label),{group=entry},Modifier.semantics {selected=group==entry},primary=group==entry)
            }
        }
        if(failed) Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.ha_v2_failed_load));StovaButton(stringResource(R.string.ha_v2_retry),{retry++})
        }
        if(!loading && !failed && status==HaStatus.CONNECTED) Text(pluralStringResource(R.plurals.ha_v2_entities,filtered.size,filtered.size),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(GridCells.Fixed(3),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(loading) items(6) { EntitySkeleton() }
            else if(status==HaStatus.CONNECTED && !failed) {
                items(filtered,key={it.entity.id}) {item -> EntityTile(item,item.entity.id in existing,!adding) {
                    val types=cardTypesFor(item.entity)
                    if(types.size==1) add(item.entity,types.single()) else choosing=item.entity
                }}
                if(filtered.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {Text(stringResource(if(catalog.isEmpty()) R.string.ha_no_entities else R.string.ha_v2_no_matches))}
            }
        }
    }
    choosing?.let {entity -> AlertDialog(onDismissRequest={choosing=null},title={Text(stringResource(R.string.ha_choose_type))},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {cardTypesFor(entity).forEach {type -> StovaButton(stringResource(cardTypeLabel(type)),{add(entity,type)})}}
    },confirmButton={StovaButton(stringResource(R.string.ha_close),{choosing=null})})}
}

@Composable private fun EntityTile(item:HaCatalogItem,added:Boolean,enabled:Boolean,onClick:()->Unit) {
    val source=remember {MutableInteractionSource()};val shape=MaterialTheme.shapes.large
    val type=cardTypesFor(item.entity).first()
    Column(Modifier.fillMaxWidth().heightIn(min=128.dp).focusOutline(source,shape).clip(shape).background(MaterialTheme.colorScheme.surface)
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
