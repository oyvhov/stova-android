package app.stova.discover

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import app.stova.media.SpoleHandoff
import app.stova.profiles.SpoleLink
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

/**
 * Asks the installed Spole for Discover (`content://<package>.launcher/discover…`); Spole asks the
 * person's Seerr with its own sign-in. A Spole that knows Discover says in the `seerr` extra of every
 * answer what the person may do. An older one answers with an empty cursor without it, which is no
 * answer, and the next installed Spole is asked. Nothing is asked when Spole is not installed. A
 * question no Spole answered because one tripped throws, so a hiccup is never taken for "no Discover".
 * Blocks while Spole asks Seerr: call it off the main thread and bounded in time, as [SpoleDiscoverFetch] does.
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

    /**
     * What the first installed Spole that answers holds; null when none does (no Spole, or only older
     * ones). When none answered and one tripped, the last failure is thrown.
     */
    private fun <T> first(answer: (pkg: String) -> Answered<T>?): T? {
        var tripped: Exception? = null
        for (pkg in SpoleHandoff(context).installedPackages()) {
            try { answer(pkg)?.let { return it.value } } catch (e: Exception) { tripped = e }
        }
        tripped?.let { throw it }
        return null
    }

    /** [pkg]'s answer to [path] for the person [link] names, read by [read]; null when it has none, and a throw when it tripped. */
    private fun <T> ask(pkg: String, link: SpoleLink, path: String, params: List<Pair<String, String>> = emptyList(), read: (Cursor) -> T): Answered<T>? {
        val uri = Uri.parse("content://$pkg.launcher/$path").buildUpon()
            .appendQueryParameter("kind", link.kind.name.lowercase()).appendQueryParameter("user", link.userId)
            .apply { params.forEach { (key, value) -> appendQueryParameter(key, value) } }.build()
        // Spole always gives a cursor. Android gives none for a Spole without the provider, and for one that
        // died or threw what cannot cross to Stova: only the first is no answer.
        val cursor = context.contentResolver.query(uri, null, null, null, null)
            ?: if (context.packageManager.resolveContentProvider("$pkg.launcher", 0) == null) return null
            else throw IllegalStateException("$pkg gave no cursor for $path")
        return cursor.use { if (parseSeerrAccess(it) == null) null else Answered(read(it)) }
    }

    /**
     * [method] sent to the first installed Spole that answers it, and only that one, so a request is
     * never sent twice. An older Spole answers nothing; a Spole that tripped makes it [DiscoverActionResult.FAILED]
     * when no other answered. Numbers go as Int: Spole reads a Long as 0.
     */
    private fun act(method: String, link: SpoleLink, extras: Bundle.() -> Unit): DiscoverActionResult {
        var tripped = false
        for (pkg in SpoleHandoff(context).installedPackages()) {
            val word = try {
                context.contentResolver.call(Uri.parse("content://$pkg.launcher"), method, null, Bundle().apply {
                    putString("kind", link.kind.name.lowercase()); putString("user", link.userId); extras()
                })?.getString("result")
            } catch (e: Exception) { tripped = true; null }
            if (word != null) return discoverActionResult(word)
        }
        return if (tripped) DiscoverActionResult.FAILED else DiscoverActionResult.NOT_SUPPORTED
    }
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

/**
 * [SpoleDiscover] off the main thread, each question given [timeoutMillis] to be answered. Spole asks Seerr while Stova
 * waits, and a Seerr that takes the connection but never answers would leave the page fetching for good and the next
 * questions queued behind it: a question not answered in time fails as one where Spole tripped, so the rows shown stay
 * and the page can try again. A request or a withdrawal not answered in time is [DiscoverActionResult.FAILED].
 */
class SpoleDiscoverFetch(private val spole: SpoleDiscover, private val timeoutMillis: Long = FETCH_TIMEOUT_MILLIS) : DiscoverFetch {
    // A binder call cannot be stopped, and a coroutine waits for what it started on another thread before it ends. The
    // questions are asked here, apart from the one who asked, so the wait ends on time and a late answer is dropped.
    private val questions = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun rows(link: SpoleLink): DiscoverAnswer? = ask { spole.rows(link) }
    override suspend fun requests(link: SpoleLink, refresh: Boolean): List<DiscoverRequest>? = ask { spole.requests(link, refresh) }
    override suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? = ask { spole.details(link, type, tmdbId) }
    override suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> = ask { spole.related(link, type, tmdbId) }
    override suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? = ask { spole.search(link, query, page) }
    override suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>): DiscoverActionResult =
        act { spole.request(link, type, tmdbId, seasons) }
    override suspend fun withdraw(link: SpoleLink, requestId: Int): DiscoverActionResult = act { spole.withdraw(link, requestId) }

    /** [question]'s answer, or an [IOException] when Spole has not answered within [timeoutMillis]; the caller's own cancellation stays one. */
    private suspend fun <T> ask(question: () -> T): T {
        val answer = questions.async { question() }
        return try {
            withTimeout(timeoutMillis) { answer.await() }
        } catch (e: TimeoutCancellationException) {
            throw IOException("Spole gave no answer in $timeoutMillis ms", e)
        } finally {
            answer.cancel()
        }
    }

    /** [send]'s word; Spole tells of its own failures as words, so the only throw left is the wait running out. */
    private suspend fun act(send: () -> DiscoverActionResult): DiscoverActionResult = try {
        ask(send)
    } catch (e: IOException) {
        DiscoverActionResult.FAILED
    }

    companion object {
        /** How long Stova waits for Spole's answer, Seerr's time included, before a question counts as failed. */
        const val FETCH_TIMEOUT_MILLIS = 15_000L
    }
}
