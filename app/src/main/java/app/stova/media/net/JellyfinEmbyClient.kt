package app.stova.media.net
import app.stova.media.model.*
import okhttp3.*
interface MediaServerClient {
 suspend fun resume(): List<MediaItem>
 suspend fun nextUp(): List<MediaItem>
 suspend fun latest(endpoint: Endpoint): List<MediaItem>
 /** Separate requests provide tag OR genre, never the server's tag AND genre intersection. */
 suspend fun seasonal(season: app.stova.profiles.Season): List<MediaItem> = when(season) {
  app.stova.profiles.Season.NONE -> emptyList()
  app.stova.profiles.Season.CHRISTMAS -> latest(Endpoint.CHRISTMAS_TAG)
  app.stova.profiles.Season.HALLOWEEN -> (latest(Endpoint.HALLOWEEN_TAG) + latest(Endpoint.HALLOWEEN_GENRE)).distinctBy { it.id }.take(20)
 }
 suspend fun items(ids: List<String>): List<MediaItem> = emptyList()
}
class JellyfinEmbyClient(private val account: MediaAccount, private val token: String, private val http: OkHttpClient, private val identity: ClientIdentity): MediaServerClient {
 override suspend fun resume() = latest(Endpoint.RESUME)
 override suspend fun nextUp() = latest(Endpoint.NEXT_UP)
 override suspend fun latest(endpoint: Endpoint) = load(endpointCandidates(account.kind,endpoint,account.userId))
 override suspend fun items(ids: List<String>) = if(ids.isEmpty()) emptyList() else load(idsCandidates(account.kind,account.userId,ids))
 private suspend fun load(candidates: List<String>): List<MediaItem> {
  val header=authHeader(account.kind,identity,token)
  for(path in candidates) {
   try { return ItemParser.parseList(http.fetch(Request.Builder().url("${account.baseUrl}/$path").header(header.first,header.second).build()),account) }
   catch(e: MediaError.Server) { if(e.code!=404) throw e }
  }
  throw MediaError.Server(404)
 }
}
