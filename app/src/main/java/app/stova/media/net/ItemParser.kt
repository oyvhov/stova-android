package app.stova.media.net
import app.stova.media.model.*
import kotlinx.serialization.json.*
internal fun JsonObject.field(name: String): JsonElement? = entries.firstOrNull { it.key.equals(name, true) }?.value
internal fun JsonObject.text(name: String): String? = (field(name) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
internal fun JsonObject.obj(name: String) = field(name) as? JsonObject ?: JsonObject(emptyMap())
private fun JsonObject.firstTag(name: String) = (field(name) as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull
object ItemParser {
 fun parseList(body: String, account: MediaAccount): List<MediaItem> {
  val root = Json.parseToJsonElement(body)
  val items = root as? JsonArray ?: (root as? JsonObject)?.field("Items") as? JsonArray ?: return emptyList()
  return items.mapNotNull { (it as? JsonObject)?.let { obj -> parseItem(obj, account) } }
 }
 fun parseItem(obj: JsonObject, account: MediaAccount): MediaItem? {
  val id=obj.text("Id") ?: return null; val title=obj.text("Name") ?: return null
  val type=when(obj.text("Type")?.lowercase()) { "movie" -> ItemType.MOVIE; "series" -> ItemType.SERIES; "season" -> ItemType.SEASON; "episode" -> ItemType.EPISODE; else -> ItemType.OTHER }
  val tags=obj.obj("ImageTags"); val base=account.baseUrl
  val backdropId=if(obj.firstTag("BackdropImageTags") != null) id else obj.text("ParentBackdropItemId")
  val backdropTag=obj.firstTag("BackdropImageTags") ?: obj.firstTag("ParentBackdropImageTags")
  val logo=tags.text("Logo")?.let { ImageUrls.logo(base,id,it) } ?: obj.text("ParentLogoItemId")?.let { parent -> obj.text("ParentLogoImageTag")?.let { ImageUrls.logo(base,parent,it) } }
  val poster=if(type==ItemType.EPISODE) obj.text("SeriesId")?.let { series -> obj.text("SeriesPrimaryImageTag")?.let { ImageUrls.poster(base,series,it) } } else tags.text("Primary")?.let { ImageUrls.poster(base,id,it) }
  val parentThumb=obj.text("ParentThumbItemId")?.let { parent -> obj.text("ParentThumbImageTag")?.let { ImageUrls.wide(base,parent,"Thumb",it) } }
  val backdropWide=backdropId?.let { parent -> backdropTag?.let { ImageUrls.wide(base,parent,"Backdrop/0",it) } }
  val ownThumb=tags.text("Thumb")?.let { ImageUrls.wide(base,id,"Thumb",it) }
  // An episode card wears its series art; its own still only when the series has none.
  val wide=if(type==ItemType.EPISODE) parentThumb ?: backdropWide ?: ownThumb ?: tags.text("Primary")?.let { ImageUrls.wide(base,id,"Primary",it) }
   else ownThumb ?: parentThumb ?: backdropWide
  fun int(n:String)=obj.text(n)?.toIntOrNull()
  val season=if(type==ItemType.SEASON) int("IndexNumber") else int("ParentIndexNumber")
  return MediaItem(id,account.id,type,title,obj.text("SeriesName"),obj.text("SeriesId"),season,if(type==ItemType.SEASON) null else int("IndexNumber"),int("ProductionYear"),
   obj.text("RunTimeTicks")?.toLongOrNull()?.takeIf { it>0 }?.div(600_000_000)?.toInt(),obj.text("Overview"),
   Ratings(obj.text("CommunityRating")?.toDoubleOrNull(),obj.text("CriticRating")?.toDoubleOrNull()?.toInt(),obj.text("OfficialRating")),
   obj.obj("UserData").text("PlayedPercentage")?.toFloatOrNull()?.takeIf { it>0 && it<100 }?.div(100f),
   ItemArt(backdropId?.let { parent -> backdropTag?.let { ImageUrls.backdrop(base,parent,it) } },logo,poster,wide),
   obj.text("PremiereDate")?.take(10))
 }
}
