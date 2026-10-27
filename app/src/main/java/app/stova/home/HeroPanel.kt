package app.stova.home
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stova.profiles.*
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.theme.stovaMotion
import app.stova.media.SpotlightEntry
import app.stova.media.ui.*
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
/** Large is a bigger hero, not only a taller box: its logo, title and text column all grow. */
internal val HeroSize.logoHeight: Dp get() = if (this == HeroSize.LARGE) 156.dp else 96.dp
internal val HeroSize.logoWidth: Dp get() = if (this == HeroSize.LARGE) 540.dp else 360.dp
internal val HeroSize.textWidth: Dp get() = if (this == HeroSize.LARGE) 560.dp else 400.dp

/** The rotating library spotlight: a film or series, its logo, a calm line of facts and the start of its story. */
@Composable fun HeroPanel(entry:SpotlightEntry?,modifier:Modifier=Modifier) {
 val large=LocalDensity.current.fontScale>=1.5f
 val prefs=app.stova.core.theme.LocalAppearance.current
 if(prefs.heroSize==HeroSize.OFF) return
 val motion=stovaMotion
 Box(modifier.fillMaxWidth().padding(horizontal=StovaLayout.TvSafeEdge,vertical=16.dp),contentAlignment=Alignment.TopStart) {
  // The new title arrives at the backdrop's pace, once the old one has mostly left.
  AnimatedContent(entry,contentKey={ it?.item?.id },contentAlignment=Alignment.TopStart,label="spotlight",
   transitionSpec={ (fadeIn(motion.enter(motion.ambient,delayMillis=motion.standard)) togetherWith fadeOut(motion.exit(motion.emphasized))).using(null) }) { current ->
   val big=prefs.heroSize==HeroSize.LARGE
   val titleStyle=if(big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium
   Column(Modifier.widthIn(max=prefs.heroSize.textWidth).fillMaxWidth().fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(if(big) 10.dp else 8.dp)) {
    if(current==null) {
     val season = if(prefs.seasonalDecor == SeasonalDecor.OFF) Season.NONE else prefs.season()
     Text(stringResource(when(season) {
      Season.HALLOWEEN -> R.string.theme_greeting_halloween
      Season.CHRISTMAS -> R.string.theme_greeting_christmas
      Season.NONE -> when(java.time.LocalTime.now().hour) { in 5..9 -> R.string.greeting_morning;in 10..16 -> R.string.greeting_day;in 17..21 -> R.string.greeting_evening;else -> R.string.greeting_night }
     }),style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onSurface)
     SeasonalCountdown(season)
    }
    else {
     val item=current.item;val logo=item.art.logo
     // Measure the real facts and optional overview first. The logo gets the space left over,
     // capped inside the existing hero, so neither title changes nor decoding move the rows.
     if(logo!=null && prefs.heroTitle==HeroTitle.LOGO) HeroLogo(logo, item.title, large || prefs.showOverview, titleStyle,
         Modifier.weight(1f, fill=false).heightIn(max=prefs.heroSize.logoHeight).fillMaxHeight(), prefs.heroSize.logoWidth)
     else Text(item.title,style=titleStyle,color=MaterialTheme.colorScheme.onSurface,maxLines=if(large || prefs.showOverview) 1 else 2,overflow=TextOverflow.Ellipsis)
     FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),itemVerticalAlignment=Alignment.CenterVertically) {
      if(current.newSeason && prefs.showNewSeason) Text(stringResource(R.string.spotlight_new_season),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface)
      RatingRow(item.ratings.copy(
       community = item.ratings.community.takeIf { prefs.showHeroRatings },
       criticPercent = item.ratings.criticPercent.takeIf { prefs.showHeroRatings },
       official = item.ratings.official.takeIf { prefs.showHeroAge }), year = item.year.takeIf { prefs.showHeroYear })
     }
     if(prefs.showGenres) Text((listOf(stringResource(if(item.type==app.stova.media.model.ItemType.MOVIE) R.string.appearance_movie else R.string.appearance_series))+item.genres).joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
     if(prefs.showOverview && !large) item.overview?.let { Text(it,style=if(big) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=if(big) 3 else 2,overflow=TextOverflow.Ellipsis) }
    }
   }
  }
 }
}

@Composable
private fun HeroLogo(url: String, title: String, compactTitle: Boolean, style: TextStyle, modifier: Modifier, maxWidth: Dp) {
    SubcomposeAsyncImage(
        ImageRequest.Builder(LocalContext.current).data(url).size(800, 240)
            .transformations(TrimTransparent()).build(),
        title,
        modifier.widthIn(max = maxWidth).fillMaxWidth(),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
        loading = { HeroTitle(title, compactTitle, style) },
        error = { HeroTitle(title, compactTitle, style) },
    )
}

@Composable private fun HeroTitle(title: String, compact: Boolean, style: TextStyle) {
    Text(title, style = style, color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
}
