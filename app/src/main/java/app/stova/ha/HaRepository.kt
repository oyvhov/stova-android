package app.stova.ha

import app.stova.ha.cards.*
import app.stova.profiles.*
import java.util.UUID
import app.stova.core.storage.JsonFileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HaFeedback { FAILED, DONE }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HaRepository(private val socket: HaConnection, private val session: ProfileSession, private val profiles: ProfileRepository, private val scope: CoroutineScope,
    private val cache: JsonFileStore<HaSnapshot>? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableEntities = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    private val optimistic = MutableStateFlow<Map<String,HaEntity>>(emptyMap())
    val entities = combine(mutableEntities,optimistic) { live,predicted -> live + predicted }
        .stateIn(scope,SharingStarted.Eagerly,emptyMap())
    private val mutablePending = MutableStateFlow<Set<String>>(emptySet())
    val pending = mutablePending.asStateFlow()
    private val mutableFeedback = MutableStateFlow<HaFeedback?>(null)
    val feedback = mutableFeedback.asStateFlow()
    private val mutableCompleted=MutableStateFlow<Set<String>>(emptySet())
    val completed=mutableCompleted.asStateFlow()
    private val receipts=mutableMapOf<String,kotlinx.coroutines.Job>()
    private val mutableRegistry = MutableStateFlow(HaRegistry())
    val registry = mutableRegistry.asStateFlow()
    private val registryMutex = Mutex()
    private var registryAt: Long? = null
    fun invalidateRegistry() { registryAt = null; mutableRegistry.value = HaRegistry() }
    suspend fun loadRegistry(): HaRegistry? = registryMutex.withLock {
        val now = clock()
        if (registryAt?.let { now - it in 0 until 6 * 60 * 60 * 1_000L } == true) return@withLock registry.value
        if (socket.status.value != HaStatus.CONNECTED) return@withLock registry.value.takeIf { it.entities.isNotEmpty() }
        socket.registry()?.also { mutableRegistry.value = it; registryAt = now }
            ?: registry.value.takeIf { it.entities.isNotEmpty() }
    }
    val cards: StateFlow<List<CardConfig>> = session.active.map { it?.dashboard.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val homeCards: StateFlow<List<CardConfig>> = cards.map { it.filter(CardConfig::onHome).take(3) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val focusedCards=mutableMapOf<String,String>()
    private val writes = HaSnapshotWriter(cache, scope)
    suspend fun flush() = writes.flush()
    private val dashboardVisible = MutableStateFlow(false)
    fun showingDashboard(shown: Boolean) {
        if (dashboardVisible.value == shown) return
        dashboardVisible.value = shown
        if (shown) socket.hold() else socket.release()
    }
    fun lastFocused(profileId:String):String?=focusedCards[profileId]
    fun rememberFocus(profileId:String,cardId:String) {focusedCards[profileId]=cardId}
    init {
        scope.launch {
            combine(socket.status, cards, dashboardVisible) { status, selected, dashboard -> status to (selected.isNotEmpty() || dashboard) }
                .distinctUntilChanged().collectLatest { (status, needed) ->
                if(status==HaStatus.CONNECTED && needed) {
                    // A failed fetch keeps the rooms we have; the next connection tries again.
                    loadRegistry()
                    val active=session.active.value
                    if(active!=null && mutableEntities.value.isNotEmpty()) writes.offer(HaSnapshot(active.id,snapshotEntities(active.dashboard,mutableEntities.value),selectedRegistry(active.dashboard)))
                }
            }
        }
        scope.launch {
            var previousProfileId: String? = null
            combine(session.active, dashboardVisible) { profile, dashboard ->
                val selected = profile?.dashboard.orEmpty()
                Triple(profile?.id, selected, if (dashboard) selected else if (profile?.appearance?.showHaChips == true) selected.filter { it.onHome }.take(3) else emptyList())
            }.distinctUntilChanged().collectLatest { (profileId, cards, visible) ->
                val ids = cards.map(CardConfig::entityId).toSet()
                val watched = visible.map(CardConfig::entityId).toSet()
                socket.watch(watched)
                if(profileId!=previousProfileId) {optimistic.value=emptyMap();mutablePending.value=emptySet();mutableFeedback.value=null;receipts.values.forEach {it.cancel()};receipts.clear();mutableCompleted.value=emptySet()}
                val retained = if (profileId == previousProfileId) mutableEntities.value.filterKeys { it in ids } else emptyMap()
                mutableEntities.value = retained
                previousProfileId = profileId
                val stored = cache?.current()
                if(mutableRegistry.value.entities.isEmpty() && stored!=null && stored.profileId==profileId) mutableRegistry.value=stored.registry
                val saved = stored?.takeIf { it.profileId == profileId }?.entities.orEmpty().filterKeys { it in ids } + retained
                mutableEntities.value = saved
                if (profileId != null && stored?.profileId != null) {
                    val pruned = HaSnapshot(profileId, snapshotEntities(cards, saved),selectedRegistry(cards))
                    if (pruned != stored) writes.offer(pruned)
                }
                socket.entitySnapshots(watched).collect { selected ->
                    // A live Home snapshot is authoritative only for its own small subscription.
                    val merged = mutableEntities.value.filterKeys { it !in watched } + selected
                    mutableEntities.value = merged
                    if (cache != null) writes.offer(HaSnapshot(profileId, snapshotEntities(cards, merged),selectedRegistry(cards)))
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
        if (from < 0) list else {
            val card=list[from]
            val room=registry.value.entities[card.entityId]?.room
            val section=list.indices.filter {i->session.active.value?.smartHome?.grouping != app.stova.ha.cards.DashboardGrouping.ROOM ||
                (list[i].favorite==card.favorite && (card.favorite || registry.value.entities[list[i].entityId]?.room==room))}
            val target=section[(section.indexOf(from)+delta).coerceIn(0,section.lastIndex)]
            list.toMutableList().apply {add(target,removeAt(from))}
        }
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
        val active=session.active.value
        if(active?.id!=profile.id || !canControl(active) || entityId in pending.value || entityId.startsWith("alarm_control_panel.") || call.domain=="alarm_control_panel") return false
        val before=mutableEntities.value[entityId]
        if(stateText(before)==StateText.Unavailable) return false
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
                    current!=null && predictionReceived(current,predicted,call)
                }}
            }
            mutableFeedback.value=if(success) HaFeedback.DONE else HaFeedback.FAILED
            if(success && entityId.substringBefore('.') in setOf("scene","script")) {
                receipts.remove(entityId)?.cancel()
                mutableCompleted.value+=entityId
                receipts[entityId]=scope.launch {delay(2_000);mutableCompleted.value-=entityId;receipts.remove(entityId)}
            }
            return success
        } finally {
            if(session.active.value?.id==profile.id) {
                optimistic.value-=entityId;mutablePending.value-=entityId
            }
        }
    }
}

private fun predictionReceived(current:HaEntity,predicted:HaEntity,call:ServiceCall):Boolean =
    current.state==predicted.state && call.data.keys.filterNot {it=="hvac_mode"}.all {key ->
        val attribute=when(key) {"brightness_pct"->"brightness";"position"->"current_position";else->key}
        val actual=current.attributes.number(attribute)
        val expected=predicted.attributes.number(attribute)
        if(actual!=null && expected!=null) kotlin.math.abs(actual-expected)<=if(attribute=="brightness") 1.0 else 0.001
        else current.attributes[attribute]==predicted.attributes[attribute]
    }
