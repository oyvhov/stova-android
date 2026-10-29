@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media

import app.stova.core.security.InMemorySecretStore
import app.stova.core.storage.JsonFileStore
import app.stova.core.theme.AvatarColor
import app.stova.home.HomeRowUi
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.profiles.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class KidsMediaRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val profile = Profile("p", "Barn", AvatarColor.LIME, kid = true,
        appearance = SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs()))
    private fun accounts() = MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),
        MediaAccountsFile.serializer(), MediaAccountsFile(), UnconfinedTestDispatcher()), InMemorySecretStore())
    private suspend fun add(accounts: MediaAccountRepository, kind: ServerKind = ServerKind.EMBY) =
        accounts.add("p", ServerInfo(kind, "s", "Server", "v", "http://demo.invalid"), AuthResult("token", "u", "Barn"))
    private fun item(account: MediaAccount, id: String) = MediaItem(id, account.id, ItemType.MOVIE, id)
    private fun client(account: MediaAccount, fail: () -> String? = { null }) = object : MediaServerClient {
        override suspend fun resume() = listOf(item(account, "continue"))
        override suspend fun nextUp() = emptyList<MediaItem>()
        override suspend fun latest(endpoint: Endpoint): List<MediaItem> {
            if (endpoint.name == fail()) throw MediaError.Server(500)
            return if (endpoint.name in listOf("LATEST_MOVIES", "LATEST_SERIES", "FAVORITES", "MOST_PLAYED"))
                listOf(item(account, endpoint.name)) else emptyList()
        }
        override suspend fun seasonal(season: Season) = listOf(item(account, "season"))
    }
    private suspend fun rows(repo: MediaRepository, selected: Profile = profile) =
        MediaRowSource(repo).rows(selected).first().filterIsInstance<HomeRowUi.Media>()

    @Test fun kidsGetOrderedRowsThatSurviveAColdStartAndNeverLeakToAdults() = runTest {
        val accounts = accounts(); val account = add(accounts)
        val io = UnconfinedTestDispatcher(testScheduler)
        val repo = MediaRepository(accounts, { client(account) }, tmp.root.resolve("snapshots"), backgroundScope, io)
        rows(repo); repo.refresh("p")
        val expected = listOf("CONTINUE", "FAVORITES", "HALLOWEEN", "MOST_PLAYED", "LATEST_MOVIES", "LATEST_SERIES")
        assertEquals(expected, rows(repo).map { it.kind.name })
        assertEquals(listOf("continue", "favorites", "halloween", "most_played", "latest_movies", "latest_series"),
            rows(repo).map { it.key.value.substringBefore(':') })
        val cold = MediaRepository(accounts, { awaitCancellation() }, tmp.root.resolve("snapshots"), backgroundScope, io)
        assertEquals(expected, rows(cold).map { it.kind.name })
        assertTrue(rows(cold).all { it.offline })
        assertFalse(rows(cold, profile.copy(kid = false)).any { it.kind.name in listOf("FAVORITES", "MOST_PLAYED") })
    }

    @Test fun optionalFailuresKeepCoreRowsOnlineAndRetainOnlyTheFailedRowsCache() = runTest {
        val accounts = accounts(); val account = add(accounts); var failure: String? = null
        val repo = MediaRepository(accounts, { client(account) { failure } }, tmp.root.resolve("snapshots"),
            backgroundScope, UnconfinedTestDispatcher(testScheduler))
        rows(repo); repo.refresh("p")
        for (kind in listOf("FAVORITES", "MOST_PLAYED")) {
            failure = kind; repo.refresh("p", minAgeMillis = 0)
            val current = rows(repo)
            assertEquals(6, current.size)
            assertTrue(current.single { it.kind.name == kind }.offline)
            assertTrue(current.filterNot { it.kind.name == kind }.all { !it.offline })
            assertFalse(accounts.forProfile("p").first().single().needsSignIn)
        }
    }

    @Test fun timeoutOrUnauthorizedInAnExtraNeverCostsCoreRowsOrTheOtherExtra() = runTest {
        val accounts = accounts(); val account = add(accounts)
        var timeout = true
        val server = object : MediaServerClient by client(account) {
            override suspend fun latest(endpoint: Endpoint): List<MediaItem> {
                if (endpoint == Endpoint.FAVORITES) {
                    if (timeout) awaitCancellation() else throw MediaError.Unauthorized()
                }
                return client(account).latest(endpoint)
            }
        }
        val repo = MediaRepository(accounts, { server }, tmp.root.resolve("snapshots"), backgroundScope,
            UnconfinedTestDispatcher(testScheduler))
        rows(repo); repo.refresh("p")
        assertTrue(rows(repo).all { !it.offline })
        assertTrue(rows(repo).any { it.kind == MediaRowKind.MOST_PLAYED })
        assertFalse(rows(repo).any { it.kind == MediaRowKind.FAVORITES })
        timeout = false; repo.refresh("p", minAgeMillis = 0)
        assertTrue(rows(repo).all { !it.offline })
        assertFalse(accounts.forProfile("p").first().single().needsSignIn)
    }

    @Test fun kidModeChangesInvalidateYoungRowsAndAdultsStopRequestingExtras() = runTest {
        val accounts = accounts(); val account = add(accounts)
        val requested = mutableListOf<Endpoint>()
        val server = object : MediaServerClient by client(account) {
            override suspend fun latest(endpoint: Endpoint): List<MediaItem> {
                requested += endpoint
                return client(account).latest(endpoint)
            }
        }
        val repo = MediaRepository(accounts, { server }, tmp.root.resolve("snapshots"), backgroundScope,
            UnconfinedTestDispatcher(testScheduler), clock = { 1_000L })
        rows(repo, profile.copy(kid = false)); repo.refresh("p")
        assertFalse(Endpoint.FAVORITES in requested)
        rows(repo); repo.refresh("p")
        assertEquals(1, requested.count { it == Endpoint.FAVORITES })
        assertEquals(1, requested.count { it == Endpoint.MOST_PLAYED })
        val before = requested.size
        rows(repo, profile.copy(kid = false)); repo.refresh("p")
        assertEquals(listOf(Endpoint.LATEST_MOVIES, Endpoint.LATEST_SERIES), requested.drop(before))
        assertFalse(rows(repo, profile.copy(kid = false)).any { it.kind.kidsOnly })
    }

    @Test fun kidsWithoutAnAccountOrWithAnExpiredLoginMakeNoClientCalls() = runTest {
        val accounts = accounts()
        val repo = MediaRepository(accounts, { error("No usable account must mean no client") }, tmp.root.resolve("snapshots"),
            backgroundScope, UnconfinedTestDispatcher(testScheduler))
        rows(repo); repo.refresh("p")
        val account = add(accounts); accounts.markNeedsSignIn(account.id)
        repo.refresh("p")
        assertTrue(rows(repo).isEmpty())
        assertFalse(tmp.root.resolve("snapshots/p.json").exists())
    }

    @Test fun oldSnapshotsRemainReadableAndNewFlagsRoundTrip() {
        val json = app.stova.core.storage.StovaJson
        val old = json.decodeFromString(MediaSnapshot.serializer(), """{"rows":[{"key":"latest_movies:a","accountId":"a","kind":"LATEST_MOVIES","items":[{"id":"m","accountId":"a","type":"MOVIE","title":"Film"}]}]}""")
        assertFalse(old.rows.single().items.single().isNew)
        assertEquals(listOf("CONTINUE", "LATEST_MOVIES", "LATEST_SERIES", "HALLOWEEN", "CHRISTMAS"),
            MediaRowKind.entries.take(5).map { it.name })
        val fresh = old.copy(rows = listOf(old.rows.single().copy(kind = MediaRowKind.FAVORITES,
            key = app.stova.home.HomeRowKey.favorites("a"), items = listOf(old.rows.single().items.single().copy(isNew = true)))))
        assertEquals(fresh, json.decodeFromString(MediaSnapshot.serializer(), json.encodeToString(MediaSnapshot.serializer(), fresh)))
    }

    @Test fun adultMakesExactlyTheExistingSevenHttpRequestsAndReturningHomeMakesNone() = runBlocking {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse().setBody("{\"Items\":[]}")
            }
            server.start()
            val accounts = accounts()
            accounts.add("p", ServerInfo(ServerKind.EMBY, "s", "Server", "v", server.url("/").toString().trimEnd('/')),
                AuthResult("token", "u", "Adult"))
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val repo = MediaRepository(accounts, { JellyfinEmbyClient(it, "token", OkHttpClient(), ClientIdentity("d", "i", "v")) },
                    tmp.root.resolve("snapshots"), scope, Dispatchers.IO, clock = { 1_000L })
                val adult = profile.copy(kid = false, appearance = AppearancePrefs(seasonChoice = SeasonChoice.NONE))
                rows(repo, adult); repo.refresh("p")
                // Resume, NextUp, latest films/series, spotlight, new seasons and the existing user picture.
                repeat(7) {
                    val query = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.requestUrl!!
                    assertNull(query.queryParameter("Filters"))
                    assertNotEquals("Movie,Episode", query.queryParameter("IncludeItemTypes"))
                }
                assertEquals(7, server.requestCount)
                repeat(3) { rows(repo, adult); repo.refresh("p") }
                assertEquals(7, server.requestCount)
            } finally { scope.cancel() }
        }
    }
}
