package app.stova.ha

import app.stova.ha.cards.*
import app.stova.profiles.*
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HaRepository(private val socket: HaConnection, private val session: ProfileSession, private val profiles: ProfileRepository, scope: CoroutineScope) {
    val cards: StateFlow<List<CardConfig>> = session.active.map { it?.dashboard.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val homeCards: StateFlow<List<CardConfig>> = cards.map { it.filter(CardConfig::onHome).take(3) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    init { scope.launch { session.active.map { it?.dashboard.orEmpty().map(CardConfig::entityId).toSet() }.distinctUntilChanged().collect(socket::watch) } }
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
