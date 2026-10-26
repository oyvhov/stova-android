package app.stova.background
import app.stova.profiles.spotlightMillis
import app.stova.profiles.dimmingPercent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.stova.core.theme.LocalReduceMotion
import app.stova.core.theme.stovaMotion
import androidx.compose.ui.platform.testTag
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
private fun backdropRequest(context:Context,url:String)=ImageRequest.Builder(context).data(url).size(1920,1080).build()
/** What the backdrop shows, kept by the app shell so a return to Home never fades in from ink. */
class BackdropState { var shown by mutableStateOf<String?>(null) }
@Composable fun rememberBackdropState() = remember { BackdropState() }
@Composable fun BackdropLayer(url:String?,modifier:Modifier=Modifier,imageLoader:ImageLoader=SingletonImageLoader.get(LocalContext.current),visible:Boolean=true,state:BackdropState=rememberBackdropState()) {
 val ink=MaterialTheme.colorScheme.background
 val prefs=app.stova.core.theme.LocalAppearance.current
 val dimming=prefs.dimmingPercent
 val context=LocalContext.current
 // The next title fades in only once its image is decoded, so a change never dips to the bare
 // background; a failed load keeps the current picture.
 val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
 val moving=backgroundCanMove(prefs,visible && lifecycle.isAtLeast(Lifecycle.State.STARTED)) && !LocalReduceMotion.current
 val scale=remember { Animatable(1f) }
 LaunchedEffect(url,imageLoader) {
  if(url==null) state.shown=null
  else if(url!=state.shown && imageLoader.execute(backdropRequest(context,url)) is SuccessResult) state.shown=url
 }
 val shown=state.shown
 LaunchedEffect(moving,shown,prefs.spotlightMillis) {
  scale.snapTo(1f)
  if(moving && shown!=null) scale.animateTo(1.04f,tween(prefs.spotlightMillis.toInt(),easing=LinearEasing))
 }
 val motion=stovaMotion
 Box(modifier.fillMaxSize().background(ink)) {
  Crossfade(shown,modifier=Modifier.graphicsLayer { scaleX=scale.value;scaleY=scale.value },animationSpec=motion.fade(motion.ambient),label="backdrop") { image ->
   if(image!=null) {
    var ready by remember(image) { mutableStateOf(false) }
    // "backdrop-target" is what the layer shows; "backdrop" only once that picture has decoded.
    Box(Modifier.fillMaxSize().testTag("backdrop-target:$image")) {
     AsyncImage(backdropRequest(context,image),null,imageLoader,
      Modifier.fillMaxSize().then(if(ready) Modifier.testTag("backdrop:$image") else Modifier),
      contentScale=ContentScale.Crop,onSuccess={ ready=true })
    }
   }
  }
  app.stova.core.theme.BackdropScrim(dimming, Modifier.fillMaxSize())
 }
}
