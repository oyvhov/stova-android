package app.stova.media.net
import app.stova.media.model.ServerKind
import java.net.URLEncoder
enum class Endpoint { RESUME, NEXT_UP, LATEST_MOVIES, LATEST_SERIES }
const val ITEM_QUERY = "Fields=Overview,ProductionYear,RunTimeTicks,OfficialRating,CommunityRating,CriticRating,ParentId,SeriesId&EnableUserData=true&EnableImageTypes=Primary,Backdrop,Logo,Thumb&ImageTypeLimit=1&Limit=20"
fun endpointCandidates(kind: ServerKind, endpoint: Endpoint, userId: String): List<String> {
 val u=URLEncoder.encode(userId,"UTF-8")
 if(endpoint==Endpoint.NEXT_UP) return listOf("Shows/NextUp?UserId=$u&$ITEM_QUERY")
 val suffix=if(endpoint==Endpoint.RESUME) "Resume" else "Latest"
 val query=when(endpoint) { Endpoint.RESUME -> "MediaTypes=Video"; Endpoint.LATEST_MOVIES -> "IncludeItemTypes=Movie"; else -> "IncludeItemTypes=Episode&GroupItems=true" }
 val legacy="Users/$u/Items/$suffix?$query&$ITEM_QUERY"
 return if(kind==ServerKind.EMBY) listOf(legacy) else listOf("${if(endpoint==Endpoint.RESUME) "UserItems" else "Items"}/$suffix?userId=$u&$query&$ITEM_QUERY",legacy)
}
