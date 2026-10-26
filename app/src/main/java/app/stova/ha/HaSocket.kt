package app.stova.ha

import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

enum class HaStatus { NOT_CONFIGURED, DISCONNECTED, CONNECTING, CONNECTED, SIGNED_OUT }

/** UI and repositories depend on the connection contract; transport details stay here. */
interface HaConnection {
    val timeZone:ZoneId get()=ZoneId.systemDefault()
    val status: StateFlow<HaStatus>
    val entities: StateFlow<Map<String, HaEntity>>
    /** Received snapshots, excluding the uninitialised empty StateFlow value. */
    fun entitySnapshots(entityIds: Set<String>): Flow<Map<String, HaEntity>> =
        entities.map { live -> live.filterKeys { it in entityIds } }.dropWhile { it.isEmpty() }
    fun watch(entityIds: Set<String>)
    /** Pages that need live data while shown hold the connection; with no holder and no watched id the socket rests. */
    fun hold() {}
    fun release() {}
    suspend fun callService(domain: String, service: String, entityId: String, data: JsonObject = JsonObject(emptyMap())): Boolean
    suspend fun history(entityId: String, hours: Int = 24): List<Pair<Long, Double>>
    suspend fun states(): List<HaEntity>
    /** Null when any of the three lists failed: a partial registry would erase the rooms. */
    suspend fun registry(): HaRegistry? = HaRegistry()
    suspend fun forecast(entityId: String): List<ForecastDay> = emptyList()
    suspend fun hourlyForecast(entityId:String):List<ForecastDay> = emptyList()
    suspend fun energy(entity:HaEntity):HaEntity = entity
}

class HaSocket(
    http: OkHttpClient,
    private val auth: HaAuth,
    private val address: suspend () -> HaAddress?,
    private val scope: CoroutineScope,
    private val backoff: List<Long> = listOf(1_000, 2_000, 4_000, 8_000, 16_000, 30_000, 60_000, 120_000, 300_000),
    private val graceMillis: Long = 30_000,
) : HaConnection {
    private val http = http.newBuilder().pingInterval(30, TimeUnit.SECONDS).build()
    private val mutableStatus = MutableStateFlow(HaStatus.NOT_CONFIGURED)
    override val status = mutableStatus.asStateFlow()
    private val mutableEntities = MutableStateFlow<Map<String, HaEntity>>(emptyMap())
    override val entities = mutableEntities.asStateFlow()
    private data class WatchedEntities(val ids: Set<String>, val values: Map<String, HaEntity>)
    private val snapshots = MutableStateFlow<WatchedEntities?>(null)
    override fun entitySnapshots(entityIds: Set<String>): Flow<Map<String, HaEntity>> =
        snapshots.filterNotNull().filter { it.ids == entityIds }.map { it.values }
    private val inbox = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var socket: WebSocket? = null
    private var running = false
    private var holders = 0
    private var resting: Job? = null
    private val demanded get() = watched.isNotEmpty() || holders > 0
    private var generation = 0
    private var nextId = 1
    private var subscription: Int? = null
    private var awaitingSnapshot = true
    private var watched = emptySet<String>()
    private var failures = 0
    private var authFailures = 0
    private var reconnect: Job? = null
    private var stopping: Job? = null
    private val pending = mutableMapOf<Int, CompletableDeferred<JsonObject?>>()
    private val priceMutex=Mutex()
    private var priceResponse:JsonObject?=null
    private var pricesAt=0L
    private val histories = HaReadCache<List<Pair<Long, Double>>>()
    private val forecasts = HaReadCache<List<ForecastDay>>()
    /** Integrations Home Assistant has loaded (`get_config` components); null until first known. */
    private val components=MutableStateFlow<Set<String>?>(null)
    @Volatile private var homeZone=ZoneId.systemDefault()
    override val timeZone:ZoneId get()=homeZone
    init {
        require(backoff.isNotEmpty())
        scope.launch {
            try { for (action in inbox) action() }
            finally { socket?.cancel(); pending.values.forEach { it.complete(null) }; inbox.close() }
        }
    }
    private fun post(action: suspend () -> Unit) { inbox.trySend(action) }
    fun start() = post {
        stopping?.cancel(); stopping = null; running = true
        reconcileDemand()
    }
    fun stop(graceMillis: Long = this.graceMillis) = post {
        stopping?.cancel()
        stopping = scope.launch {
            delay(graceMillis)
            post { running = false; reconnect?.cancel(); disconnect(); mutableStatus.value = if (address() == null) HaStatus.NOT_CONFIGURED else HaStatus.DISCONNECTED }
        }
    }
    fun reconfigure() = post {
        histories.clear(); forecasts.clear(); components.value = null; priceResponse = null; pricesAt = 0L
        reconnect?.cancel(); disconnect(); mutableEntities.value = emptyMap(); snapshots.value = null; authFailures = 0; failures = 0
        mutableStatus.value = if (address() == null) HaStatus.NOT_CONFIGURED else HaStatus.DISCONNECTED
        reconcileDemand()
    }
    override fun watch(entityIds: Set<String>) = post {
        if (entityIds != watched) {
            watched = entityIds.toSet()
            snapshots.value = null
            mutableEntities.value = entities.value.filterKeys { it in watched }
            if (status.value == HaStatus.CONNECTED) resubscribe()
        }
        reconcileDemand()
    }
    override fun hold() = post { holders++; reconcileDemand() }
    override fun release() = post { holders = (holders - 1).coerceAtLeast(0); reconcileDemand() }

    private suspend fun reconcileDemand() {
        if (!running) return
        if (demanded) {
            resting?.cancel(); resting = null
            if (socket == null && reconnect?.isActive != true && status.value != HaStatus.SIGNED_OUT) connect()
        } else {
            reconnect?.cancel(); reconnect = null
            if (socket == null) mutableStatus.value = if (address() == null) HaStatus.NOT_CONFIGURED else HaStatus.DISCONNECTED
            else if (resting?.isActive != true) resting = scope.launch {
                delay(graceMillis)
                post { if (!demanded) { disconnect(); mutableStatus.value = HaStatus.DISCONNECTED } }
            }
        }
    }
    private fun resubscribe() {
        subscription?.let { socket?.send(unsubscribe(nextId++, it)) }
        subscription = if (watched.isEmpty()) null else nextId++.also { socket?.send(subscribeEntities(it, watched)) }
        awaitingSnapshot = true
    }
    private fun disconnect() {
        generation++; socket?.cancel(); socket = null; subscription = null
        pending.values.forEach { it.complete(null) }; pending.clear()
    }
    private suspend fun connect() {
        if (!running || !demanded) return
        val target = address() ?: run { mutableStatus.value = HaStatus.NOT_CONFIGURED; return }
        mutableStatus.value = HaStatus.CONNECTING
        val token = try { auth.accessToken() }
        catch (_: HaSignedOut) { mutableStatus.value = HaStatus.SIGNED_OUT; return }
        catch (failure: java.io.IOException) {
            if (app.stova.BuildConfig.DEBUG) android.util.Log.w("StovaHa", "Authentication transport: ${failure.javaClass.simpleName}")
            retry(); return
        }
        val epoch = ++generation
        nextId = 1
        socket = http.newWebSocket(Request.Builder().url(target.websocket).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) = post {
                if (epoch != generation) return@post
                val messages = runCatching { splitIncoming(text) }.getOrDefault(emptyList())
                for (message in messages) {
                    if (epoch != generation) break
                    when (message["type"]?.jsonPrimitive?.content) {
                        "auth_required" -> webSocket.send(authMessage(token))
                        "auth_ok" -> {
                            failures = 0; authFailures = 0; mutableStatus.value = HaStatus.CONNECTED
                            webSocket.send(supportedFeatures(nextId++)); resubscribe()
                        }
                        "auth_invalid" -> {
                            disconnect(); authFailures++
                            if (authFailures >= 2) { auth.reject(); mutableStatus.value = HaStatus.SIGNED_OUT }
                            else { auth.invalidateAccess(); connect() }
                        }
                        "event" -> if (message["id"]?.jsonPrimitive?.intOrNull == subscription) {
                            val event = message["event"] as? JsonObject ?: continue
                            val previous = if (awaitingSnapshot) emptyMap() else entities.value
                            mutableEntities.value = EntityStateDecoder.apply(previous, event).filterKeys { it in watched }
                            awaitingSnapshot = false
                            // Even an empty first response is authoritative: cached entities may have been deleted.
                            snapshots.value = WatchedEntities(watched, mutableEntities.value)
                        }
                        "result" -> message["id"]?.jsonPrimitive?.intOrNull?.let { pending.remove(it)?.complete(message) }
                    }
                }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, reason) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = post { if (epoch == generation) retry() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = post {
                if (epoch == generation) {
                    // No tokens, addresses or response bodies in diagnostics.
                    if (app.stova.BuildConfig.DEBUG) android.util.Log.w("StovaHa", "WebSocket transport: ${t.javaClass.simpleName}, HTTP ${response?.code}")
                    retry()
                }
            }
        })
    }
    private fun retry() {
        disconnect(); mutableStatus.value = HaStatus.DISCONNECTED
        if (!running || !demanded) return
        reconnect?.cancel()
        val wait = backoff[failures.coerceAtMost(backoff.lastIndex)]; failures++
        reconnect = scope.launch { delay(wait); post { if (running) connect() } }
    }
    private suspend fun request(build: (Int) -> String): JsonObject? {
        val answer = CompletableDeferred<JsonObject?>()
        var id: Int? = null
        post {
            if (status.value != HaStatus.CONNECTED) answer.complete(null)
            else {
                val key = nextId++; id = key; pending[key] = answer
                if (socket?.send(build(key)) != true) pending.remove(key)?.complete(null)
            }
        }
        return try { withTimeoutOrNull(8_000) { answer.await() } }
        finally { post { id?.let { pending.remove(it) } } }
    }
    override suspend fun callService(domain: String, service: String, entityId: String, data: JsonObject): Boolean =
        request { app.stova.ha.callService(it, domain, service, entityId, data) }?.get("success")?.jsonPrimitive?.booleanOrNull == true
    override suspend fun history(entityId: String, hours: Int): List<Pair<Long, Double>> = histories.get("$entityId/$hours", 300_000) {
        val response = request { historyRequest(it, entityId, Instant.now().minusSeconds(hours.toLong() * 3600)) }
        (response?.get("result") as? JsonObject)?.let { parseHistory(it, entityId) }.orEmpty()
    }
    override suspend fun states(): List<HaEntity> = withContext(Dispatchers.Default) { (request(::getStates)?.get("result") as? JsonArray)?.let(::parseStates)
        ?: throw java.io.IOException("HA states unavailable") }
    override suspend fun registry(): HaRegistry? = coroutineScope {
        val config=async {request(::getConfig)?.get("result") as? JsonObject}
        val results = HaRegistryKind.entries.map { kind -> async {
            request { registryRequest(it, kind) }?.get("result") as? JsonArray
        } }.awaitAll()
        val settings=config.await()
        settings?.text("time_zone")?.let {zone->runCatching {ZoneId.of(zone)}.getOrNull()?.let {homeZone=it}}
        (settings?.get("components") as? JsonArray)?.let {list->components.value=list.mapNotNull {(it as? JsonPrimitive)?.contentOrNull}.toSet()}
        if(results.any { it==null }) null
        // Thousands of entries: parse off the main thread the socket's callers run on.
        else withContext(Dispatchers.Default) { parseRegistry(results[0]!!, results[1]!!, results[2]!!) }
    }
    override suspend fun forecast(entityId: String): List<ForecastDay> = forecasts.get("$entityId/daily", 900_000) {
        (request { forecastRequest(it, entityId) }?.get("result") as? JsonObject)?.let { parseForecast(it, entityId) }.orEmpty()
    }
    override suspend fun hourlyForecast(entityId:String):List<ForecastDay> = forecasts.get("$entityId/hourly", 900_000) {
        (request {forecastRequest(it,entityId,"hourly")}?.get("result") as? JsonObject)?.let {parseForecast(it,entityId,24)}.orEmpty()
    }
    /** Never ask an integration that is not there; unknown after 10 s counts as absent. */
    private suspend fun hasIntegration(domain:String)=
        withTimeoutOrNull(10_000) {components.filterNotNull().first()}?.contains(domain)==true
    override suspend fun energy(entity:HaEntity):HaEntity {
        if(app.stova.ha.cards.pricePoints(entity).isNotEmpty()) return entity
        if(!hasIntegration("tibber")) return entity
        val result=priceMutex.withLock {
            // A missing answer (no Tibber) is remembered too, so cards and resumes never re-ask within 15 min.
            if(System.currentTimeMillis()-pricesAt>900_000) {
                priceResponse=request {tibberPricesRequest(it,LocalDate.now(timeZone))}?.get("result") as? JsonObject
                pricesAt=System.currentTimeMillis()
            }
            priceResponse
        } ?: return entity
        return entity.copy(attributes=JsonObject(entity.attributes+parseTibberPrices(result,entity,LocalDate.now(timeZone),timeZone)))
    }
}
