package app.stova.media.net
import app.stova.media.model.ServerKind
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

data class AuthResult(val token: String, val userId: String, val userName: String)
data class QuickConnectCode(val code: String, val secret: String)
class MediaAuth(private val http: OkHttpClient, private val identity: ClientIdentity) {
 private suspend fun request(server: ServerInfo,path: String,body: JsonObject?=null): String {
  val header=authHeader(server.kind,identity,null)
  val r=Request.Builder().url("${server.baseUrl}/$path").header(header.first,header.second)
  if(body!=null) r.post(body.toString().toRequestBody("application/json".toMediaType()))
  return http.fetch(r.build())
 }
 private fun auth(body: String): AuthResult {
  val obj=Json.parseToJsonElement(body).jsonObject
  return AuthResult(obj.text("AccessToken") ?: error("token"),obj.obj("User").text("Id") ?: error("user id"),obj.obj("User").text("Name").orEmpty())
 }
 suspend fun signIn(server: ServerInfo,user: String,password: String) = auth(request(server,"Users/AuthenticateByName",buildJsonObject { put("Username",user);put("Pw",password) }))
 suspend fun quickConnectEnabled(server: ServerInfo): Boolean = server.kind==ServerKind.JELLYFIN && request(server,"QuickConnect/Enabled").trim().equals("true",true)
 suspend fun quickConnectStart(server: ServerInfo): QuickConnectCode {
  val obj=Json.parseToJsonElement(request(server,"QuickConnect/Initiate",buildJsonObject {})).jsonObject
  return QuickConnectCode(obj.text("Code") ?: error("code"),obj.text("Secret") ?: error("secret"))
 }
 suspend fun quickConnectAwait(server: ServerInfo,secret: String,pollMillis: Long=2_000,timeoutMillis: Long=300_000): AuthResult = withTimeout(timeoutMillis) {
  val encoded=java.net.URLEncoder.encode(secret,"UTF-8")
  while(true) {
   val obj=Json.parseToJsonElement(request(server,"QuickConnect/Connect?secret=$encoded")).jsonObject
   if(obj.text("Authenticated")=="true") break
   delay(pollMillis)
  }
  auth(request(server,"Users/AuthenticateWithQuickConnect",buildJsonObject { put("Secret",secret) }))
 }
}
