package app.stova.media

import app.stova.home.HomeRowSource
import app.stova.home.HomeRowUi
import app.stova.media.model.MediaAccount
import app.stova.profiles.Profile
import app.stova.profiles.SpoleLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Rows from Spole for a profile that follows a Spole person and has no account of its own: Spole
 * fetches the person's «Hald fram», new films and new episodes with its own sign-in. Eilev's profile
 * shows his Emby rows without his password in Stova. A profile with an account keeps Stova's own rows,
 * and nothing is asked of Spole for a profile that follows nobody.
 *
 * The rows come at once from the last answer, then from Spole. Returning to Home asks again when the
 * last answer is older than [minAgeMillis]; an action on a title or Spole's report from playback asks
 * at once ([refresh] with `force`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpoleRowSource(
    private val active: Flow<Profile?>,
    private val accounts: (String) -> Flow<List<MediaAccount>>,
    private val fetch: suspend (profileId: String, link: SpoleLink) -> List<HomeRowUi.Media>,
    private val now: () -> Long = System::currentTimeMillis,
    private val minAgeMillis: Long = MIN_AGE_MILLIS,
) : HomeRowSource {
    private val answers = MutableStateFlow<Map<String, List<HomeRowUi.Media>>>(emptyMap())
    private val fetchedAt = mutableMapOf<String, Long>()
    private val requests = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 8)

    /** Asks Spole again for [profileId]'s rows; [force] skips the wait since the last answer. */
    fun refresh(profileId: String, force: Boolean = false) { requests.tryEmit(profileId to force) }

    /** The Spole person whose rows [profile] shows, or null while it follows nobody or has an account of its own. */
    private fun linkFor(profile: Profile): Flow<SpoleLink?> =
        // The profile's link can change in settings while Home runs, so it is read from the session.
        combine(active.map { current -> current?.takeIf { it.id == profile.id }?.spole ?: profile.spole.takeIf { current == null } },
            accounts(profile.id).map { it.isEmpty() }) { link, noAccounts -> link?.takeIf { noAccounts } }
            .distinctUntilChanged()

    /**
     * The hero's titles: [own], Stova's from its accounts, or for a profile whose rows come from Spole the
     * titles in those rows that have a backdrop, under the profile's own choice of source and age. Nothing
     * extra is asked of Spole; the titles follow the rows' last answer.
     */
    fun spotlight(profile: Profile, own: Flow<List<SpotlightEntry>>): Flow<List<SpotlightEntry>> =
        linkFor(profile).flatMapLatest { link ->
            if (link == null) own
            else answers.map { all -> spoleSpotlight(all[profile.id].orEmpty(), profile.appearance) }.distinctUntilChanged()
        }

    override fun rows(profile: Profile): Flow<List<HomeRowUi>> =
        linkFor(profile)
            .flatMapLatest { link ->
                if (link == null) flowOf(emptyList())
                else channelFlow {
                    send(answers.value[profile.id].orEmpty())
                    suspend fun ask() {
                        val rows = fetch(profile.id, link)
                        fetchedAt[profile.id] = now()
                        answers.update { it + (profile.id to rows) }
                        send(rows)
                    }
                    ask()
                    requests.filter { (id, force) ->
                        id == profile.id && (force || now() - (fetchedAt[id] ?: 0L) >= minAgeMillis)
                    }.collect { ask() }
                }
            }

    internal companion object {
        /** A title once, with a backdrop, in the rows' order; libraries are Stova's own, so a choice of them does not apply. */
        fun spoleSpotlight(rows: List<HomeRowUi.Media>, look: app.stova.profiles.AppearancePrefs): List<SpotlightEntry> =
            rows.flatMap { row -> row.items.filter { it.art.backdrop != null }.map { SpotlightEntry(it, row.account, newSeason = false) } }
                .distinctBy { it.item.id }
                .filter { acceptsSpotlight(it.item, look.spotlightSource) && acceptsBackground(it.item, look.backgroundFilter.copy(libraries = null)) }

        /** Coming back to Home within a minute shows the last answer instead of asking the server again. */
        const val MIN_AGE_MILLIS = 60_000L
    }
}
