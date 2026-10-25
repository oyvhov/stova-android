package app.stova.media.ui
import android.content.Context
import okio.Path.Companion.toOkioPath
import app.stova.media.model.ServerKind
import app.stova.media.net.*
import okhttp3.*
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.request.allowHardware
class MediaAuthInterceptor(private val tokensByHost: () -> Map<String,Pair<ServerKind,String>>,private val identity: () -> ClientIdentity): Interceptor {
 constructor(tokensByHost: () -> Map<String,Pair<ServerKind,String>>, identity: ClientIdentity) : this(tokensByHost, { identity })
 override fun intercept(chain:Interceptor.Chain):Response {
  val request=chain.request();val builder=request.newBuilder().removeHeader("Authorization").removeHeader("X-Emby-Authorization")
  tokensByHost()["${request.url.host}:${request.url.port}"]?.let { (kind,token) -> val (key,value)=authHeader(kind,identity(),token);builder.header(key,value) }
  return chain.proceed(builder.build())
 }
}
fun buildImageLoader(context:Context,http:OkHttpClient,interceptor:MediaAuthInterceptor):ImageLoader = ImageLoader.Builder(context)
 .components { add(OkHttpNetworkFetcherFactory(callFactory={ http.newBuilder().addNetworkInterceptor(interceptor).build() })) }
 .memoryCache { MemoryCache.Builder().maxSizePercent(context,0.20).build() }
 .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve("images").toOkioPath()).maxSizeBytes(256L*1024*1024).build() }
 .crossfade(false).allowHardware(true).build()
