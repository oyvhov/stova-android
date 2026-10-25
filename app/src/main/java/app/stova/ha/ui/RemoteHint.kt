package app.stova.ha.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.stova.R
import app.stova.ha.HaEntity
import app.stova.ha.cards.*

@Composable internal fun remoteHint(card:CardConfig,entity:HaEntity?,readOnly:Boolean,kid:Boolean,editing:Boolean):String {
    if(editing) return stringResource(R.string.ha_action_hint_edit)
    if(readOnly || card.entityId.startsWith("alarm_control_panel.")) return stringResource(R.string.ha_action_hint_read)
    val resource=when {
        stateText(entity)==StateText.Unavailable->R.string.ha_action_hint_unavailable
        card.type==CardType.LIGHT->if(entity?.state=="on") R.string.ha_action_hint_light_off else R.string.ha_action_hint_light_on
        card.type==CardType.TOGGLE->if(entity?.state=="on") R.string.ha_action_hint_toggle_off else R.string.ha_action_hint_toggle_on
        card.type==CardType.SCENE->R.string.ha_action_hint_scene
        card.type==CardType.MEDIA_PLAYER->if(entity?.state=="playing") R.string.ha_action_hint_pause else R.string.ha_action_hint_play
        card.type==CardType.CLIMATE->R.string.ha_action_hint_climate
        card.type==CardType.COVER->R.string.ha_action_hint_cover
        card.type==CardType.LOCK->R.string.ha_action_hint_lock
        card.type==CardType.CAMERA->R.string.ha_action_hint_camera
        card.type==CardType.WEATHER->R.string.ha_action_hint_weather
        card.type==CardType.ENERGY_PRICE->R.string.ha_action_hint_price
        else->R.string.ha_action_hint_sensor
    }
    return stringResource(resource)+(if(kid) "" else stringResource(R.string.ha_action_hint_menu))
}
