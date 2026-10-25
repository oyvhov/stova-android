package app.stova.ha

import app.stova.core.security.InMemorySecretStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.ha.cards.*
import app.stova.profiles.*
import java.io.File
import org.junit.rules.TemporaryFolder

class HaSocketTest {
    @get:Rule val server = MockWebServer()
    @get:Rule val dir = TemporaryFolder()
    private val received = CopyOnWriteArrayList<JsonObject>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var peer: WebSocket? = null
    private fun enqueue(invalid: Boolean = false, emptySnapshot: Boolean = false) {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) { peer = ws; ws.send("""{"type":"auth_required"}""") }
            override fun onMessage(ws: WebSocket, text: String) {
                val j = splitIncoming(text).single(); received += j
                val id = j["id"]?.jsonPrimitive?.int
                when (j["type"]!!.jsonPrimitive.content) {
                    "auth" -> ws.send(if (invalid) """{"type":"auth_invalid"}""" else """{"type":"auth_ok"}""")
                    "subscribe_entities" -> {
                        val event = if (emptySnapshot) "{}" else """{"a":{"light.stove":{"s":"on","a":{},"lc":1.0}}}"""
                        ws.send("""[{"id":$id,"type":"result","success":true},{"id":$id,"type":"event","event":$event}]""")
                    }
                    else -> ws.send("""{"id":$id,"type":"result","success":true,"result":[]} """)
                }
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) { ws.close(code, reason) }
        }))
    }
    private suspend fun socket(): HaSocket {
        val address = HaAddress(server.url("/").toString().trimEnd('/'), server.url("/api/websocket").toString().replaceFirst("http", "ws"))
        val auth = HaAuth(OkHttpClient(), InMemorySecretStore(), { address }); auth.useLongLivedToken("ll")
        return HaSocket(OkHttpClient(), auth, { address }, scope, listOf(10, 20))
    }
    private fun subscriptions() = received.filter { it["type"]?.jsonPrimitive?.content == "subscribe_entities" }
    private suspend fun connected(s: HaSocket) = withTimeout(5_000) { s.entities.first { it["light.stove"]?.state == "on" } }
    @Test fun emptyInitialSnapshotReplacesCachedEntities() = runBlocking {
        try {
            enqueue(emptySnapshot = true)
            val profiles = ProfileRepository(JsonFileStore(File(dir.root, "profiles.json"), ProfilesFile.serializer(), ProfilesFile()))
            val profile = profiles.create("Demo", AvatarColor.LIME, false)
            profiles.update(profile.id) { it.copy(dashboard = listOf(CardConfig("light", CardType.LIGHT, "light.stove"))) }
            val cache = JsonFileStore(File(dir.root, "ha-cache.json"), HaSnapshot.serializer(), HaSnapshot())
            cache.update { HaSnapshot(profile.id, mapOf("light.stove" to HaEntity("light.stove", "on", JsonObject(emptyMap()), 0, 0))) }
            val session = ProfileSession(profiles, scope)
            val socket = socket()
            val repo = HaRepository(socket, session, profiles, scope, cache)
            session.enter(profile.id)
            withTimeout(5_000) { repo.entities.first { it["light.stove"]?.state == "on" } }
            socket.start()
            withTimeout(5_000) { repo.entities.first { it.isEmpty() } }
            withTimeout(5_000) { cache.data.first { it.entities.isEmpty() } }
            Unit
        } finally { scope.cancel() }
    }
    @Test fun connectsAuthenticatesAndReceivesEntities() = runBlocking {
        try {
            enqueue(); val s = socket(); s.watch(setOf("light.stove")); s.start(); connected(s)
            assertEquals(HaStatus.CONNECTED, s.status.value)
            assertTrue(received.any { it["features"]?.jsonObject?.get("coalesce_messages")?.jsonPrimitive?.int == 1 })
            assertEquals(1, subscriptions().size)
        } finally { scope.cancel() }
    }
    @Test fun reconnectsAndResubscribesAfterDrop() = runBlocking {
        try {
            enqueue(); enqueue(emptySnapshot = true); val s = socket(); s.watch(setOf("light.stove")); s.start(); connected(s)
            peer!!.close(1001, "restart")
            withTimeout(5_000) { while (subscriptions().size < 2) delay(10) }
            withTimeout(5_000) { s.entities.first { it.isEmpty() } }
            assertEquals(subscriptions()[0]["entity_ids"], subscriptions()[1]["entity_ids"])
            s.watch(setOf("light.stove")); delay(100); assertEquals(2, subscriptions().size)
        } finally { scope.cancel() }
    }
    @Test fun authInvalidTwiceSignsOut() = runBlocking {
        try {
            enqueue(true); enqueue(true); val s = socket(); s.start()
            withTimeout(5_000) { s.status.first { it == HaStatus.SIGNED_OUT } }
            delay(100); assertEquals(2, server.requestCount)
        } finally { scope.cancel() }
    }
    @Test fun callServiceSendsTargetAndReturnsSuccess() = runBlocking {
        try {
            enqueue(); val s = socket(); s.watch(setOf("light.stove")); s.start(); connected(s)
            assertTrue(s.callService("light", "toggle", "light.stove"))
            assertEquals("light.stove", received.last()["target"]!!.jsonObject["entity_id"]!!.jsonPrimitive.content)
        } finally { scope.cancel() }
    }
    @Test fun stopGraceCanBeCancelledByStart() = runBlocking {
        try {
            enqueue(); val s = socket(); s.watch(setOf("light.stove")); s.start(); connected(s)
            s.stop(80); s.start(); delay(150); assertEquals(HaStatus.CONNECTED, s.status.value)
            s.stop(0); withTimeout(5_000) { s.status.first { it == HaStatus.DISCONNECTED } }
            Unit
        } finally { scope.cancel() }
    }
}
