package app.stova.media
import app.stova.home.HomeRowKey
import app.stova.media.model.MediaItem
import kotlinx.serialization.Serializable
@Serializable enum class MediaRowKind { CONTINUE, LATEST_MOVIES, LATEST_SERIES, HALLOWEEN, CHRISTMAS;
 val season: app.stova.profiles.Season get() = when(this) {
  HALLOWEEN -> app.stova.profiles.Season.HALLOWEEN
  CHRISTMAS -> app.stova.profiles.Season.CHRISTMAS
  else -> app.stova.profiles.Season.NONE
 }
}
@Serializable data class MediaSnapshot(val rows: List<SnapshotRow> = emptyList(),val savedAt: Long=0,val spotlight: List<SnapshotSpotlight> = emptyList(),val spotlightAt: Long=0)
@Serializable data class SnapshotRow(val key: HomeRowKey,val accountId: String,val kind: MediaRowKind,val items: List<MediaItem>)
fun mergeContinue(resume: List<MediaItem>,nextUp: List<MediaItem>,limit: Int=20): List<MediaItem> {
 val ids=resume.mapTo(mutableSetOf()) { it.id }
 val series=resume.filter { it.type==app.stova.media.model.ItemType.EPISODE }.mapNotNullTo(mutableSetOf()) { it.seriesId }
 return (resume+nextUp.filter { it.id !in ids && (it.seriesId==null || it.seriesId !in series) }.distinctBy { it.id }).take(limit)
}
