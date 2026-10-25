package app.stova.home
import app.stova.media.model.*
sealed interface FocusedTarget {
 data class Media(val item:MediaItem,val account:MediaAccount):FocusedTarget
 data class App(val label:String):FocusedTarget
}
sealed interface MetaPart {
 data class Year(val y:Int):MetaPart
 data class Runtime(val minutes:Int):MetaPart
}
/** Year, and a film's length; a series' episode runtime would read as the length of the whole show. */
fun metaParts(item:MediaItem):List<MetaPart> = buildList {
 item.year?.let { add(MetaPart.Year(it)) }
 if(item.type==ItemType.MOVIE) item.runtimeMinutes?.let { add(MetaPart.Runtime(it)) }
}
