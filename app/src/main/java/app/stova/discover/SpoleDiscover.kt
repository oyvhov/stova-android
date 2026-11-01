package app.stova.discover

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import app.stova.media.SpoleHandoff
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks the installed Spole for Discover (`content://<package>.launcher/discover…`); Spole asks the
 * person's Seerr with its own sign-in. A Spole that knows Discover says in the `seerr` extra of every
 * answer what the person may do. An older one answers with an empty cursor without it, which is no
 * answer, and the next installed Spole is asked. Nothing is asked when Spole is not installed.
 * Blocks while Spole asks Seerr: call it off the main thread, as [SpoleDiscoverFetch] does.
 */
class SpoleDiscover(private val context: Context) {
    fun rows(link: SpoleLink): DiscoverAnswer? = first { pkg ->
        ask(pkg, link, "discover") { cursor -> parseSeerrAccess(cursor)?.let { DiscoverAnswer(it, parseDiscoverRows(cursor)) } }
    }

    /** [refresh] has Spole ask Seerr for the requests' state first. */
    fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>? = first { pkg ->
        ask(pkg, link, "discover/requests", if (refresh) listOf("refresh" to "1") else emptyList(), ::parseDiscoverRequests)
    }

    fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? = first { pkg ->
        val params = listOf("type" to type.wire, "tmdb" to "$tmdbId")
        ask(pkg, link, "discover/title", params) { cursor ->
            parseDiscoverTitle(cursor, emptyList())?.let { details ->
                // A series' seasons come from the Spole that answered for it; a film has none.
                if (type != DiscoverType.SERIES) details
                else details.copy(seasons = ask(pkg, link, "discover/seasons", params, ::parseDiscoverSeasons)?.value.orEmpty())
            }
        }
    }

    fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> = first { pkg ->
        ask(pkg, link, "discover/related", listOf("type" to type.wire, "tmdb" to "$tmdbId"), ::parseDiscoverRows)
    }.orEmpty()

    fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? = first { pkg ->
        ask(pkg, link, "discover/search", listOf("q" to query, "page" to "$page"), ::parseDiscoverPage)
    }

    fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>): DiscoverActionResult =
        act("discover_request", link) { putString("type", type.wire); putInt("tmdb", tmdbId); putIntArray("seasons", seasons.toIntArray()) }

    fun withdraw(link: SpoleLink, requestId: Int): DiscoverActionResult = act("discover_withdraw", link) { putInt("request_id", requestId) }

    /** An answer from a Spole that knows Discover, even when what it holds is nothing. */
    private class Answered<T>(val value: T)

    /** What the first installed Spole that answers holds; null when none does. */
    private fun <T> first(answer: (pkg: String) -> Answered<T>?): T? = SpoleHandoff(context).installedPackages().firstNotNullOfOrNull(answer)?.value

    /** [pkg]'s answer to [path] for the person [link] names, read by [read]; null when it has none or trips. */
    private fun <T> ask(pkg: String, link: SpoleLink, path: String, params: List<Pair<String, String>> = emptyList(), read: (Cursor) -> T): Answered<T>? =
        runCatching {
            val uri = Uri.parse("content://$pkg.launcher/$path").buildUpon()
                .appendQueryParameter("kind", link.kind.name.lowercase()).appendQueryParameter("user", link.userId)
                .apply { params.forEach { (key, value) -> appendQueryParameter(key, value) } }.build()
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (parseSeerrAccess(cursor) == null) null else Answered(read(cursor))
            }
        }.getOrNull()

    /**
     * [method] sent to the first installed Spole that answers it, and only that one, so a request is
     * never sent twice. An older Spole answers nothing. Numbers go as Int: Spole reads a Long as 0.
     */
    private fun act(method: String, link: SpoleLink, extras: Bundle.() -> Unit): DiscoverActionResult =
        discoverActionResult(SpoleHandoff(context).installedPackages().firstNotNullOfOrNull { pkg ->
            runCatching {
                context.contentResolver.call(Uri.parse("content://$pkg.launcher"), method, null, Bundle().apply {
                    putString("kind", link.kind.name.lowercase()); putString("user", link.userId); extras()
                })?.getString("result")
            }.getOrNull()
        })
}

/** What [DiscoverRepository] asks of Spole: [SpoleDiscover]'s questions, suspended. */
interface DiscoverFetch {
    suspend fun rows(link: SpoleLink): DiscoverAnswer?
    suspend fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>?
    suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails?
    suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow>
    suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage?
    suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>): DiscoverActionResult
    suspend fun withdraw(link: SpoleLink, requestId: Int): DiscoverActionResult
}

/** [SpoleDiscover] off the main thread. */
class SpoleDiscoverFetch(private val spole: SpoleDiscover) : DiscoverFetch {
    override suspend fun rows(link: SpoleLink): DiscoverAnswer? = withContext(Dispatchers.IO) { spole.rows(link) }
    override suspend fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>? = withContext(Dispatchers.IO) { spole.requests(link, refresh) }
    override suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? =
        withContext(Dispatchers.IO) { spole.details(link, type, tmdbId) }
    override suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> =
        withContext(Dispatchers.IO) { spole.related(link, type, tmdbId) }
    override suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? = withContext(Dispatchers.IO) { spole.search(link, query, page) }
    override suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>): DiscoverActionResult =
        withContext(Dispatchers.IO) { spole.request(link, type, tmdbId, seasons) }
    override suspend fun withdraw(link: SpoleLink, requestId: Int): DiscoverActionResult = withContext(Dispatchers.IO) { spole.withdraw(link, requestId) }
}
