package app.stova.media.net
import app.stova.media.model.ServerKind
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ClientIdentity(val device: String, val deviceId: String, val version: String, val client: String = "Stova")
fun authHeader(kind: ServerKind, identity: ClientIdentity, token: String?): Pair<String,String> {
 fun safe(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "")
 val value = "MediaBrowser Client=\"${safe(identity.client)}\", Device=\"${safe(identity.device)}\", DeviceId=\"${safe(identity.deviceId)}\", Version=\"${safe(identity.version)}\"" + (token?.let { ", Token=\"${safe(it)}\"" } ?: "")
 return (if(kind==ServerKind.JELLYFIN) "Authorization" else "X-Emby-Authorization") to value
}
sealed class MediaError(msg: String) : Exception(msg) {
 class Unauthorized : MediaError("401")
 class Unreachable(cause: Throwable) : MediaError("unreachable") { init { initCause(cause) } }
 class Server(val code: Int) : MediaError("http $code")
}
fun mediaHttp() = OkHttpClient.Builder()
 .connectTimeout(4,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS)
 .addNetworkInterceptor { chain ->
  val request=chain.request()
  val origin=chain.call().request().url
  val sameOrigin=request.url.scheme==origin.scheme && request.url.host==origin.host && request.url.port==origin.port
  chain.proceed(if(sameOrigin) request else request.newBuilder().removeHeader("Authorization").removeHeader("X-Emby-Authorization").build())
 }.build()
/** Cancelling a refresh also closes the socket, including during the response body read. */
internal suspend fun OkHttpClient.fetch(request: Request): String = suspendCancellableCoroutine { cont ->
 val call=newCall(request);cont.invokeOnCancellation { call.cancel() }
 call.enqueue(object: Callback {
  override fun onFailure(call: Call,e: IOException) { if(cont.isActive) cont.resumeWithException(MediaError.Unreachable(e)) }
  override fun onResponse(call: Call,response: Response) {
   try { response.use { r ->
    val body=when { r.code==401 -> throw MediaError.Unauthorized(); !r.isSuccessful -> throw MediaError.Server(r.code); else -> r.body?.string().orEmpty() }
    if(cont.isActive) cont.resume(body)
   } } catch(e: Exception) { if(cont.isActive) cont.resumeWithException(if(e is IOException) MediaError.Unreachable(e) else e) }
  }
 })
}
