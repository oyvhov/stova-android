package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import app.stova.R
import app.stova.core.ui.StovaIcons
import app.stova.core.ui.focusOutline
import app.stova.ha.HaEntity
import app.stova.ha.text
import kotlinx.coroutines.delay

@Composable fun MediaPlayerCard(entity:HaEntity,locked:Boolean=false,perform:(ServiceCall)->Unit={},size:CardSize=CardSize.S) {
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(entity.state) {if(entity.state=="playing") while(true) {delay(1_000);now=System.currentTimeMillis()}}
    val duration=entity.attributes.number("media_duration")
    val position=entity.attributes.number("media_position")
    val updated=entity.attributes.text("media_position_updated_at")?.let {runCatching {app.stova.ha.haMillis(it)}.getOrNull()}
    val elapsed=if(entity.state=="playing" && updated!=null) ((now-updated)/1000.0).coerceAtLeast(0.0) else 0.0
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(entity.attributes.text("media_title") ?: entityStateLabel(entity),style=MaterialTheme.typography.titleMedium,maxLines=if(size==CardSize.L) 3 else 1,overflow=TextOverflow.Ellipsis)
            entity.attributes.text("media_artist")?.let {CardDetail(it)}
            if(position!=null && duration!=null && duration>0) LinearProgressIndicator(
                progress={((position+elapsed)/duration).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(3.dp),
                color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.surfaceVariant,drawStopIndicator={})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                listOf("media_play_pause","media_next_track").forEach {service ->
                    val source=remember {MutableInteractionSource()}
                    val playing=entity.state=="playing"
                    val label=stringResource(if(service=="media_next_track") R.string.ha_v2_next else if(playing) R.string.ha_pause else R.string.ha_play)
                    Box(Modifier.size(32.dp).focusOutline(source,MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.small)
                        .clickable(source,indication=null,enabled=!locked,onClick={perform(ServiceCall("media_player",service))}),contentAlignment=Alignment.Center) {
                        Icon(if(service=="media_next_track") HaIcons.Next else if(playing) StovaIcons.Pause else StovaIcons.Play,label,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurface)
                    }
                }
                entity.attributes.number("volume_level")?.let {CardDetail(stringResource(R.string.ha_v2_volume,(it*100).toInt()))}
            }
        }
    }
}
