package app.stova.discover

import app.stova.home.HomeRowKey
import app.stova.home.HomeRowSource
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.model.ItemArt
import app.stova.media.model.ItemType
import app.stova.media.model.MediaAccount
import app.stova.media.model.MediaItem
import app.stova.media.model.Ratings
import app.stova.media.seerrAccount
import app.stova.profiles.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Home's «Oppdag» row: the first twelve of Seerr's popular titles, asked through Spole as the profile's
 * Spole person. Nothing is asked for a child, or for a profile with nobody to ask as. The row comes from
 * [DiscoverRepository]'s last answer, so Home and the Discover page share one question to Spole; until
 * the first answer, and when Seerr is not there for the person, there is no row.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverRowSource(
    private val repository: DiscoverRepository,
    private val accounts: (String) -> Flow<List<MediaAccount>>,
    private val scope: CoroutineScope,
) : HomeRowSource {
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> =
        if (profile.kid) flowOf(emptyList())
        else accounts(profile.id).map { repository.identity(profile, it) }.distinctUntilChanged().flatMapLatest { link ->
            if (link == null) flowOf(emptyList())
            else {
                // In the app's scope: a profile switch mid-question leaves the answer for the next visit.
                scope.launch { repository.refresh(profile.id, link) }
                val account = seerrAccount(profile.id, link)
                repository.state(profile.id).map { state ->
                    val trending = state?.rows?.firstOrNull { it.kind == DiscoverRowKind.TRENDING }?.items.orEmpty()
                    if (trending.isEmpty()) emptyList()
                    else listOf(HomeRowUi.Media(HomeRowKey.DISCOVER, account, MediaRowKind.DISCOVER,
                        trending.take(ROW_SIZE).map { it.asMediaItem(account.id) }, offline = false))
                }.distinctUntilChanged()
            }
        }

    private companion object {
        const val ROW_SIZE = 12
    }
}

/** A Discover title as a card on Home; its id is the title's [DiscoverTitle.key], which [discoverRef] reads back. */
fun DiscoverTitle.asMediaItem(accountId: String): MediaItem = MediaItem(
    id = key, accountId = accountId, type = if (type == DiscoverType.MOVIE) ItemType.MOVIE else ItemType.SERIES, title = title,
    year = year, overview = overview, ratings = Ratings(community, null, null),
    art = ItemArt(backdrop = backdrop, logo = null, poster = poster, wide = backdrop), genres = genres)

/** The type and TMDB id a Discover card stands for (`movie-603`, `tv-1399`), or null for any other title. */
fun MediaItem.discoverRef(): Pair<DiscoverType, Int>? {
    val type = discoverType(id.substringBefore('-', "")) ?: return null
    val tmdbId = id.substringAfter('-').toIntOrNull()?.takeIf { it > 0 } ?: return null
    return type to tmdbId
}

/** The card as the title the Discover page opens with: what the card showed, while the page asks Seerr for the rest. */
fun MediaItem.asDiscoverTitle(): DiscoverTitle? = discoverRef()?.let { (type, tmdbId) ->
    DiscoverTitle(type, tmdbId, title, year, overview, art.poster, art.backdrop, SeerrStatus.UNKNOWN, ratings.community, genres)
}
