package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity

@Composable
fun HaCard(config: CardConfig, entity: HaEntity?, locked: Boolean, perform: (ServiceCall) -> Unit, openDialog: (CardDialog) -> Unit, images: HaImageSource? = null, visible: Boolean = true, onEdit:(()->Unit)?=null) {
    val unavailable = stateText(entity) == StateText.Unavailable
    val icon = when(config.type) {
        CardType.LIGHT -> StovaIcons.Light; CardType.TOGGLE -> StovaIcons.Toggle; CardType.CLIMATE -> StovaIcons.Climate
        CardType.SENSOR,CardType.ENERGY_PRICE -> StovaIcons.Sensor
        CardType.WEATHER -> weatherIcon(entity?.state.orEmpty()); CardType.MEDIA_PLAYER -> StovaIcons.Play
        CardType.SCENE -> StovaIcons.Home; CardType.CAMERA -> StovaIcons.Camera
        CardType.COVER -> StovaIcons.Home; CardType.LOCK -> StovaIcons.Lock
    }
    val badge=if(unavailable) null else when(config.type) {
        CardType.LIGHT,CardType.TOGGLE -> stringResource(if(entity?.state=="on") R.string.ha_on else R.string.ha_off)
        CardType.MEDIA_PLAYER -> stringResource(if(entity?.state=="playing") R.string.ha_play else R.string.ha_pause)
        else -> null
    }
    CardFrame(config, icon, entity?.let(::friendlyName) ?: config.entityId, badge, locked, onClick={
        if(onEdit!=null) onEdit() else if (!unavailable && entity != null) {
            when(config.type) {
                CardType.CLIMATE -> openDialog(CardDialog.Climate(entity.id))
                CardType.CAMERA -> openDialog(CardDialog.Camera(entity.id))
                else -> primaryAction(config.type,entity)?.let(perform)
            }
        }
    }, onLongClick={
        if(onEdit!=null) onEdit() else if (!unavailable && entity != null) when(config.type) {
            CardType.LIGHT -> openDialog(CardDialog.Brightness(entity.id))
            CardType.SENSOR,CardType.ENERGY_PRICE -> openDialog(CardDialog.Graph(entity.id,today=config.type==CardType.ENERGY_PRICE))
            else -> longAction(config.type,entity)?.let(perform)
        }
    }) {
        if (unavailable || entity == null) Text(stringResource(R.string.ha_unavailable),color=MaterialTheme.colorScheme.onSurfaceVariant)
        else when(config.type) {
            CardType.LIGHT -> LightCard(entity)
            CardType.TOGGLE -> ToggleCard(entity)
            CardType.CLIMATE -> ClimateCard(entity)
            CardType.SENSOR -> SensorCard(entity)
            CardType.WEATHER -> WeatherCard(entity)
            CardType.ENERGY_PRICE -> EnergyPriceCard(entity)
            CardType.MEDIA_PLAYER -> MediaPlayerCard(entity,images)
            CardType.SCENE -> SceneCard()
            CardType.CAMERA -> CameraCard(entity,images,visible)
            CardType.COVER,CardType.LOCK -> StateValue(entity)
        }
    }
}

@Composable internal fun StateValue(entity: HaEntity) {
    when(val text=stateText(entity)) {
        is StateText.Number -> CardValue(formatNumber(text.value,LocalConfiguration.current.locales[0]),text.unit)
        is StateText.OnOff -> CardValue(stringResource(if(text.on) R.string.ha_on else R.string.ha_off))
        is StateText.Raw -> CardValue(text.text)
        StateText.Unavailable -> CardValue(stringResource(R.string.ha_unavailable))
    }
}
@Composable internal fun CardValue(value:String,unit:String?=null) {
    Column {
        Text(value,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.onSurface)
        if(!unit.isNullOrBlank()) Text(unit,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
