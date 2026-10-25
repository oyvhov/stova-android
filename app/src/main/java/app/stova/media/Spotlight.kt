package app.stova.media
import app.stova.media.model.*
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** One title the home hero shows while it rotates: a film or a series, never an episode. */
data class SpotlightEntry(val item: MediaItem, val account: MediaAccount, val newSeason: Boolean, val season: app.stova.profiles.Season = app.stova.profiles.Season.NONE)
@Serializable data class SnapshotSpotlight(val accountId: String, val item: MediaItem, val newSeason: Boolean)

const val SPOTLIGHT_LIMIT = 30
/** A season counts as new for this long after it premiered. */
const val NEW_SEASON_DAYS = 45L

fun acceptsSpotlight(item: MediaItem, source: app.stova.profiles.SpotlightSource): Boolean = when(source) {
    app.stova.profiles.SpotlightSource.LIBRARY -> true
    app.stova.profiles.SpotlightSource.MOVIES -> item.type == ItemType.MOVIE
    app.stova.profiles.SpotlightSource.SERIES -> item.type == ItemType.SERIES
    app.stova.profiles.SpotlightSource.FAVORITES -> item.favorite
}

private fun freshSeasons(seasons: List<MediaItem>, today: LocalDate) = seasons.filter { season ->
    val premiered = season.premiered?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    season.type == ItemType.SEASON && (season.season ?: 0) >= 2 && season.seriesId != null && season.art.backdrop != null &&
        premiered != null && !premiered.isAfter(today) && !premiered.isBefore(today.minusDays(NEW_SEASON_DAYS))
}.distinctBy { it.seriesId }

/** The series worth one detail request: those whose new season will lead the spotlight. */
fun newSeasonSeriesIds(seasons: List<MediaItem>, today: LocalDate): List<String> = freshSeasons(seasons, today).mapNotNull { it.seriesId }

/**
 * Series whose second or later season premiered lately lead, shown as the series; then the random
 * library pool. Only titles with a backdrop qualify, and those with a logo come first.
 */
fun spotlightEntries(account: MediaAccount, pool: List<MediaItem>, seasons: List<MediaItem>, today: LocalDate,
    series: List<MediaItem> = emptyList()): List<SpotlightEntry> {
    val known = series.filter { it.type == ItemType.SERIES && it.art.backdrop != null }.associateBy { it.id }
    // The series' own story, ratings and logo when known; otherwise what the season inherits from it.
    val fresh = freshSeasons(seasons, today).map { season ->
        val item = known[season.seriesId] ?: MediaItem(season.seriesId!!, season.accountId, ItemType.SERIES, season.seriesName ?: season.title,
            year = season.year, overview = season.overview, ratings = season.ratings, art = season.art)
        SpotlightEntry(item, account, newSeason = true)
    }
    val taken = fresh.mapTo(mutableSetOf()) { it.item.id }
    val rest = pool.filter { (it.type == ItemType.MOVIE || it.type == ItemType.SERIES) && it.art.backdrop != null && it.id !in taken }
        .distinctBy { it.id }.sortedBy { it.art.logo == null }.map { SpotlightEntry(it, account, newSeason = false) }
    return (fresh + rest).take(SPOTLIGHT_LIMIT)
}

/** The seasonal shelf leads; the complete ordinary pool follows, with each account kept distinct. */
fun seasonalSpotlight(pool: List<SpotlightEntry>, rows: List<app.stova.home.HomeRowUi.Media>, season: app.stova.profiles.Season): List<SpotlightEntry> {
    if (season == app.stova.profiles.Season.NONE) return pool
    val seasonal = rows.filter { it.kind.season == season }.flatMap { row ->
        row.items.filter { (it.type == ItemType.MOVIE || it.type == ItemType.SERIES) && it.art.backdrop != null }.map { item ->
            SpotlightEntry(item, row.account, pool.any { it.account.id == row.account.id && it.item.id == item.id && it.newSeason }, season)
        }
    }
    return (seasonal + pool).distinctBy { it.account.id to it.item.id }
}
