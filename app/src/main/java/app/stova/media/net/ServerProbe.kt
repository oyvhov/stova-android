package app.stova.media.net
import app.stova.media.model.ServerKind
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException

data class ServerInfo(val kind: ServerKind, val id: String, val name: String, val version: String, val baseUrl: String)
fun addressCandidates(input: String): List<String> {
 val address=input.trim().trimEnd('/')
 return when { "://" in address -> listOf(address); ':' in address -> listOf("http://$address","https://$address"); else -> listOf("http://$address:8096","https://$address","http://$address") }
}
class ServerProbe(private val http: OkHttpClient) {
 suspend fun probe(input: String): ServerInfo {
  var failure: Throwable=IllegalArgumentException("address")
  for(base in addressCandidates(input)) try {
   val url=base.toHttpUrl(); require(url.username.isEmpty() && url.password.isEmpty() && url.query==null && url.fragment==null)
   val obj=Json.parseToJsonElement(http.fetch(Request.Builder().url("$base/System/Info/Public").build())).jsonObject
   return ServerInfo(if(obj.text("ProductName")?.contains("Emby",true)==true) ServerKind.EMBY else ServerKind.JELLYFIN,obj.text("Id") ?: error("server id"),obj.text("ServerName") ?: "",obj.text("Version") ?: "",base)
  } catch(e: CancellationException) { throw e } catch(e: Exception) { failure=e }
  throw MediaError.Unreachable(failure)
 }
}
