package app.stova.media.net
import app.stova.media.model.*
import okhttp3.*
import app.stova.media.BackgroundLibrary
import app.stova.profiles.BackgroundLibraryRef
import app.stova.profiles.SpotlightSource
import kotlinx.serialization.json.*
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
 suspend fun spotlight(source: app.stova.profiles.SpotlightSource): List<MediaItem> = latest(Endpoint.SPOTLIGHT)
 suspend fun searchTitles(query: String): List<MediaItem> = emptyList()
 suspend fun backgroundLibraries(): List<BackgroundLibrary> = emptyList()
 suspend fun backgrounds(source: SpotlightSource, libraryId: String?, query: String? = null, ids: List<String>? = null): List<MediaItem> =
  if(libraryId!=null) emptyList() else when { ids!=null->items(ids);query!=null->searchTitles(query);else->spotlight(source) }
}
class JellyfinEmbyClient(private val account: MediaAccount, private val token: String, private val http: OkHttpClient, private val identity: ClientIdentity): MediaServerClient {
 override suspend fun resume() = latest(Endpoint.RESUME)
 override suspend fun nextUp() = latest(Endpoint.NEXT_UP)
 override suspend fun latest(endpoint: Endpoint) = load(endpointCandidates(account.kind,endpoint,account.userId))
 override suspend fun items(ids: List<String>) = if(ids.isEmpty()) emptyList() else load(idsCandidates(account.kind,account.userId,ids))
 override suspend fun spotlight(source: app.stova.profiles.SpotlightSource) = load(spotlightCandidates(account.kind,account.userId,source))
 override suspend fun searchTitles(query: String) = load(titleSearchCandidates(account.kind,account.userId,query))
 override suspend fun backgrounds(source: SpotlightSource,libraryId:String?,query:String?,ids:List<String>?) =
  load(backgroundCandidates(account.kind,account.userId,source,libraryId,query,ids))
   .map { if(libraryId==null) it else it.copy(backgroundLibraryIds=setOf(libraryId)) }
 override suspend fun backgroundLibraries():List<BackgroundLibrary> {
  val header=authHeader(account.kind,identity,token)
  val body=http.fetch(Request.Builder().url("${account.baseUrl}/${libraryViewsPath(account.userId)}").header(header.first,header.second).build())
  val root=Json.parseToJsonElement(body)
  val views=(root as? JsonObject)?.field("Items") as? JsonArray ?: root as? JsonArray ?: return emptyList()
  return views.mapNotNull { value ->
   val view=value as? JsonObject ?: return@mapNotNull null
   if(view.text("CollectionType")?.lowercase() !in listOf(null,"movies","tvshows","mixed","homevideos")) return@mapNotNull null
   val id=view.text("Id") ?: return@mapNotNull null
   val name=view.text("Name") ?: return@mapNotNull null
   BackgroundLibrary(BackgroundLibraryRef(account.id,id),name,"${account.serverName} · ${account.userName}")
  }
 }
 private suspend fun load(candidates: List<String>): List<MediaItem> {
  val header=authHeader(account.kind,identity,token)
  for(path in candidates) {
   try { return ItemParser.parseList(http.fetch(Request.Builder().url("${account.baseUrl}/$path").header(header.first,header.second).build()),account) }
   catch(e: MediaError.Server) { if(e.code!=404) throw e }
  }
  throw MediaError.Server(404)
 }
}
