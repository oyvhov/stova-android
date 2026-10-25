package app.stova.media.ui
import androidx.compose.foundation.border
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
/** Small, opaque matte pills keep facts readable even over white artwork. */
@Composable fun FactPill(text: String, content: (@Composable () -> Unit)? = null) {
 Row(Modifier.background(MaterialTheme.colorScheme.background,MaterialTheme.shapes.extraSmall)
  .then(Modifier.border(1.dp,MaterialTheme.colorScheme.outline,MaterialTheme.shapes.extraSmall))
  .padding(horizontal=6.dp,vertical=2.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
  content?.invoke()
  Text(text,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface)
 }
}
@Composable fun RatingRow(ratings:Ratings,modifier:Modifier=Modifier,year:Int?=null) {
 val locale=LocalLocale.current.platformLocale
 Row(modifier,horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
  ratings.community?.takeIf { it in 0.0..10.0 }?.let { FactPill(stringResource(R.string.appearance_star_score,communityText(it,locale))) }
  year?.let { FactPill(it.toString()) }
  ratings.official?.let { FactPill(it) }
  ratings.criticPercent?.takeIf { it in 0..100 }?.let { score ->
   FactPill(criticText(score,locale)) {
    Image(painterResource(R.drawable.ic_tomato),null,Modifier.size(14.dp),colorFilter=ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant))
   }
  }
 }
}
