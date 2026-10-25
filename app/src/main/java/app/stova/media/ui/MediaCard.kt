package app.stova.media.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
@Composable fun MediaCard(item:MediaItem,poster:Boolean,onClick:()->Unit,onLongClick:()->Unit,onFocused:()->Unit,modifier:Modifier=Modifier) {
 val prefs=app.stova.core.theme.LocalAppearance.current
 val title=if(item.type==ItemType.EPISODE) item.seriesName ?: item.title else item.title
 val source=remember { MutableInteractionSource() };val shape=MaterialTheme.shapes.medium
 val ratio=prefs.cardSize.width/248f
 val width=if(poster) StovaLayout.PosterWidth*ratio else prefs.cardSize.width.dp
 val height=if(poster) StovaLayout.PosterHeight*ratio else StovaLayout.WideHeight*ratio
 val density=LocalDensity.current;val context=LocalContext.current
 val artUrl=if(poster) item.art.poster else item.art.wide
 var artFailed by remember(artUrl) { mutableStateOf(false) }
 val request=remember(item.art,poster,density,width,height) { ImageRequest.Builder(context).data(if(poster) item.art.poster else item.art.wide).size(with(density){width.roundToPx()},with(density){height.roundToPx()}).build() }
 // The ring frames the artwork only, so it never cuts through the title below it.
 Column(modifier.width(width).semantics { contentDescription=title }.onFocusChanged { if(it.isFocused) onFocused() }
  .onKeyEvent { if(it.key==Key.Menu && it.type==KeyEventType.KeyUp) { onLongClick();true } else false }
  .combinedClickable(source,indication=null,role=Role.Button,onClick=onClick,onLongClick=onLongClick)) {
  Box(Modifier.size(width,height).focusOutline(source,shape,zoom=true).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
   if(artUrl.isNullOrBlank() || artFailed) Text(title,Modifier.align(Alignment.Center).padding(12.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis)
   else AsyncImage(request,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize(),onError={ artFailed=true })
   if(!poster) item.progress?.let { progress -> Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f,1f)).height(4.dp).background(MaterialTheme.colorScheme.primary)) }
  }
  if(prefs.showTitles) Text(title,Modifier.padding(top=8.dp),style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurface)
 }
}
