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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
@Composable fun BackdropLayer(url:String?,modifier:Modifier=Modifier) {
 val ink=MaterialTheme.colorScheme.background
 val context=LocalContext.current
 Box(modifier.fillMaxSize().background(ink)) {
  Crossfade(url,animationSpec=tween(if(LocalReduceMotion.current) 0 else 300),label="backdrop") { image ->
   if(image!=null) AsyncImage(ImageRequest.Builder(context).data(image).size(1920,1080).build(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  }
  Canvas(Modifier.fillMaxSize()) {
   // Hold the scrim under the entire bounded hero column before fading into the artwork.
   // An even ease-out: dark behind the hero text (which ends near 63 %), no visible edge after it.
   // At least 0.74 up to 62 % keeps 6:1 behind the overview on bright art; then a long fade, no seam.
   drawRect(Brush.horizontalGradient(0f to ink,0.45f to ink.copy(alpha=0.9f),0.62f to ink.copy(alpha=0.74f),0.72f to ink.copy(alpha=0.45f),0.82f to ink.copy(alpha=0.18f),0.92f to ink.copy(alpha=0.04f),1f to ink.copy(alpha=0f)))
   drawRect(Brush.verticalGradient(listOf(ink.copy(alpha=0f),ink),startY=size.height*0.45f,endY=size.height),topLeft=Offset(0f,size.height*0.45f),size=Size(size.width,size.height*0.55f))
   drawRect(Brush.verticalGradient(listOf(ink,ink.copy(alpha=0f)),endY=size.height*0.3f),size=Size(size.width,size.height*0.3f))
  }
 }
}
