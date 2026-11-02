package app.stova.discover

import app.stova.media.model.MediaAccount
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What Discover shows for a profile: what the person may do, the rows, the requests they follow, and how the last ask went. */
data class DiscoverState(val access: SeerrAccess, val rows: List<DiscoverRow>, val requests: List<DiscoverRequest>, val loading: Boolean,
    val failed: Boolean)

/** Nobody answered, or Seerr is not there for the person: nothing to show, nothing being asked, and nothing went wrong. */
val DiscoverState.nothingToShow: Boolean get() = access == SeerrAccess.NONE && !loading && !failed

/**
 * Discover from Spole, kept in memory so the page and Home's row open with what Stova has and ask again
 * quietly. [refresh] asks only when the last good answer is older than [minAgeMillis], unless forced; when
 * Spole trips, the rows already shown stay and the state says it failed. The person's requests come with
 * the rows, from a Spole that has Seerr for them; Spole tripping on those alone keeps the ones shown and
 * fails nothing. A title's page and its related rows are kept five minutes per person, and forgotten when
 * a request changes the title.
 */
class DiscoverRepository(
    private val fetch: DiscoverFetch,
    private val now: () -> Long = System::currentTimeMillis,
    private val minAgeMillis: Long = MIN_AGE_MILLIS,
) {
    private val states = ConcurrentHashMap<String, MutableStateFlow<DiscoverState?>>()
    private val fetchedAt = ConcurrentHashMap<String, Long>()
    private val requestsAt = ConcurrentHashMap<String, Long>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val titles = ConcurrentHashMap<TitleKey, Kept<DiscoverDetails>>()
    private val related = ConcurrentHashMap<TitleKey, Kept<List<DiscoverRow>>>()

    /** Null until Spole has first been asked for [profileId]. */
    fun state(profileId: String): StateFlow<DiscoverState?> = stateOf(profileId).asStateFlow()

    private fun stateOf(profileId: String) = states.computeIfAbsent(profileId) { MutableStateFlow(null) }

    suspend fun refresh(profileId: String, link: SpoleLink, force: Boolean = false) {
        // One ask at a time per profile: one that waited finds the answer fresh and asks nothing.
        locks.computeIfAbsent(profileId) { Mutex() }.withLock {
            if (!force && fetchedAt[profileId]?.let { now() - it < minAgeMillis } == true) return
            val state = stateOf(profileId)
            state.update { (it ?: EMPTY).copy(loading = true) }
            try {
                val answer = fetch.rows(link)
                state.update { answered(it ?: EMPTY, answer) }
                // The rows show at once; the requests follow, asked only of a Spole that has Seerr for the person.
                if (answer != null && answer.access != SeerrAccess.NONE) {
                    requests(profileId, link, force)?.let { requests -> state.update { it?.copy(requests = requests) } }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { (it ?: EMPTY).copy(loading = false, failed = true) }
            } finally {
                state.update { it?.copy(loading = false) }
            }
            // Only a good answer is fresh: after a failure the next return asks again.
            if (state.value?.failed == false) fetchedAt[profileId] = now() else fetchedAt.remove(profileId)
        }
    }

    /**
     * The person's requests, with Spole asking Seerr for their state first when forced or when the last look that
     * answered is older than [minAgeMillis]; none when Spole tripped or nobody answered, so the ones shown stay.
     */
    private suspend fun requests(profileId: String, link: SpoleLink, force: Boolean): List<DiscoverRequest>? {
        val old = requestsAt[profileId]?.let { now() - it >= minAgeMillis } ?: true
        return orNull { fetch.requests(link, refresh = force || old) }?.also { requestsAt[profileId] = now() }
    }

    private fun answered(old: DiscoverState, answer: DiscoverAnswer?): DiscoverState = when {
        // No Spole answered, or Seerr is not for this person: nothing to show, and nothing went wrong.
        answer == null || answer.access == SeerrAccess.NONE -> EMPTY
        // Seerr is there but gave no rows: a hiccup, so the rows already shown stay.
        answer.rows.isEmpty() -> old.copy(access = answer.access, loading = false, failed = true)
        else -> old.copy(access = answer.access, rows = answer.rows, loading = false, failed = false)
    }

    suspend fun details(link: SpoleLink, type: DiscoverType, tmdbId: Int): DiscoverDetails? =
        kept(titles, TitleKey(link, type, tmdbId)) { fetch.details(link, type, tmdbId) }

    suspend fun related(link: SpoleLink, type: DiscoverType, tmdbId: Int): List<DiscoverRow> =
        kept(related, TitleKey(link, type, tmdbId)) { fetch.related(link, type, tmdbId).ifEmpty { null } }.orEmpty()

    /** Forgets what is kept of a title, for everyone: its state in Seerr has changed. */
    fun invalidate(type: DiscoverType, tmdbId: Int) {
        titles.keys.removeAll { it.type == type && it.tmdbId == tmdbId }
        related.keys.removeAll { it.type == type && it.tmdbId == tmdbId }
    }

    suspend fun search(link: SpoleLink, query: String, page: Int): DiscoverPage? = orNull { fetch.search(link, query, page) }

    suspend fun request(link: SpoleLink, type: DiscoverType, tmdbId: Int, seasons: List<Int>): DiscoverActionResult =
        fetch.request(link, type, tmdbId, seasons).also { if (it == DiscoverActionResult.DONE) invalidate(type, tmdbId) }

    /** A withdrawal names only the request, so the title forgotten is the kept page that carries it. */
    suspend fun withdraw(link: SpoleLink, requestId: Int): DiscoverActionResult = fetch.withdraw(link, requestId).also { result ->
        if (result == DiscoverActionResult.DONE) titles.filterValues { it.value.requestId == requestId }.keys.forEach { invalidate(it.type, it.tmdbId) }
    }

    /** The Spole person [profile] is in Discover: the one chosen in settings, else its first account. A child has no Discover. */
    fun identity(profile: Profile, accounts: List<MediaAccount>): SpoleLink? =
        if (profile.kid) null else profile.spole ?: accounts.firstOrNull()?.let { SpoleLink(it.kind, it.userId) }

    /** A page holds the person's own request, so it is kept per person. */
    private data class TitleKey(val link: SpoleLink, val type: DiscoverType, val tmdbId: Int)
    private class Kept<T>(val value: T, val at: Long)

    /** What is kept for [key] while younger than five minutes, else [ask]'s answer; no answer is not kept, so the next look asks again. */
    private suspend fun <T : Any> kept(cache: ConcurrentHashMap<TitleKey, Kept<T>>, key: TitleKey, ask: suspend () -> T?): T? {
        cache[key]?.takeIf { now() - it.at < TITLE_MAX_AGE_MILLIS }?.let { return it.value }
        return orNull(ask)?.also { cache[key] = Kept(it, now()) }
    }

    /** [ask]'s answer, or none when Spole tripped: a page shows what it has and asks again on the next look. */
    private suspend fun <T> orNull(ask: suspend () -> T?): T? = try {
        ask()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        /** Coming back to Discover within ten minutes shows the last answer instead of asking Seerr again. */
        const val MIN_AGE_MILLIS = 600_000L
        private const val TITLE_MAX_AGE_MILLIS = 300_000L
        private val EMPTY = DiscoverState(SeerrAccess.NONE, emptyList(), emptyList(), loading = false, failed = false)
    }
}
