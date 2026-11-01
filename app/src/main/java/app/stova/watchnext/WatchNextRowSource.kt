package app.stova.watchnext
import app.stova.home.*
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.*
class WatchNextRowSource(private val reader: WatchNextReader): HomeRowSource {
    // A child's Home never has the row. Otherwise the profile's age limit applies to what other apps put
    // here as it does to the hero (2026-11-01: a 15-rated film from a grown-up's app lay on a six-year-old's Home).
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> = if (profile.kid) flowOf(emptyList()) else reader.observe().map { result ->
        val entries = withinAge((result as? WatchNextResult.Available)?.entries.orEmpty(), profile.appearance.backgroundFilter.maxAge)
        if (entries.isEmpty()) emptyList() else listOf(HomeRowUi.WatchNext(entries = entries))
    }
}
