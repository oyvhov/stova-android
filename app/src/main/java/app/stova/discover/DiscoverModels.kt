package app.stova.discover

/** Films and series as Seerr and TMDB name them; [wire] is the word Spole sends and `spole://discover` takes. */
enum class DiscoverType(val wire: String) { MOVIE("movie"), SERIES("tv") }

fun discoverType(wire: String?): DiscoverType? = DiscoverType.entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) }

/** What Seerr says about a title or a season: Seerr's media status 1 to 6, where 1 and anything unknown is not in Seerr at all. */
enum class SeerrStatus { UNKNOWN, PENDING, PROCESSING, PARTLY, AVAILABLE, BLOCKED }

fun seerrStatus(code: Int?): SeerrStatus = when (code) {
    2 -> SeerrStatus.PENDING
    3 -> SeerrStatus.PROCESSING
    4 -> SeerrStatus.PARTLY
    5 -> SeerrStatus.AVAILABLE
    6 -> SeerrStatus.BLOCKED
    else -> SeerrStatus.UNKNOWN
}

/** What the person may do with Seerr through Spole: nothing, only look, or look and ask in their own name. */
enum class SeerrAccess { NONE, BROWSE, PERSONAL }

/** Null for a word Stova does not know, or none: a Spole that has not answered is not a Spole that said [SeerrAccess.NONE]. */
fun seerrAccess(wire: String?): SeerrAccess? = SeerrAccess.entries.firstOrNull { it.name.equals(wire, ignoreCase = true) }

/** The rows Spole's Discover answer holds, in the order Stova shows them; [wire] is the `row` column. */
enum class DiscoverRowKind(val wire: String) {
    TRENDING("trending"), UPCOMING_MOVIES("upcoming_movies"), UPCOMING_SERIES("upcoming_series"),
    SIMILAR("similar"), RECOMMENDED("recommended"), SEARCH("search"),
}

/** A title of a Discover row; [key] is what a card and a request know it by. */
data class DiscoverTitle(
    val type: DiscoverType, val tmdbId: Int, val title: String, val year: Int?, val overview: String?, val poster: String?,
    val backdrop: String?, val status: SeerrStatus, val community: Double?, val genres: List<String>,
) {
    val key: String get() = "${type.wire}-$tmdbId"
}

data class DiscoverRow(val kind: DiscoverRowKind, val items: List<DiscoverTitle>)

/** One season of a series; only a season Seerr knows nothing of can be asked for. */
data class DiscoverSeason(val number: Int, val name: String, val episodes: Int, val status: SeerrStatus, val airDate: String?) {
    val requestable: Boolean get() = status == SeerrStatus.UNKNOWN
}

/** How far a request has come, by the word Spole sends (the lowercase name); a word Stova does not know is [UNKNOWN]. */
enum class RequestStage { REQUESTED, DOWNLOADING, IMPORTING, AVAILABLE, DECLINED, FAILED, UNKNOWN, WATCHING }

fun requestStage(wire: String?): RequestStage = RequestStage.entries.firstOrNull { it.name.equals(wire, ignoreCase = true) } ?: RequestStage.UNKNOWN

private val WITHDRAWABLE_STAGES = setOf(RequestStage.REQUESTED, RequestStage.DOWNLOADING, RequestStage.IMPORTING)

/**
 * A title's page. [requestId] and [requestStage] are the person's own request for it, when there is
 * one; the Specials (season 0) are never offered, and a request can be taken back only until it is in the library.
 */
data class DiscoverDetails(
    val title: DiscoverTitle, val tagline: String?, val runtimeMinutes: Int?, val critic: Int?, val canRequest: Boolean,
    val requestId: Int?, val requestStage: RequestStage?, val trailer: String?, val seasons: List<DiscoverSeason>,
) {
    val requestableSeasons: List<DiscoverSeason> get() = seasons.filter { it.requestable && it.number > 0 }
    val withdrawable: Boolean get() = requestId != null && requestStage in WITHDRAWABLE_STAGES
}

/** What asking for a title means: the film itself, or a choice of a series' seasons. */
enum class RequestAction { MOVIE, SEASONS }

/** What the person can ask Seerr for here; null when Seerr says no, the title is in the library, or a series has no season left to get. */
val DiscoverDetails.requestAction: RequestAction?
    get() = when {
        !canRequest || title.status == SeerrStatus.AVAILABLE -> null
        title.type == DiscoverType.MOVIE -> RequestAction.MOVIE
        requestableSeasons.isNotEmpty() -> RequestAction.SEASONS
        else -> null
    }

/** A request the person follows. [percent] is the download's, when Spole knows it; [seasons] is empty for a film. */
data class DiscoverRequest(
    val type: DiscoverType, val tmdbId: Int, val title: String, val poster: String?, val backdrop: String?, val stage: RequestStage,
    val percent: Int?, val seasons: List<Int>, val requestId: Int?, val is4k: Boolean,
)

/** What Spole answered for Discover: what the person may do, and the rows to show. */
data class DiscoverAnswer(val access: SeerrAccess, val rows: List<DiscoverRow>)

/** One page of a search; [hasMore] says there is a next one. */
data class DiscoverPage(val items: List<DiscoverTitle>, val page: Int, val hasMore: Boolean)

/** How a request or a withdrawal ended; [NOT_SUPPORTED] is a Spole too old to answer, or none. */
enum class DiscoverActionResult { DONE, ALREADY, NOT_PERSONAL, FORBIDDEN, FAILED, UNKNOWN, NOT_SUPPORTED }

fun discoverActionResult(wire: String?): DiscoverActionResult = when (wire) {
    "ok" -> DiscoverActionResult.DONE
    "already" -> DiscoverActionResult.ALREADY
    "not_personal" -> DiscoverActionResult.NOT_PERSONAL
    "forbidden" -> DiscoverActionResult.FORBIDDEN
    "failed" -> DiscoverActionResult.FAILED
    "unknown" -> DiscoverActionResult.UNKNOWN
    else -> DiscoverActionResult.NOT_SUPPORTED
}
