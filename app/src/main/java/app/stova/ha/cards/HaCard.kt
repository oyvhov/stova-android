package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.stova.R
import app.stova.ha.*
import kotlinx.coroutines.*

@Composable
fun HaCard(config:CardConfig,entity:HaEntity?,locked:Boolean,perform:(ServiceCall)->Unit,openDialog:(CardDialog)->Unit,
    images:HaImageSource?=null,visible:Boolean=true,onEdit:(()->Unit)?=null,onMenu:(()->Unit)?=null,
    pending:Boolean=false,connection:HaConnection?=null,modifier:Modifier=Modifier) {
    val unavailable=stateText(entity)==StateText.Unavailable
    var adjusting by remember(config.entityId,entity) {mutableStateOf(entity)}
    var adjustment by remember(config.entityId) {mutableStateOf<ServiceCall?>(null)}
    var history by remember(config.entityId) {mutableStateOf(emptyList<Double>())}
    var forecast by remember(config.entityId) {mutableStateOf(emptyList<ForecastDay>())}
    val scope=rememberCoroutineScope()
    LifecycleResumeEffect(config.entityId,visible,connection) {
        val job=scope.launch {
            if(visible && connection!=null) when(config.type) {
                CardType.SENSOR -> if(entity?.state?.toDoubleOrNull()!=null) {do {history=connection.history(config.entityId).map {it.second};delay(300_000)} while(isActive)}
                CardType.WEATHER -> do {forecast=connection.forecast(config.entityId);delay(900_000)} while(isActive)
                else -> Unit
            }
        }
        onPauseOrDispose {job.cancel()}
    }
    LaunchedEffect(adjustment) {adjustment?.let {delay(250);perform(it);adjustment=null}}
    val shown=if(adjustment!=null) adjusting else entity
    val badge=if(unavailable || shown==null) null else when(config.type) {
        CardType.LIGHT,CardType.TOGGLE,CardType.MEDIA_PLAYER,CardType.CLIMATE,CardType.COVER,CardType.LOCK -> entityStateLabel(shown)
        else -> null
    }
    val menu=onMenu ?: onEdit
    fun click() {
        if(onEdit!=null) {onEdit();return}
        if(unavailable || entity==null) return
        when(config.type) {
            CardType.SENSOR -> if(entity.state.toDoubleOrNull()!=null) openDialog(CardDialog.Graph(entity.id))
            CardType.ENERGY_PRICE -> openDialog(CardDialog.Graph(entity.id,true))
            CardType.CAMERA -> openDialog(CardDialog.Camera(entity.id))
            CardType.CLIMATE -> if(!locked) openDialog(CardDialog.Climate(entity.id))
            CardType.COVER -> if(!locked) openDialog(CardDialog.Cover(entity.id))
            CardType.LOCK -> if(!locked) openDialog(CardDialog.Lock(entity.id))
            else -> if(!locked && !pending) primaryAction(config.type,entity)?.let(perform)
        }
    }
    CardFrame(config,cardIcon(config.type,shown,config.icon),config.name ?: entity?.let(::friendlyName) ?: config.entityId,
        badge,locked,::click,onLongClick={
            if(menu!=null) menu() else if(!unavailable && entity!=null) when(config.type) {
                CardType.LIGHT -> if(!locked) openDialog(CardDialog.Brightness(entity.id))
                CardType.SENSOR,CardType.ENERGY_PRICE -> openDialog(CardDialog.Graph(entity.id,config.type==CardType.ENERGY_PRICE))
                else -> Unit
            }
        },modifier=modifier,active=shown?.state=="on" && config.type==CardType.LIGHT,pending=pending,onDirection={direction ->
            val call=shown?.takeUnless {unavailable}?.let {directionAction(config.type,it,direction)}
            if(call!=null && onEdit==null) {
                if(!locked && !pending) {adjusting=optimisticEntity(shown!!,call);adjustment=call}
                true
            } else false
        }) {
        if(unavailable || shown==null) Text(stringResource(R.string.ha_unavailable),color=MaterialTheme.colorScheme.onSurfaceVariant)
        else when(config.type) {
            CardType.LIGHT -> LightCard(shown)
            CardType.TOGGLE -> ToggleCard(shown)
            CardType.CLIMATE -> ClimateCard(shown)
            CardType.SENSOR -> {SensorCard(shown);if(history.size>1) Sparkline(history,Modifier.height(28.dp))}
            CardType.WEATHER -> WeatherCard(shown,forecast,config.size)
            CardType.ENERGY_PRICE -> EnergyPriceCard(shown)
            CardType.MEDIA_PLAYER -> MediaPlayerCard(shown,images,locked || pending,perform)
            CardType.SCENE -> SceneCard()
            CardType.CAMERA -> CameraCard(shown,images,visible,config.size)
            CardType.COVER -> {CardValue(entityStateLabel(shown));shown.attributes.number("current_position")?.let {CardDetail(stringResource(R.string.ha_percent,it.toInt()))}}
            CardType.LOCK -> CardValue(entityStateLabel(shown))
        }
    }
}

@Composable internal fun StateValue(entity:HaEntity) {
    when(val text=stateText(entity)) {
        is StateText.Number -> CardValue(formatNumber(text.value,LocalConfiguration.current.locales[0]),text.unit)
        else -> CardValue(entityStateLabel(entity))
    }
}
@Composable internal fun CardValue(value:String,unit:String?=null) {
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=androidx.compose.ui.Alignment.Bottom) {
        Text(value,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.weight(1f,fill=false),maxLines=2,overflow=TextOverflow.Ellipsis)
        if(!unit.isNullOrBlank()) Text(unit,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=4.dp))
    }
}
@Composable internal fun CardDetail(value:String) = Text(value,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
