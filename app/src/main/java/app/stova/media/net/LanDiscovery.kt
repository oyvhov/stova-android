package app.stova.media.net
import app.stova.media.model.ServerKind
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DiscoveredServer(val kind: ServerKind, val id: String, val name: String, val address: String)
fun parseDiscoveryReply(payload: String,sender: InetAddress,kind: ServerKind): DiscoveredServer? = runCatching {
 val obj=Json.parseToJsonElement(payload).jsonObject
 val url=(obj.text("Address") ?: return null).toHttpUrl()
 val host=url.host
 val literal=host.contains(':') || host.matches(Regex("[0-9.]+"))
 val address=if(literal && host!=sender.hostAddress) url.newBuilder().host(sender.hostAddress!!).build().toString().trimEnd('/') else url.toString().trimEnd('/')
 DiscoveredServer(kind,obj.text("Id") ?: return null,obj.text("Name").orEmpty(),address)
}.getOrNull()
class LanDiscovery {
 fun discover(timeoutMillis: Long=2_500): Flow<DiscoveredServer> = flow {
  val seen=mutableSetOf<String>()
  DatagramSocket().use { socket ->
   socket.broadcast=true; socket.soTimeout=200
   val destinations=mutableSetOf(InetAddress.getByName("255.255.255.255"))
   NetworkInterface.getNetworkInterfaces().toList().flatMap { it.interfaceAddresses }.mapNotNullTo(destinations) { it.broadcast }
   for(kind in ServerKind.entries) for(address in destinations) {
    val msg="who is ${if(kind==ServerKind.JELLYFIN) "Jellyfin" else "Emby"}Server?".toByteArray()
    runCatching { socket.send(DatagramPacket(msg,msg.size,address,7359)) }
   }
   val end=System.nanoTime()+timeoutMillis*1_000_000
   while(System.nanoTime()<end) {
    currentCoroutineContext().ensureActive()
    val packet=DatagramPacket(ByteArray(16_384),16_384)
    try { socket.receive(packet) } catch(_: SocketTimeoutException) { continue }
    val payload=String(packet.data,0,packet.length,Charsets.UTF_8)
    val kind=if(payload.contains("Emby",true)) ServerKind.EMBY else ServerKind.JELLYFIN
    parseDiscoveryReply(payload,packet.address,kind)?.let { if(seen.add(it.id)) emit(it) }
   }
  }
 }.flowOn(Dispatchers.IO)
}
