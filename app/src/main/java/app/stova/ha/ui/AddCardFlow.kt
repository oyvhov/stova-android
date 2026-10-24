package app.stova.ha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.AppContainer
import app.stova.R
import app.stova.core.ui.*
import app.stova.ha.*
import app.stova.ha.cards.*
import kotlinx.coroutines.launch

private enum class CardGroup(val label:Int,val domains:Set<String>) {
    LIGHTS(R.string.ha_group_lights,setOf("light")),
    TOGGLES(R.string.ha_group_toggles,setOf("switch","input_boolean","fan")),
    CLIMATE(R.string.ha_group_climate,setOf("climate")),
    SENSORS(R.string.ha_group_sensors,setOf("sensor","binary_sensor")),
    WEATHER(R.string.ha_group_weather,setOf("weather")),
    MEDIA(R.string.ha_group_media,setOf("media_player")),
    SCENES(R.string.ha_group_scenes,setOf("scene","script")),
    CAMERAS(R.string.ha_group_cameras,setOf("camera")),
}

@Composable fun AddCardFlow(container:AppContainer,onDone:()->Unit) =
    AddCardFlow(container.haSocket,{entity,type->container.haRepository.add(entity,type)},onDone)

@Composable fun AddCardFlow(connection:HaConnection,onAdd:suspend (HaEntity,CardType)->Unit,onDone:()->Unit) {
    var entities by remember { mutableStateOf(emptyList<HaEntity>()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<CardGroup?>(null) }
    var choosing by remember { mutableStateOf<HaEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    LaunchedEffect(connection) { entities=connection.states().filter {cardTypesFor(it).isNotEmpty()}.sortedBy {friendlyName(it).lowercase()};loading=false }
    fun add(entity:HaEntity,type:CardType) {
        if(adding) return
        adding=true
        scope.launch { try { onAdd(entity,type); onDone() } finally {adding=false} }
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.ha_add_card),style=MaterialTheme.typography.headlineMedium)
        RemoteTextField(query,{query=it},stringResource(R.string.ha_search))
        if(loading) Text(stringResource(R.string.ha_loading))
        val filtered=entities.filter { friendlyName(it).contains(query,ignoreCase=true) || it.id.contains(query,ignoreCase=true) }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CardGroup.entries.forEach { entry ->
                val matches=filtered.filter { it.id.substringBefore('.') in entry.domains }
                if(matches.isNotEmpty()) {
                    item(key=entry.name) { StovaButton(stringResource(entry.label),{group=if(group==entry) null else entry}) }
                    if(group==entry || query.isNotBlank()) items(matches,key={it.id}) {entity->
                        StovaButton(friendlyName(entity),{
                            val types=cardTypesFor(entity)
                            if(types.size==1) add(entity,types.single()) else choosing=entity
                        },enabled=!adding)
                    }
                }
            }
            if(!loading && filtered.isEmpty()) item {Text(stringResource(R.string.ha_no_entities))}
        }
        choosing?.let {entity->
            Text(stringResource(R.string.ha_choose_type))
            cardTypesFor(entity).forEach {type-> StovaButton(stringResource(cardTypeLabel(type)),{add(entity,type)},enabled=!adding) }
        }
        StovaButton(stringResource(R.string.ha_close),onDone)
    }
}

internal fun cardTypeLabel(type:CardType):Int = when(type) {
    CardType.LIGHT -> R.string.ha_group_lights; CardType.TOGGLE -> R.string.ha_group_toggles
    CardType.CLIMATE -> R.string.ha_group_climate; CardType.SENSOR -> R.string.ha_sensor
    CardType.WEATHER -> R.string.ha_group_weather; CardType.ENERGY_PRICE -> R.string.ha_energy_price
    CardType.MEDIA_PLAYER -> R.string.ha_group_media; CardType.SCENE -> R.string.ha_group_scenes
    CardType.CAMERA -> R.string.ha_group_cameras
}
