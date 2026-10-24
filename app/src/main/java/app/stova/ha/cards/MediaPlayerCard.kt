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
@Composable fun MediaPlayerCard(entity:HaEntity,images:HaImageSource?) {
    entity.attributes["entity_picture"]?.jsonPrimitive?.contentOrNull?.let { HaImage(it,images,Modifier.size(48.dp)) }
    CardValue(entity.attributes["media_title"]?.jsonPrimitive?.contentOrNull ?: entity.state)
    entity.attributes["media_artist"]?.jsonPrimitive?.contentOrNull?.let { Text(it,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    val playing=entity.state=="playing"
    Icon(if(playing) StovaIcons.Pause else StovaIcons.Play,stringResource(if(playing) R.string.ha_pause else R.string.ha_play),Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary,CircleShape).padding(6.dp),tint=MaterialTheme.colorScheme.onPrimary)
}
