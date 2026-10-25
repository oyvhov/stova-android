package app.stova.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.stova.core.theme.LocalReduceMotion
import androidx.compose.ui.platform.testTag
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import android.content.Context
import androidx.compose.runtime.*
private fun backdropRequest(context:Context,url:String)=ImageRequest.Builder(context).data(url).size(1920,1080).build()
/** What the backdrop shows, kept by the app shell so a return to Home never fades in from ink. */
class BackdropState { var shown by mutableStateOf<String?>(null) }
@Composable fun rememberBackdropState() = remember { BackdropState() }
@Composable fun BackdropLayer(url:String?,modifier:Modifier=Modifier,imageLoader:ImageLoader=SingletonImageLoader.get(LocalContext.current),state:BackdropState=rememberBackdropState()) {
 val ink=MaterialTheme.colorScheme.background
 val dimming=app.stova.core.theme.LocalAppearance.current.backdropDimming
 val context=LocalContext.current
 // The next title fades in only once its image is decoded, so a change never dips to the bare
 // background; a failed load keeps the current picture.
 LaunchedEffect(url,imageLoader) {
  if(url==null) state.shown=null
  else if(url!=state.shown && imageLoader.execute(backdropRequest(context,url)) is SuccessResult) state.shown=url
 }
 val shown=state.shown
 Box(modifier.fillMaxSize().background(ink)) {
  Crossfade(shown,animationSpec=tween(if(LocalReduceMotion.current) 0 else 900),label="backdrop") { image ->
   if(image!=null) AsyncImage(backdropRequest(context,image),null,imageLoader,Modifier.fillMaxSize().testTag("backdrop:$image"),contentScale=ContentScale.Crop)
  }
  Canvas(Modifier.fillMaxSize()) {
   // Keep a readable left column, then release the picture well before the right half.
   // Dark preserves the previous scrim; lighter choices never weaken the top-bar protection.
   val stops=when(dimming) {
    app.stova.profiles.BackdropDimming.DARK -> arrayOf(0f to ink,0.45f to ink.copy(alpha=0.9f),0.62f to ink.copy(alpha=0.74f),0.72f to ink.copy(alpha=0.45f),0.82f to ink.copy(alpha=0.18f),0.92f to ink.copy(alpha=0.04f),1f to ink.copy(alpha=0f))
    app.stova.profiles.BackdropDimming.MEDIUM -> arrayOf(0f to ink,0.15f to ink.copy(alpha=0.94f),0.35f to ink.copy(alpha=0.9f),0.46f to ink.copy(alpha=0.84f),0.52f to ink.copy(alpha=0.65f),0.62f to ink.copy(alpha=0.18f),0.76f to ink.copy(alpha=0f),1f to ink.copy(alpha=0f))
    app.stova.profiles.BackdropDimming.LIGHT -> arrayOf(0f to ink.copy(alpha=0.96f),0.15f to ink.copy(alpha=0.92f),0.35f to ink.copy(alpha=0.88f),0.46f to ink.copy(alpha=0.84f),0.52f to ink.copy(alpha=0.5f),0.62f to ink.copy(alpha=0.05f),0.72f to ink.copy(alpha=0f),1f to ink.copy(alpha=0f))
   }
   drawRect(Brush.horizontalGradient(*stops))
   val bottomStart=if(dimming==app.stova.profiles.BackdropDimming.LIGHT) 0.5f else 0.45f
   drawRect(Brush.verticalGradient(listOf(ink.copy(alpha=0f),ink),startY=size.height*bottomStart,endY=size.height),topLeft=Offset(0f,size.height*bottomStart),size=Size(size.width,size.height*(1f-bottomStart)))
   drawRect(Brush.verticalGradient(listOf(ink,ink.copy(alpha=0f)),endY=size.height*0.3f),size=Size(size.width,size.height*0.3f))
  }
 }
}
