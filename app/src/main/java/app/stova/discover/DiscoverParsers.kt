package app.stova.discover

import android.database.Cursor

// Spole's numbers may reach us as text, and a real cursor gives a whole number as «40.0» when Spole
// stored it as a float: every column is read as text and turned into what it should be.
private fun Cursor.text(column: String): String? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let { getString(it) }
private fun Cursor.optional(column: String): String? = text(column)?.takeIf { it.isNotBlank() }
private fun Cursor.int(column: String): Int? = text(column)?.trim()?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }
private fun Cursor.double(column: String): Double? = text(column)?.trim()?.toDoubleOrNull()

/** A title Stova could open: a known type and a TMDB id. The same columns stand in a row and in a title's answer. */
private fun Cursor.discoverTitle(): DiscoverTitle? {
    val type = discoverType(text("type")) ?: return null
    val tmdb = int("tmdb")?.takeIf { it > 0 } ?: return null
    return DiscoverTitle(type, tmdb, text("title").orEmpty(), int("year"), optional("overview"), optional("poster"), optional("backdrop"),
        seerrStatus(int("status")), double("community"),
        text("genres")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty())
}

/**
 * The rows Spole's `discover` and `discover/related` answer, in the order Stova shows them whatever
 * order Spole sent. A row Stova does not know and a title it could not open are left out, a title
 * stands once per row, and a row with nothing left is no row.
 */
fun parseDiscoverRows(cursor: Cursor): List<DiscoverRow> {
    val rows = mutableMapOf<DiscoverRowKind, MutableMap<String, DiscoverTitle>>()
    while (cursor.moveToNext()) {
        val row = cursor.text("row")
        val kind = DiscoverRowKind.entries.firstOrNull { it.wire == row } ?: continue
        val title = cursor.discoverTitle() ?: continue
        val titles = rows.getOrPut(kind) { linkedMapOf() }
        if (title.key !in titles) titles[title.key] = title
    }
    return DiscoverRowKind.entries.mapNotNull { kind -> rows[kind]?.let { DiscoverRow(kind, it.values.toList()) } }
}

/** What the person may do with Seerr: the `seerr` extra, or null from a Spole that does not say (an older one). */
fun parseSeerrAccess(cursor: Cursor): SeerrAccess? = seerrAccess(cursor.extras?.getString("seerr"))

/** A title's page from the one row of `discover/title`, with the [seasons] of `discover/seasons` (none for a film). */
fun parseDiscoverTitle(cursor: Cursor, seasons: List<DiscoverSeason>): DiscoverDetails? {
    if (!cursor.moveToFirst()) return null
    val title = cursor.discoverTitle() ?: return null
    return DiscoverDetails(title, cursor.optional("tagline"), cursor.int("runtime"), cursor.int("critic"), cursor.int("can_request") == 1,
        cursor.int("request_id"), cursor.text("request_status")?.let(::requestStage), cursor.optional("trailer"), seasons)
}

/** The seasons of a series from `discover/seasons`. A row without a season number is left out. */
fun parseDiscoverSeasons(cursor: Cursor): List<DiscoverSeason> {
    val seasons = mutableListOf<DiscoverSeason>()
    while (cursor.moveToNext()) {
        val number = cursor.int("number")?.takeIf { it >= 0 } ?: continue
        seasons += DiscoverSeason(number, cursor.text("name").orEmpty(), cursor.int("episodes") ?: 0, seerrStatus(cursor.int("status")),
            cursor.optional("air_date"))
    }
    return seasons
}

/** The requests the person follows, from `discover/requests`. A title may have several (one in 4K), so none is dropped as a repeat. */
fun parseDiscoverRequests(cursor: Cursor): List<DiscoverRequest> {
    val requests = mutableListOf<DiscoverRequest>()
    while (cursor.moveToNext()) {
        val type = discoverType(cursor.text("type")) ?: continue
        val tmdb = cursor.int("tmdb")?.takeIf { it > 0 } ?: continue
        requests += DiscoverRequest(type, tmdb, cursor.text("title").orEmpty(), cursor.optional("poster"), cursor.optional("backdrop"),
            requestStage(cursor.text("stage")), cursor.int("percent"),
            cursor.text("seasons")?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty(), cursor.int("request_id"), cursor.int("is4k") == 1)
    }
    return requests
}

/** One page of `discover/search`: its titles, and the page and `has_more` extras (page 1, the last, from a Spole that sends none). */
fun parseDiscoverPage(cursor: Cursor): DiscoverPage {
    val extras = cursor.extras
    val items = parseDiscoverRows(cursor).firstOrNull { it.kind == DiscoverRowKind.SEARCH }?.items.orEmpty()
    return DiscoverPage(items, extras?.getInt("page", 1) ?: 1, extras?.getBoolean("has_more", false) ?: false)
}
