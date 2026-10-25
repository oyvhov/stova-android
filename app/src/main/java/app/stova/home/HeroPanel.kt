package app.stova.home
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.media.ui.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
@Composable fun HeroPanel(target:FocusedTarget?,modifier:Modifier=Modifier) {
 val large=LocalDensity.current.fontScale>=1.5f
 Box(modifier.fillMaxWidth().padding(horizontal=StovaLayout.TvSafeEdge,vertical=16.dp),contentAlignment=Alignment.BottomStart) {
 Column(Modifier.widthIn(max=560.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  when(target) {
   is FocusedTarget.Media -> {
    val item=target.item;val logo=item.art.logo
    if(logo!=null) AsyncImage(ImageRequest.Builder(LocalContext.current).data(logo).size(600,180).build(),item.title,Modifier.widthIn(max=420.dp).height(if(large) 64.dp else 80.dp),contentScale=ContentScale.Fit,alignment=Alignment.CenterStart)
    else Text(item.seriesName ?: item.title,style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface,maxLines=if(large) 1 else 2,overflow=TextOverflow.Ellipsis)
    val parts=metaParts(item).map { part -> when(part) {
     is MetaPart.Year -> part.y.toString()
     is MetaPart.Runtime -> if(part.minutes>=60) stringResource(R.string.runtime_hm,part.minutes/60,part.minutes%60) else stringResource(R.string.runtime_m,part.minutes)
     is MetaPart.EpisodeCode -> stringResource(R.string.episode_code,part.season,part.episode)
    } }
    Text(parts.joinToString(" · "),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    RatingRow(item.ratings)
    if(!large) item.overview?.let { Text(it,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis) }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) { ServiceLogo(target.account.kind);Text(target.account.serverName,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
   }
   is FocusedTarget.App -> Text(target.label,style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface)
   null -> Text(stringResource(when(java.time.LocalTime.now().hour) { in 5..9 -> R.string.greeting_morning;in 10..16 -> R.string.greeting_day;in 17..21 -> R.string.greeting_evening;else -> R.string.greeting_night }),style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface)
  }
 }
 }
}
