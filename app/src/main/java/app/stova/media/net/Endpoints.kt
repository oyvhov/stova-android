package app.stova.media.net
import app.stova.media.model.ServerKind
import java.net.URLEncoder
enum class Endpoint { RESUME, NEXT_UP, LATEST_MOVIES, LATEST_SERIES, SPOTLIGHT, NEW_SEASONS, HALLOWEEN_TAG, HALLOWEEN_GENRE, CHRISTMAS_TAG }
const val ITEM_FIELDS = "Fields=Overview,ProductionYear,RunTimeTicks,OfficialRating,CommunityRating,CriticRating,Genres,ParentId,SeriesId&EnableUserData=true&EnableImageTypes=Primary,Backdrop,Logo,Thumb&ImageTypeLimit=1"
const val ITEM_QUERY = "$ITEM_FIELDS&Limit=20"
/** Several known items in one request, such as the series behind new seasons. */
fun idsCandidates(kind: ServerKind, userId: String, ids: List<String>): List<String> {
 val u=URLEncoder.encode(userId,"UTF-8");val list=ids.joinToString(",") { URLEncoder.encode(it,"UTF-8") }
 val legacy="Users/$u/Items?Ids=$list&$ITEM_FIELDS"
 return if(kind==ServerKind.EMBY) listOf(legacy) else listOf("Items?userId=$u&Ids=$list&$ITEM_FIELDS",legacy)
}
fun endpointCandidates(kind: ServerKind, endpoint: Endpoint, userId: String): List<String> {
 val u=URLEncoder.encode(userId,"UTF-8")
 if(endpoint==Endpoint.NEXT_UP) return listOf("Shows/NextUp?UserId=$u&$ITEM_QUERY")
 // The whole library, not a shelf: films and series with art for the spotlight, and the newest seasons.
 val library=when(endpoint) {
  Endpoint.HALLOWEEN_TAG -> "IncludeItemTypes=Movie,Series&Recursive=true&Tags=halloween&$ITEM_QUERY"
  Endpoint.HALLOWEEN_GENRE -> "IncludeItemTypes=Movie,Series&Recursive=true&Genres=Horror%7CSkrekk&$ITEM_QUERY"
  Endpoint.CHRISTMAS_TAG -> "IncludeItemTypes=Movie,Series&Recursive=true&Tags=christmas&$ITEM_QUERY"
  Endpoint.SPOTLIGHT -> "IncludeItemTypes=Movie,Series&Recursive=true&SortBy=Random&ImageTypes=Backdrop&$ITEM_FIELDS&Limit=30"
  Endpoint.NEW_SEASONS -> "IncludeItemTypes=Season&Recursive=true&SortBy=PremiereDate&SortOrder=Descending&$ITEM_FIELDS&Limit=20"
  else -> null
 }
 if(library!=null) return if(kind==ServerKind.EMBY) listOf("Users/$u/Items?$library") else listOf("Items?userId=$u&$library","Users/$u/Items?$library")
 val suffix=if(endpoint==Endpoint.RESUME) "Resume" else "Latest"
 val query=when(endpoint) { Endpoint.RESUME -> "MediaTypes=Video"; Endpoint.LATEST_MOVIES -> "IncludeItemTypes=Movie"; else -> "IncludeItemTypes=Episode&GroupItems=true" }
 val legacy="Users/$u/Items/$suffix?$query&$ITEM_QUERY"
 return if(kind==ServerKind.EMBY) listOf(legacy) else listOf("${if(endpoint==Endpoint.RESUME) "UserItems" else "Items"}/$suffix?userId=$u&$query&$ITEM_QUERY",legacy)
}
