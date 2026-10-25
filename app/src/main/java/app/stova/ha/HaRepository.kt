package app.stova.ha

import app.stova.ha.cards.*
import app.stova.profiles.*
import java.util.UUID
import app.stova.core.storage.JsonFileStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HaRepository(private val socket: HaConnection, private val session: ProfileSession, private val profiles: ProfileRepository, scope: CoroutineScope,
    private val cache: JsonFileStore<HaSnapshot>? = null,
) {
    private val mutableEntities = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    val entities = mutableEntities.asStateFlow()
    val cards: StateFlow<List<CardConfig>> = session.active.map { it?.dashboard.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val homeCards: StateFlow<List<CardConfig>> = cards.map { it.filter(CardConfig::onHome).take(3) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    init {
        val writes = Channel<HaSnapshot>(Channel.CONFLATED)
        scope.launch {
            for (snapshot in writes) {
                try { cache?.update { snapshot } } catch (_: IOException) { /* Keep the live UI usable if disk is full. */ }
                // Conflate updates while waiting, preserving the most recent value for the next write.
                delay(5_000)
            }
        }
        scope.launch {
            var previousProfileId: String? = null
            session.active.map { it?.id to it?.dashboard.orEmpty() }.distinctUntilChanged().collectLatest { (profileId, cards) ->
                val ids = cards.map(CardConfig::entityId).toSet()
                socket.watch(ids)
                val retained = if (profileId == previousProfileId) entities.value.filterKeys { it in ids } else emptyMap()
                mutableEntities.value = retained
                previousProfileId = profileId
                val stored = cache?.current()
                val saved = stored?.takeIf { it.profileId == profileId }?.entities.orEmpty().filterKeys { it in ids } + retained
                mutableEntities.value = saved
                if (profileId != null && stored?.profileId != null) {
                    val pruned = HaSnapshot(profileId, snapshotEntities(cards, saved))
                    if (pruned != stored) writes.trySend(pruned)
                }
                socket.entitySnapshots(ids).collect { selected ->
                    mutableEntities.value = selected
                    if (cache != null) writes.trySend(HaSnapshot(profileId, snapshotEntities(cards, selected)))
                }
            }
        }
    }
    private suspend fun edit(change: (List<CardConfig>) -> List<CardConfig>) {
        val active = session.active.value?.takeUnless { it.kid } ?: return
        profiles.update(active.id) { it.copy(dashboard = change(it.dashboard)) }
    }
    suspend fun add(entity: HaEntity, type: CardType) {
        require(type in cardTypesFor(entity))
        edit { it + CardConfig(UUID.randomUUID().toString(), type, entity.id) }
    }
    suspend fun remove(cardId: String) = edit { it.filterNot { c -> c.id == cardId } }
    suspend fun move(cardId: String, delta: Int) = edit { list ->
        val from = list.indexOfFirst { it.id == cardId }
        if (from < 0) list else list.toMutableList().apply { add((from + delta).coerceIn(0,lastIndex), removeAt(from)) }
    }
    suspend fun setOnHome(cardId: String, on: Boolean): Boolean {
        var accepted = false
        edit { list ->
            val card = list.find { it.id == cardId }
            accepted = card != null && (!on || card.onHome || list.count { it.onHome } < 3)
            if (accepted) list.map { if (it.id == cardId) it.copy(onHome = on) else it } else list
        }
        return accepted
    }
    suspend fun setWide(cardId: String, wide: Boolean) = edit { it.map { c -> if (c.id == cardId) c.copy(wide = wide) else c } }
    suspend fun perform(call: ServiceCall, entityId: String, profile: Profile): Boolean =
        canControl(profile) && socket.callService(call.domain, call.service, entityId, call.data)
}
