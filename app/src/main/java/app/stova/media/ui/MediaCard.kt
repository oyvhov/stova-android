package app.stova.media.ui
import androidx.compose.foundation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.StovaLayout
import app.stova.core.ui.focusOutline
import app.stova.media.model.*
import app.stova.home.LocalRowStyle
import app.stova.home.artworkDimensions
import app.stova.home.showsCardTitles
import app.stova.profiles.RowFormat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
@Composable fun MediaCard(item:MediaItem,poster:Boolean,onClick:()->Unit,onLongClick:()->Unit,onFocused:()->Unit,modifier:Modifier=Modifier) {
 val prefs=app.stova.core.theme.LocalAppearance.current
 val title=if(item.type==ItemType.EPISODE) item.seriesName ?: item.title else item.title
 val source=remember { MutableInteractionSource() };val shape=MaterialTheme.shapes.medium
 val rowStyle=LocalRowStyle.current
 val format=rowStyle.format ?: if(poster) RowFormat.POSTER else RowFormat.WIDE
 val dimensions=artworkDimensions(format,rowStyle.size ?: prefs.cardSize)
 val fit=app.stova.home.LocalArtworkScale.current
 val width=(dimensions.width*fit).dp
 val height=(dimensions.height*fit).dp
 val usePoster=format==RowFormat.POSTER
 val density=LocalDensity.current;val context=LocalContext.current
 val fallbackLineHeight=with(density) { MaterialTheme.typography.titleSmall.lineHeight.toDp() }
 val fallbackPadding=((height - fallbackLineHeight)/2).coerceIn(0.dp, 8.dp)
 val artUrl=if(usePoster) item.art.poster ?: item.art.wide else item.art.wide ?: item.art.poster
 var artFailed by remember(artUrl) { mutableStateOf(false) }
 val request=remember(artUrl,density,width,height) { ImageRequest.Builder(context).data(artUrl).size(with(density){width.roundToPx()},with(density){height.roundToPx()}).build() }
 // The ring frames the artwork only, so it never cuts through the title below it.
 Column(modifier.width(width).semantics { contentDescription=title }.onFocusChanged { if(it.isFocused) onFocused() }
  .onKeyEvent { if(it.key==Key.Menu && it.type==KeyEventType.KeyUp) { onLongClick();true } else false }
  .combinedClickable(source,indication=null,role=Role.Button,onClick=onClick,onLongClick=onLongClick)) {
  Box(Modifier.size(width,height).focusOutline(source,shape,zoom=true).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
   if(artUrl.isNullOrBlank() || artFailed) Text(title,Modifier.align(Alignment.Center).padding(horizontal=8.dp,vertical=fallbackPadding),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurface,maxLines=if(height >= fallbackLineHeight*2 + fallbackPadding*2) 2 else 1,overflow=TextOverflow.Ellipsis)
   else AsyncImage(request,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize(),onError={ artFailed=true })
   if(prefs.progressStyle.dp > 0) item.progress?.let { progress -> Box(Modifier.testTag("card-progress").align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f,1f)).height(prefs.progressStyle.dp.dp).background(MaterialTheme.colorScheme.primary)) }
  }
  if(rowStyle.showsCardTitles(prefs)) {
  val focused by source.collectIsFocusedAsState()
  val motion=app.stova.core.theme.stovaMotion
  val strength by animateFloatAsState(app.stova.core.theme.cardTitleAlpha(focused || app.stova.core.ui.LocalDrawnFocus.current,app.stova.home.LocalRowHasFocus.current),motion.fade(motion.focusRing),label="card-title")
  Text(title,Modifier.padding(top=8.dp).graphicsLayer { alpha=strength },style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurface)
 }
 }
}
