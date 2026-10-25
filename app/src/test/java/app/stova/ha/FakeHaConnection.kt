package app.stova.ha

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject

class FakeHaConnection : HaConnection {
    override val status = MutableStateFlow(HaStatus.CONNECTED)
    override val entities = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    val watches = mutableListOf<Set<String>>()
    var calls = 0
    var allStates = emptyList<HaEntity>()
    var registryValue = HaRegistry()
    var stateRequests = 0
    var service: suspend (String,String,String,JsonObject) -> Boolean = {_,_,_,_->true}
    override fun watch(entityIds: Set<String>) { watches += entityIds }
    override suspend fun callService(domain: String, service: String, entityId: String, data: JsonObject): Boolean { calls++; return this.service(domain,service,entityId,data) }
    override suspend fun history(entityId: String, hours: Int) = emptyList<Pair<Long, Double>>()
    override suspend fun states():List<HaEntity> {stateRequests++; return allStates}
    override suspend fun registry() = registryValue
}
