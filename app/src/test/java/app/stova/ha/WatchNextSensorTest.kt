package app.stova.ha

import app.stova.watchnext.WatchNextEntry
import app.stova.watchnext.WatchNextResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WatchNextSensorTest {
    @get:Rule val server = MockWebServer()
    @Volatile private var code = 201

    @Before fun answer() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(code)
        }
    }

    private val nrk = WatchNextEntry(1, "no.nrk.tv", "Lykkeland", "Episode 3", "https://gfx.nrk.no/p.jpg",
        "intent:#Intent;package=no.nrk.tv;end", 0.25f, 1_700_000_000_000, positionMs = 900_000, durationMs = 3_600_000)
    private val olderNrk = WatchNextEntry(2, "no.nrk.tv", "Fleksnes", null, null, "intent:#Intent;package=no.nrk.tv;end", null, 1_600_000_000_000)
    private val netflix = WatchNextEntry(3, "com.netflix.ninja", "Dark", null, "content://com.netflix/poster/1",
        "intent:#Intent;package=com.netflix.ninja;end", null, 1_650_000_000_000)

    private fun available(vararg entries: WatchNextEntry): WatchNextResult = WatchNextResult.Available(entries.toList())
    private fun address() = HaAddress(server.url("/").toString().trimEnd('/'), "")
    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    private fun TestScope.start(address: Flow<HaAddress?>, watchNext: Flow<WatchNextResult>, reconnected: Flow<Unit> = emptyFlow(),
        token: suspend () -> String = { "secret" }) = backgroundScope.launch {
        WatchNextSensor(OkHttpClient(), address, token, watchNext, reconnected, io = StandardTestDispatcher(testScheduler)).run()
    }
    private fun TestScope.settle(millis: Long = 2_001) { advanceTimeBy(millis); runCurrent() }

    @Test fun theBodyFollowsTheDashboardContract() {
        val expected = json("""{"state":"Lykkeland","attributes":{
            "friendly_name":"Shield · Fortsett å sjå","icon":"mdi:television-play",
            "latest":{"package":"no.nrk.tv","title":"Lykkeland","episode":"Episode 3","poster":"https://gfx.nrk.no/p.jpg",
                "progress":0.25,"position_ms":900000,"duration_ms":3600000,"engaged_at":"2023-11-14T22:13:20Z"},
            "by_package":{
                "no.nrk.tv":{"package":"no.nrk.tv","title":"Lykkeland","episode":"Episode 3","poster":"https://gfx.nrk.no/p.jpg",
                    "progress":0.25,"position_ms":900000,"duration_ms":3600000,"engaged_at":"2023-11-14T22:13:20Z"},
                "com.netflix.ninja":{"package":"com.netflix.ninja","title":"Dark","episode":null,"poster":null,
                    "progress":null,"position_ms":null,"duration_ms":null,"engaged_at":"2022-04-15T05:20:00Z"}}}}""")
        assertEquals(expected, watchNextSensorBody(listOf(netflix, olderNrk, nrk)))
    }

    @Test fun anEmptyListIsNone() = assertEquals(json("""{"state":"none","attributes":{
        "friendly_name":"Shield · Fortsett å sjå","icon":"mdi:television-play","latest":null,"by_package":{}}}"""),
        watchNextSensorBody(emptyList()))

    @Test fun theStateIsCutTo255AndAtMostTwelveAppsAreKept() {
        val entries = (0 until 15).map { WatchNextEntry(it.toLong(), "app.p$it", "T$it", null, "http://x/$it.jpg", "intent:#Intent;end", null, 1_000L + it) }
        val long = entries.last().copy(title = "x".repeat(300))
        val body = watchNextSensorBody(entries.dropLast(1) + long)
        assertEquals("x".repeat(255), body["state"]!!.jsonPrimitive.content)
        val attributes = body["attributes"]!!.jsonObject
        assertEquals("x".repeat(300), attributes["latest"]!!.jsonObject["title"]!!.jsonPrimitive.content)
        assertEquals((14 downTo 3).map { "app.p$it" }, attributes["by_package"]!!.jsonObject.keys.toList())
        assertEquals("http://x/14.jpg", attributes["latest"]!!.jsonObject["poster"]!!.jsonPrimitive.content)
    }

    @Test fun theListIsPostedOnceAfterStartAndThenOnlyWhenItChanges() = runTest {
        val list = MutableStateFlow(available(nrk))
        start(flowOf(address()), list)
        settle(1_999)
        assertEquals(0, server.requestCount)
        settle(2)
        val first = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", first.method)
        assertEquals("/api/states/sensor.stova_watch_next", first.path)
        assertEquals("Bearer secret", first.getHeader("Authorization"))
        assertEquals(watchNextSensorBody(listOf(nrk)), json(first.body.readUtf8()))

        // Only what the dashboard does not read changed: nothing to send.
        list.value = available(nrk.copy(id = 9, intentUri = "intent:#Intent;package=no.nrk.tv;S.x=y;end"))
        settle(10_000)
        assertEquals(1, server.requestCount)

        // A burst of changes is one post, with the last list.
        list.value = available(nrk.copy(positionMs = 1_000_000))
        settle(500)
        list.value = available(netflix, nrk)
        settle()
        assertEquals(2, server.requestCount)
        assertEquals(watchNextSensorBody(listOf(netflix, nrk)), json(server.takeRequest(1, TimeUnit.SECONDS)!!.body.readUtf8()))
    }

    @Test fun withoutHomeAssistantNothingIsReadOrAsked() = runTest {
        val address = MutableStateFlow<HaAddress?>(null)
        var reads = 0
        var tokens = 0
        start(address, flow { reads++; emit(available(nrk)) }, token = { tokens++; "secret" })
        settle(10_000)
        assertEquals(0, reads); assertEquals(0, tokens); assertEquals(0, server.requestCount)

        address.value = address()
        settle()
        assertEquals(1, server.requestCount)

        // Switched off, then on again: one post, though the list is the same.
        address.value = null
        settle(10_000)
        assertEquals(1, server.requestCount)
        address.value = address()
        settle()
        assertEquals(2, server.requestCount)
    }

    @Test fun anUnreadableListPostsNothing() = runTest {
        val list = MutableStateFlow<WatchNextResult>(WatchNextResult.Unavailable(WatchNextResult.Reason.NO_PERMISSION))
        start(flowOf(address()), list)
        settle(10_000)
        assertEquals(0, server.requestCount)
        list.value = available(nrk)
        settle()
        assertEquals(1, server.requestCount)
    }

    @Test fun aFailureIsQuietAndRetriedOnTheNextChange() = runTest {
        code = 500
        val list = MutableStateFlow(available(nrk))
        start(flowOf(address()), list)
        settle()
        assertEquals(1, server.requestCount)
        code = 201
        settle(60_000)
        assertEquals(1, server.requestCount)
        list.value = available(nrk, netflix)
        settle()
        assertEquals(2, server.requestCount)
        // Back to what failed before: Home Assistant never got it, so it goes again.
        list.value = available(nrk)
        settle()
        assertEquals(3, server.requestCount)
        // A change that failed and was then undone: Home Assistant already has this list.
        code = 500
        list.value = available(netflix)
        settle()
        assertEquals(4, server.requestCount)
        code = 201
        list.value = available(nrk)
        settle()
        assertEquals(4, server.requestCount)
    }

    @Test fun anUnreachableOrSignedOutHomeAssistantIsQuiet() = runTest {
        var signedIn = false
        val list = MutableStateFlow(available(nrk))
        start(flowOf(address()), list, token = { if (!signedIn) throw HaSignedOut(); "secret" })
        settle()
        assertEquals(0, server.requestCount)
        signedIn = true
        list.value = available(netflix)
        settle()
        assertEquals(1, server.requestCount)
    }

    @Test fun aRefusedConnectionIsQuiet() = runTest {
        val closed = MockWebServer().apply { start() }
        val unreachable = HaAddress(closed.url("/").toString().trimEnd('/'), "")
        closed.shutdown()
        start(flowOf(unreachable), MutableStateFlow(available(nrk)))
        settle()
        assertNull(server.takeRequest(0, TimeUnit.SECONDS))
    }

    @Test fun aNewConnectionPostsOnceEvenWhenNothingChanged() = runTest {
        // States posted through the REST API are gone after Home Assistant restarts.
        val reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        start(flowOf(address()), MutableStateFlow(available(nrk)), reconnected)
        settle()
        assertEquals(1, server.requestCount)
        reconnected.tryEmit(Unit)
        settle(0)
        assertEquals(2, server.requestCount)
        settle(10_000)
        assertEquals(2, server.requestCount)
    }
}
