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
   drawRect(Brush.horizontalGradient(0f to ink,0.35f to ink.copy(alpha=0.85f),0.6f to ink.copy(alpha=0.8f),0.625f to ink.copy(alpha=0.7f),0.65f to ink.copy(alpha=0f)))
   drawRect(Brush.verticalGradient(listOf(ink.copy(alpha=0f),ink),startY=size.height*0.45f,endY=size.height),topLeft=Offset(0f,size.height*0.45f),size=Size(size.width,size.height*0.55f))
   drawRect(Brush.verticalGradient(listOf(ink,ink.copy(alpha=0f)),endY=size.height*0.3f),size=Size(size.width,size.height*0.3f))
  }
 }
}
