package app.stova.watchnext
import app.stova.home.*
import app.stova.profiles.Profile
import kotlinx.coroutines.flow.*
class WatchNextRowSource(private val reader: WatchNextReader): HomeRowSource {
    override fun rows(profile: Profile): Flow<List<HomeRowUi>> = if (profile.kid) flowOf(emptyList()) else reader.observe().map { result ->
        val entries = (result as? WatchNextResult.Available)?.entries.orEmpty()
        if (entries.isEmpty()) emptyList() else listOf(HomeRowUi.WatchNext(entries = entries))
    }
}
