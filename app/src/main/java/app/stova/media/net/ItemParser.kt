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
  val type=when(obj.text("Type")?.lowercase()) { "movie" -> ItemType.MOVIE; "series" -> ItemType.SERIES; "episode" -> ItemType.EPISODE; else -> ItemType.OTHER }
  val tags=obj.obj("ImageTags"); val base=account.baseUrl
  val backdropId=if(obj.firstTag("BackdropImageTags") != null) id else obj.text("ParentBackdropItemId")
  val backdropTag=obj.firstTag("BackdropImageTags") ?: obj.firstTag("ParentBackdropImageTags")
  val logo=tags.text("Logo")?.let { ImageUrls.logo(base,id,it) } ?: obj.text("ParentLogoItemId")?.let { parent -> obj.text("ParentLogoImageTag")?.let { ImageUrls.logo(base,parent,it) } }
  val poster=if(type==ItemType.EPISODE) obj.text("SeriesId")?.let { series -> obj.text("SeriesPrimaryImageTag")?.let { ImageUrls.poster(base,series,it) } } else tags.text("Primary")?.let { ImageUrls.poster(base,id,it) }
  val wide=(if(type==ItemType.EPISODE) tags.text("Primary")?.let { ImageUrls.wide(base,id,"Primary",it) } else null)
   ?: tags.text("Thumb")?.let { ImageUrls.wide(base,id,"Thumb",it) }
   ?: obj.text("ParentThumbItemId")?.let { parent -> obj.text("ParentThumbImageTag")?.let { ImageUrls.wide(base,parent,"Thumb",it) } }
   ?: backdropId?.let { parent -> backdropTag?.let { ImageUrls.wide(base,parent,"Backdrop/0",it) } }
  fun int(n:String)=obj.text(n)?.toIntOrNull()
  return MediaItem(id,account.id,type,title,obj.text("SeriesName"),obj.text("SeriesId"),int("ParentIndexNumber"),int("IndexNumber"),int("ProductionYear"),
   obj.text("RunTimeTicks")?.toLongOrNull()?.takeIf { it>0 }?.div(600_000_000)?.toInt(),obj.text("Overview"),
   Ratings(obj.text("CommunityRating")?.toDoubleOrNull(),obj.text("CriticRating")?.toDoubleOrNull()?.toInt(),obj.text("OfficialRating")),
   obj.obj("UserData").text("PlayedPercentage")?.toFloatOrNull()?.takeIf { it>0 && it<100 }?.div(100f),
   ItemArt(backdropId?.let { parent -> backdropTag?.let { ImageUrls.backdrop(base,parent,it) } },logo,poster,wide))
 }
}
