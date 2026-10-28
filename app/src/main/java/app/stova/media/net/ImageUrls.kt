package app.stova.media.net
import java.net.URLEncoder
object ImageUrls {
 private fun tag(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
 private fun image(base: String, id: String, type: String, size: String, tag: String) = "${base.trimEnd('/')}/Items/${tag(id)}/Images/$type?$size&tag=${tag(tag)}"
 fun backdrop(base: String, id: String, tag: String) = image(base,id,"Backdrop/0","maxWidth=1920&quality=80",tag)
 fun logo(base: String, id: String, tag: String) = image(base,id,"Logo","maxWidth=600",tag)
 fun poster(base: String, id: String, tag: String) = image(base,id,"Primary","maxHeight=450&quality=90",tag)
 fun wide(base: String, id: String, type: String, tag: String) = image(base,id,type,"maxWidth=640",tag)
 /** The user's own picture. A new picture brings a new tag, so the old one never sticks in the cache. */
 fun user(base: String, kind: app.stova.media.model.ServerKind, userId: String, tag: String) =
  if(kind==app.stova.media.model.ServerKind.JELLYFIN) "${base.trimEnd('/')}/UserImage?userId=${tag(userId)}&tag=${tag(tag)}&maxWidth=360"
  else "${base.trimEnd('/')}/Users/${tag(userId)}/Images/Primary?tag=${tag(tag)}&maxWidth=360"
}
