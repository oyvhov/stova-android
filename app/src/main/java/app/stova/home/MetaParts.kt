package app.stova.home
import app.stova.media.model.*
sealed interface FocusedTarget {
 data class Media(val item:MediaItem,val account:MediaAccount):FocusedTarget
 data class App(val label:String):FocusedTarget
}
sealed interface MetaPart {
 data class Year(val y:Int):MetaPart
 data class Runtime(val minutes:Int):MetaPart
 data class EpisodeCode(val season:Int,val episode:Int):MetaPart
}
fun metaParts(item:MediaItem):List<MetaPart> = buildList {
 if(item.type==ItemType.EPISODE) {
  if(item.season!=null && item.episode!=null) add(MetaPart.EpisodeCode(item.season,item.episode))
  item.runtimeMinutes?.let { add(MetaPart.Runtime(it)) }
 } else {
  item.year?.let { add(MetaPart.Year(it)) };item.runtimeMinutes?.let { add(MetaPart.Runtime(it)) }
 }
}
