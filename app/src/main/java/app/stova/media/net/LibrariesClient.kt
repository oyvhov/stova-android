package app.stova.media.net

import app.stova.media.BackgroundLibrary
import app.stova.media.model.MediaItem
import app.stova.profiles.Season
import app.stova.profiles.SpotlightSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** As many cards as one of Home's rows holds. */
private const val ROW_LIMIT = 20

/**
 * A child's borrowed account: one client per chosen library (each asking inside its own), seen as one.
 * Rows from several libraries take turns, so the first titles of each come first, without repeats.
 */
class LibrariesClient(private val parts: Map<String, MediaServerClient>) : MediaServerClient {
    override suspend fun resume() = merged { it.resume() }
    override suspend fun nextUp() = merged { it.nextUp() }
    override suspend fun latest(endpoint: Endpoint) = merged { it.latest(endpoint) }
    override suspend fun seasonal(season: Season) = merged { it.seasonal(season) }
    override suspend fun spotlight(source: SpotlightSource) = merged(limit = null) { it.spotlight(source) }
    override suspend fun searchTitles(query: String) = merged(limit = null) { it.searchTitles(query) }
    override suspend fun items(ids: List<String>) = parts.values.first().items(ids)
    /** The child keeps their own letter, never the grown-up's picture. */
    override suspend fun userImageTag(): String? = null
    override suspend fun backgroundLibraries(): List<BackgroundLibrary> =
        parts.values.first().backgroundLibraries().filter { it.ref.libraryId in parts }
    override suspend fun backgrounds(source: SpotlightSource, libraryId: String?, query: String?, ids: List<String>?): List<MediaItem> =
        if (libraryId != null) parts[libraryId]?.backgrounds(source, libraryId, query, ids).orEmpty()
        else merged(limit = null) { library, part -> part.backgrounds(source, library, query, ids) }

    private suspend fun merged(limit: Int? = ROW_LIMIT, fetch: suspend (String, MediaServerClient) -> List<MediaItem>): List<MediaItem> {
        val lists = coroutineScope { parts.map { (library, part) -> async { fetch(library, part) } }.awaitAll() }
        val turns = (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }
        return turns.distinctBy { it.id }.let { if (limit == null) it else it.take(limit) }
    }
    private suspend fun merged(limit: Int? = ROW_LIMIT, fetch: suspend (MediaServerClient) -> List<MediaItem>) =
        merged(limit) { _, part -> fetch(part) }
}
