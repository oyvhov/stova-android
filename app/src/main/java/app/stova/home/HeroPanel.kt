package app.stova.home
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.StovaLayout
import app.stova.media.SpotlightEntry
import app.stova.media.ui.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
/** The rotating library spotlight: a film or series, its logo, a calm line of facts and the start of its story. */
@Composable fun HeroPanel(entry:SpotlightEntry?,modifier:Modifier=Modifier) {
 val large=LocalDensity.current.fontScale>=1.5f
 val calm=LocalReduceMotion.current
 Box(modifier.fillMaxWidth().padding(horizontal=StovaLayout.TvSafeEdge,vertical=16.dp),contentAlignment=Alignment.BottomStart) {
  AnimatedContent(entry,contentKey={ it?.item?.id },contentAlignment=Alignment.BottomStart,label="spotlight",
   transitionSpec={ (fadeIn(tween(if(calm) 0 else 700,delayMillis=if(calm) 0 else 250)) togetherWith fadeOut(tween(if(calm) 0 else 350))).using(SizeTransform(clip=false)) }) { current ->
   Column(Modifier.widthIn(max=560.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    if(current==null) Text(stringResource(when(java.time.LocalTime.now().hour) { in 5..9 -> R.string.greeting_morning;in 10..16 -> R.string.greeting_day;in 17..21 -> R.string.greeting_evening;else -> R.string.greeting_night }),style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface)
    else {
     val item=current.item;val logo=item.art.logo
     if(logo!=null) AsyncImage(ImageRequest.Builder(LocalContext.current).data(logo).size(800,240).transformations(TrimTransparent()).build(),item.title,Modifier.widthIn(max=440.dp).height(if(large) 60.dp else 80.dp),contentScale=ContentScale.Fit,alignment=Alignment.BottomStart)
     else Text(item.title,style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface,maxLines=if(large) 1 else 2,overflow=TextOverflow.Ellipsis)
     val parts=metaParts(item).map { part -> when(part) {
      is MetaPart.Year -> part.y.toString()
      is MetaPart.Runtime -> if(part.minutes>=60) stringResource(R.string.runtime_hm,part.minutes/60,part.minutes%60) else stringResource(R.string.runtime_m,part.minutes)
     } }
     // One calm line of facts: the new-season mark, year (and a film's length), then the scores.
     FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),itemVerticalAlignment=Alignment.CenterVertically) {
      if(current.newSeason) Text(stringResource(R.string.spotlight_new_season),Modifier.background(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.shapes.small).padding(horizontal=10.dp,vertical=3.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
      if(parts.isNotEmpty()) Text(parts.joinToString(" · "),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
      RatingRow(item.ratings)
     }
     if(!large) item.overview?.let { Text(it,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis) }
    }
   }
  }
 }
}
