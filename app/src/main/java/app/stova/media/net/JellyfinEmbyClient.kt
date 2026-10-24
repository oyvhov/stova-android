package app.stova.media.net
import app.stova.media.model.*
import okhttp3.*
interface MediaServerClient {
 suspend fun resume(): List<MediaItem>
 suspend fun nextUp(): List<MediaItem>
 suspend fun latest(endpoint: Endpoint): List<MediaItem>
}
class JellyfinEmbyClient(private val account: MediaAccount, private val token: String, private val http: OkHttpClient, private val identity: ClientIdentity): MediaServerClient {
 override suspend fun resume() = load(Endpoint.RESUME)
 override suspend fun nextUp() = load(Endpoint.NEXT_UP)
 override suspend fun latest(endpoint: Endpoint) = load(endpoint)
 private suspend fun load(endpoint: Endpoint): List<MediaItem> {
  val header=authHeader(account.kind,identity,token)
  for(path in endpointCandidates(account.kind,endpoint,account.userId)) {
   try { return ItemParser.parseList(http.fetch(Request.Builder().url("${account.baseUrl}/$path").header(header.first,header.second).build()),account) }
   catch(e: MediaError.Server) { if(e.code!=404) throw e }
  }
  throw MediaError.Server(404)
 }
}
