package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.ha.*
import kotlinx.coroutines.*

@Composable
fun HaCard(config:CardConfig,entity:HaEntity?,locked:Boolean,perform:(ServiceCall)->Unit,openDialog:(CardDialog)->Unit,
    images:HaImageSource?=null,visible:Boolean=true,onEdit:(()->Unit)?=null,onMenu:(()->Unit)?=null,
    pending:Boolean=false,connection:HaConnection?=null,modifier:Modifier=Modifier,room:String?=null,
    onMove:((Int)->Unit)?=null,editing:Boolean=false) {
    val unavailable=stateText(entity)==StateText.Unavailable
    var history by remember(config.entityId) {mutableStateOf(emptyList<Double>())}
    var forecast by remember(config.entityId) {mutableStateOf(emptyList<ForecastDay>())}
    var energy by remember(config.entityId) {mutableStateOf<HaEntity?>(null)}
    val scope=rememberCoroutineScope()
    val connectionStatus=connection?.status?.collectAsStateWithLifecycle()?.value
    val latestEntity by rememberUpdatedState(entity)
    LifecycleResumeEffect(config.entityId to (entity!=null),visible,connection to connectionStatus) {
        val job=scope.launch {
            if(visible && connection!=null && connectionStatus==HaStatus.CONNECTED) when(config.type) {
                CardType.SENSOR -> if(entity?.state?.toDoubleOrNull()!=null) {do {history=connection.history(config.entityId).map {it.second};delay(300_000)} while(isActive)}
                CardType.WEATHER -> do {forecast=connection.forecast(config.entityId);delay(900_000)} while(isActive)
                CardType.ENERGY_PRICE -> do {latestEntity?.let {energy=connection.energy(it)};delay(900_000)} while(isActive)
                else -> Unit
            }
        }
        onPauseOrDispose {job.cancel()}
    }
    val shown=entity
    val artwork=shown?.attributes?.text("entity_picture")?.takeIf {config.type==CardType.MEDIA_PLAYER && config.size!=CardSize.S}
    val leading:(@Composable (Modifier)->Unit)?=if(artwork!=null && images!=null) {
        {imageModifier -> HaImage(artwork,images,imageModifier,contentScale=ContentScale.Crop)}
    } else null
    val badge=if(unavailable || shown==null) null else when(config.type) {
        CardType.MEDIA_PLAYER -> if(shown.state=="playing") entityStateLabel(shown) else null
        else -> null
    }
    fun panel() {if(entity!=null) openDialog(controlPanel(config.type,entity.id))}
    fun click() {
        if(onEdit!=null) {onEdit();return}
        if(unavailable || entity==null) return
        if(locked) {panel();return}
        when(config.type) {
            CardType.SENSOR -> openDialog(CardDialog.Graph(entity.id))
            CardType.ENERGY_PRICE -> openDialog(CardDialog.Graph(entity.id,true))
            CardType.CAMERA -> openDialog(CardDialog.Camera(entity.id))
            CardType.CLIMATE -> if(!locked) openDialog(CardDialog.Climate(entity.id))
            CardType.COVER -> if(!locked) openDialog(CardDialog.Cover(entity.id))
            CardType.LOCK -> if(!locked) openDialog(CardDialog.Lock(entity.id))
            else -> if(!pending) primaryAction(config.type,entity)?.let(perform) ?: panel()
        }
    }
    CardFrame(config,cardIcon(config.type,shown,config.icon),config.name ?: entity?.let(::friendlyName) ?: config.entityId,
        badge,locked,::click,onLongClick=::panel,onMenu=onMenu,
        modifier=modifier,glow=if(config.type==CardType.LIGHT) lightGlow(shown) else null,pending=pending,leading=leading,
        onMove=onMove,editing=editing,onMediaKey={key ->
            val service=when(key) {
                Key.MediaPlayPause->"media_play_pause";Key.MediaPlay->"media_play";Key.MediaPause->"media_pause"
                Key.MediaFastForward,Key.MediaNext->"media_next_track"
                Key.MediaRewind,Key.MediaPrevious->"media_previous_track"
                else->null
            }
            if(config.type==CardType.MEDIA_PLAYER && service!=null) {
                if(!locked && !pending && !unavailable && !editing) perform(ServiceCall("media_player",service))
                true
            } else false
        }) {
        if(unavailable || shown==null) Text(stringResource(R.string.ha_unavailable),color=MaterialTheme.colorScheme.onSurfaceVariant)
        else when(config.type) {
            CardType.LIGHT -> LightCard(shown)
            CardType.TOGGLE -> ToggleCard(shown)
            CardType.CLIMATE -> ClimateCard(shown)
            CardType.SENSOR -> {SensorCard(shown);if(history.size>1) Sparkline(history,Modifier.height(28.dp))}
            CardType.WEATHER -> WeatherCard(shown,forecast,config.size,connection?.timeZone ?: java.time.ZoneId.systemDefault())
            CardType.ENERGY_PRICE -> EnergyPriceCard(energy?.copy(state=shown.state) ?: shown,connection?.timeZone ?: java.time.ZoneId.systemDefault())
            CardType.MEDIA_PLAYER -> MediaPlayerCard(shown,true,{},config.size)
            CardType.SCENE -> SceneCard()
            CardType.CAMERA -> CameraCard(shown,images,visible,config.size)
            CardType.COVER -> {CardValue(entityStateLabel(shown));shown.attributes.number("current_position")?.let {CardDetail(stringResource(R.string.ha_percent,it.toInt()))}}
            CardType.LOCK -> CardValue(entityStateLabel(shown))
        }
        if(!room.isNullOrBlank()) CardDetail(room)
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
