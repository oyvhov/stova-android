package app.stova.ha.cards
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
import kotlinx.serialization.json.*
import app.stova.core.ui.StovaButton
import androidx.compose.ui.text.style.TextOverflow
@Composable fun MediaPlayerCard(entity:HaEntity,images:HaImageSource?,locked:Boolean=false,perform:(ServiceCall)->Unit={}) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        entity.attributes["entity_picture"]?.jsonPrimitive?.contentOrNull?.let { HaImage(it,images,Modifier.size(56.dp)) }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(entity.attributes["media_title"]?.jsonPrimitive?.contentOrNull ?: entityStateLabel(entity),style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
            entity.attributes["media_artist"]?.jsonPrimitive?.contentOrNull?.let {CardDetail(it)}
        }
    }
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        StovaButton(stringResource(if(entity.state=="playing") R.string.ha_pause else R.string.ha_play),{perform(ServiceCall("media_player","media_play_pause"))},icon=if(entity.state=="playing") StovaIcons.Pause else StovaIcons.Play,enabled=!locked)
        StovaButton(stringResource(R.string.ha_v2_next),{perform(ServiceCall("media_player","media_next_track"))},enabled=!locked)
    }
    entity.attributes.number("volume_level")?.let {CardDetail(stringResource(R.string.ha_v2_volume,(it*100).toInt()))}
}
