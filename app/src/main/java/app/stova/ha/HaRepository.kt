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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

enum class HaFeedback { FAILED, DONE }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HaRepository(private val socket: HaConnection, private val session: ProfileSession, private val profiles: ProfileRepository, scope: CoroutineScope,
    private val cache: JsonFileStore<HaSnapshot>? = null,
) {
    private val mutableEntities = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    private val optimistic = MutableStateFlow<Map<String,HaEntity>>(emptyMap())
    val entities = combine(mutableEntities,optimistic) { live,predicted -> live + predicted }
        .stateIn(scope,SharingStarted.Eagerly,emptyMap())
    private val mutablePending = MutableStateFlow<Set<String>>(emptySet())
    val pending = mutablePending.asStateFlow()
    private val mutableFeedback = MutableStateFlow<HaFeedback?>(null)
    val feedback = mutableFeedback.asStateFlow()
    private val mutableRegistry = MutableStateFlow(HaRegistry())
    val registry = mutableRegistry.asStateFlow()
    val cards: StateFlow<List<CardConfig>> = session.active.map { it?.dashboard.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val homeCards: StateFlow<List<CardConfig>> = cards.map { it.filter(CardConfig::onHome).take(3) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    init {
        val writes = Channel<HaSnapshot>(Channel.CONFLATED)
        scope.launch {
            socket.status.collectLatest { status ->
                if(status==HaStatus.CONNECTED) {
                    mutableRegistry.value=socket.registry()
                    val active=session.active.value
                    if(active!=null) writes.trySend(HaSnapshot(active.id,snapshotEntities(active.dashboard,mutableEntities.value),selectedRegistry(active.dashboard)))
                }
            }
        }
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
                if(profileId!=previousProfileId) {optimistic.value=emptyMap();mutablePending.value=emptySet();mutableFeedback.value=null}
                val retained = if (profileId == previousProfileId) mutableEntities.value.filterKeys { it in ids } else emptyMap()
                mutableEntities.value = retained
                previousProfileId = profileId
                val stored = cache?.current()
                if(mutableRegistry.value.entities.isEmpty() && stored!=null && stored.profileId==profileId) mutableRegistry.value=stored.registry
                val saved = stored?.takeIf { it.profileId == profileId }?.entities.orEmpty().filterKeys { it in ids } + retained
                mutableEntities.value = saved
                if (profileId != null && stored?.profileId != null) {
                    val pruned = HaSnapshot(profileId, snapshotEntities(cards, saved),selectedRegistry(cards))
                    if (pruned != stored) writes.trySend(pruned)
                }
                socket.entitySnapshots(ids).collect { selected ->
                    mutableEntities.value = selected
                    if (cache != null) writes.trySend(HaSnapshot(profileId, snapshotEntities(cards, selected),selectedRegistry(cards)))
                }
            }
        }
    }
    private fun selectedRegistry(cards:List<CardConfig>) = HaRegistry(registry.value.entities.filterKeys { id->cards.any {it.entityId==id} })
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
    suspend fun setWide(cardId: String, wide: Boolean) = edit { it.map { c -> if (c.id == cardId) c.copy(wide = wide,size=if(wide) CardSize.M else CardSize.S) else c } }
    suspend fun configure(cardId:String,size:CardSize,name:String?,icon:String?,favorite:Boolean) = edit { list ->
        list.map { if(it.id==cardId) it.copy(wide=false,size=size,name=name?.trim()?.take(80)?.takeIf(String::isNotBlank),icon=icon,favorite=favorite) else it }
    }
    fun dismissFeedback() {mutableFeedback.value=null}
    suspend fun perform(call: ServiceCall, entityId: String, profile: Profile): Boolean {
        if(!canControl(profile) || session.active.value?.id!=profile.id || entityId in pending.value) return false
        val before=mutableEntities.value[entityId]
        val predicted=before?.let {optimisticEntity(it,call)}
        mutablePending.value+=entityId
        if(predicted!=null) optimistic.value+=entityId to predicted
        try {
            val success=try {socket.callService(call.domain,call.service,entityId,call.data)}
                catch(e:CancellationException) {throw e} catch(_:Exception) {false}
            if(session.active.value?.id!=profile.id) return success
            if(success && predicted!=null && predicted!=before) {
                // Keep the pending display until the corresponding state event arrives, or a bounded timeout.
                withTimeoutOrNull(2_000) {mutableEntities.first { live ->
                    val current=live[entityId]
                    current!=null && current.state==predicted.state && (call.data.keys + if(call.data.containsKey("brightness_pct")) setOf("brightness") else emptySet())
                        .filterNot {it=="brightness_pct"}.all {current.attributes[it]==predicted.attributes[it]}
                }}
            }
            mutableFeedback.value=if(success) HaFeedback.DONE else HaFeedback.FAILED
            return success
        } finally {
            if(session.active.value?.id==profile.id) {
                optimistic.value-=entityId;mutablePending.value-=entityId
            }
        }
    }
}
