package app.stova.media
import app.stova.media.model.*
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** One title the home hero shows while it rotates: a film or a series, never an episode. */
data class SpotlightEntry(val item: MediaItem, val account: MediaAccount, val newSeason: Boolean)
@Serializable data class SnapshotSpotlight(val accountId: String, val item: MediaItem, val newSeason: Boolean)

const val SPOTLIGHT_LIMIT = 30
/** A season counts as new for this long after it premiered. */
const val NEW_SEASON_DAYS = 45L

/**
 * Series whose second or later season premiered lately lead, shown as the series; then the random
 * library pool. Only titles with a backdrop qualify, and those with a logo come first.
 */
fun spotlightEntries(account: MediaAccount, pool: List<MediaItem>, seasons: List<MediaItem>, today: LocalDate): List<SpotlightEntry> {
    val fresh = seasons.filter { season ->
        val premiered = season.premiered?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        season.type == ItemType.SEASON && (season.season ?: 0) >= 2 && season.seriesId != null && season.art.backdrop != null &&
            premiered != null && !premiered.isAfter(today) && !premiered.isBefore(today.minusDays(NEW_SEASON_DAYS))
    }.distinctBy { it.seriesId }.map { season ->
        SpotlightEntry(MediaItem(season.seriesId!!, season.accountId, ItemType.SERIES, season.seriesName ?: season.title,
            year = season.year, overview = season.overview, ratings = season.ratings, art = season.art), account, newSeason = true)
    }
    val taken = fresh.mapTo(mutableSetOf()) { it.item.id }
    val rest = pool.filter { (it.type == ItemType.MOVIE || it.type == ItemType.SERIES) && it.art.backdrop != null && it.id !in taken }
        .distinctBy { it.id }.sortedBy { it.art.logo == null }.map { SpotlightEntry(it, account, newSeason = false) }
    return (fresh + rest).take(SPOTLIGHT_LIMIT)
}
