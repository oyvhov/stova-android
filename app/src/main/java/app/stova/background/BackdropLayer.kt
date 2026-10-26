package app.stova.background
import app.stova.profiles.spotlightMillis
import app.stova.profiles.dimmingPercent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import coil3.request.allowHardware
import coil3.toBitmap
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
private fun backdropRequest(context:Context,url:String)=ImageRequest.Builder(context).data(url).size(1920,1080).build()
/** A thumbnail of the picture just shown, from the disk cache, read on a worker thread. */
private suspend fun sampleTone(context:Context,loader:ImageLoader,url:String):androidx.compose.ui.graphics.Color? = runCatching {
 val request=ImageRequest.Builder(context).data(url).size(TONE_WIDTH,TONE_HEIGHT).allowHardware(false).build()
 val bitmap=(loader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return@runCatching null
 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
  val pixels=IntArray(bitmap.width*bitmap.height)
  bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
  app.stova.core.theme.artTone(pixels)
 }
}.getOrNull()
private const val TONE_WIDTH=48
private const val TONE_HEIGHT=27
/** What the backdrop shows, kept by the app shell so a return to Home never fades in from ink. */
class BackdropState {
 var shown by mutableStateOf<String?>(null)
 /** The colour the shown picture leans towards, or null for a grey picture or none. */
 var tone by mutableStateOf<androidx.compose.ui.graphics.Color?>(null)
}
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
  if(url==null) { state.shown=null; state.tone=null }
  else if(url!=state.shown && imageLoader.execute(backdropRequest(context,url)) is SuccessResult) {
   state.shown=url
   state.tone=sampleTone(context,imageLoader,url)
  }
 }
 val shown=state.shown
 LaunchedEffect(moving,shown,prefs.spotlightMillis) {
  scale.snapTo(1f)
  if(moving && shown!=null) scale.animateTo(1.04f,tween(prefs.spotlightMillis.toInt(),easing=LinearEasing))
 }
 val motion=stovaMotion
 Box(modifier.fillMaxSize().background(ink)) {
  // A dissolve, not a crossfade: the next picture fades in over the current one, which stays
  // opaque until it is covered, so a change never dips through the dark background.
  AnimatedContent(shown,modifier=Modifier.graphicsLayer { scaleX=scale.value;scaleY=scale.value },
   transitionSpec={ (fadeIn(motion.fade(motion.ambient)) togetherWith
    // Going to no picture at all fades the last one out instead of cutting it.
    (if(targetState==null) fadeOut(motion.fade(motion.ambient)) else ExitTransition.KeepUntilTransitionsFinished)).using(null) },
   label="backdrop") { image ->
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
