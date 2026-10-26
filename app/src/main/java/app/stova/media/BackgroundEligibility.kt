package app.stova.media

import app.stova.media.model.MediaItem
import app.stova.profiles.BackgroundFilter
import app.stova.profiles.BackgroundLibraryRef

/** Conservative display ages, not Jellyfin/Emby parental-rating scores. PG is treated as 10;
 * R/TV-MA/NC-17 as 18. Unknown and ambiguous composite labels remain unknown. */
private val ratingPrefix = Regex("^(NO|SE|DK|FI|DE|GB|UK|US|AU)[: -]+")
private val numericRating = Regex("^(?:(?:FSK|USK|NO|SE|DK|FI)[ -]?)?(0|6|7|9|10|11|12|13|14|15|16|17|18)(?:\\+| ÅR)?$")
private val swedishRating = Regex("^FRÅN (7|11|15) ÅR$")
private val namedAges = mapOf("A" to 0, "AL" to 0, "ALL" to 0, "ALL AGES" to 0, "G" to 0,
    "U" to 0, "BTL" to 0, "BARNTILLÅTEN" to 0, "TV-Y" to 0, "TV-G" to 0, "TV-Y7" to 7, "TV-Y7-FV" to 7,
    "PG" to 10, "TV-PG" to 10, "PG-13" to 13, "TV-14" to 14, "12A" to 12,
    "R" to 18, "NC-17" to 18, "TV-MA" to 18, "R18" to 18, "R18+" to 18,
    "BFJ" to 18, "BARNFÖRBJUDEN" to 18, "MA15+" to 15, "MA 15+" to 15, "M" to 15)

fun backgroundRatingAge(rating: String?): Int? {
    val value=rating?.trim()?.uppercase(java.util.Locale.ROOT) ?: return null
    val token=value.replace(ratingPrefix, "")
    return namedAges[token] ?: numericRating.matchEntire(token)?.groupValues?.get(1)?.toIntOrNull()
        ?: swedishRating.matchEntire(token)?.groupValues?.get(1)?.toIntOrNull()
}

fun acceptsBackground(item: MediaItem, filter: BackgroundFilter): Boolean =
    (filter.libraries == null || filter.libraries.any {
        it.accountId == item.accountId && it.libraryId in item.backgroundLibraryIds
    }) && (filter.maxAge == null || backgroundRatingAge(item.ratings.official)?.let { it <= filter.maxAge } == true)

fun backgroundForProfile(entry:SpotlightEntry?,profileId:String,filter:BackgroundFilter):SpotlightEntry? = entry?.takeIf {
    it.account.profileId==profileId && it.item.accountId==it.account.id && acceptsBackground(it.item,filter)
}

data class BackgroundLibrary(val ref: BackgroundLibraryRef, val name: String, val accountName: String)
data class BackgroundLibraryCatalog(val libraries: List<BackgroundLibrary> = emptyList(),
    val failedAccounts: List<String> = emptyList(), val hasAccounts: Boolean = false)

/** A title may belong to overlapping views. Retain every proven membership in the snapshot. */
internal fun mergeBackgroundMembership(entries:List<SpotlightEntry>):List<SpotlightEntry> =
    entries.groupBy {it.account.id to it.item.id}.values.map { copies->
        val first=copies.first()
        first.copy(item=first.item.copy(backgroundLibraryIds=copies.flatMap {it.item.backgroundLibraryIds}.toSet()))
    }
