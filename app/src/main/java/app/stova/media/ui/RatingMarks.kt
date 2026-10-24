package app.stova.media.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.media.model.Ratings
import java.text.NumberFormat
import java.util.Locale
fun communityText(score:Double,locale:Locale):String=String.format(locale,"%.1f",score)
fun criticText(percent:Int,locale:Locale):String=NumberFormat.getPercentInstance(locale).format(percent/100.0)
@Composable fun RatingRow(ratings:Ratings,modifier:Modifier=Modifier) {
 val locale=LocalLocale.current.platformLocale
 Row(modifier,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
  ratings.community?.let { score ->
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
    Text(stringResource(R.string.rating_tmdb),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(communityText(score,locale),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface)
   }
  }
  ratings.criticPercent?.let { score ->
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
    Image(painterResource(R.drawable.ic_tomato),null,Modifier.size(20.dp),colorFilter=ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant))
    Text(criticText(score,locale),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface)
   }
  }
  ratings.official?.let { Text(it,Modifier.background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.extraSmall).padding(horizontal=6.dp,vertical=2.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface) }
 }
}
