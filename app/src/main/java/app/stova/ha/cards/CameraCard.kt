package app.stova.ha.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.stova.R
import app.stova.ha.*
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class HaImageSource(val base:String,val auth:HaAuth,val loader:ImageLoader)

@Composable fun CameraCard(entity:HaEntity,images:HaImageSource?,visible:Boolean,size:CardSize=CardSize.S) {
    HaImage("/api/camera_proxy/${entity.id}",images,Modifier.fillMaxWidth().height(if(size==CardSize.L) 236.dp else 80.dp),if(visible) 10_000L else null)
    CardDetail(stringResource(R.string.ha_v2_camera_live))
}

/** No bearer token crosses origins, no camera frames are cached, and paused/offscreen cards stop. */
@Composable internal fun HaImage(path:String, source:HaImageSource?,modifier:Modifier=Modifier,refreshMillis:Long?=null) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var token by remember(source,path) { mutableStateOf<String?>(null) }
    var tick by remember(source,path) { mutableLongStateOf(0L) }
    val base=source?.base?.toHttpUrlOrNull()
    val url=base?.resolve(path)?.takeIf { it.scheme=="https" || isPrivateHost(it.host) }
    val sameOrigin=base != null && url != null && base.scheme==url.scheme && base.host==url.host && base.port==url.port
    LifecycleResumeEffect(source,path,refreshMillis) {
        val job=scope.launch {
            if(source!=null && url!=null) {
                do {
                    token=if(sameOrigin) try { source.auth.accessToken() } catch(e:CancellationException) {throw e} catch(_:Exception) {null} else ""
                    tick=System.currentTimeMillis()
                    if(refreshMillis==null) break
                    delay(refreshMillis)
                } while(isActive)
            }
        }
        onPauseOrDispose { job.cancel() }
    }
    val camera=path.startsWith("/api/camera_proxy/")
    val request=remember(url,token,tick) {
        if(url==null || token==null || (camera && refreshMillis==null)) null else ImageRequest.Builder(context)
            .data(if(camera) url.newBuilder().addQueryParameter("cache",tick.toString()).build().toString() else url.toString())
            .httpHeaders(NetworkHeaders.Builder().apply { if(sameOrigin && !token.isNullOrEmpty()) set("Authorization","Bearer $token") }.build())
            .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
    }
    if(source!=null) AsyncImage(request,stringResource(R.string.ha_image),source.loader,modifier)
}
