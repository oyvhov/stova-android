@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media

import app.stova.core.security.InMemorySecretStore
import app.stova.core.storage.JsonFileStore
import app.stova.home.HomeRowUi
import app.stova.media.model.*
import app.stova.media.net.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

/** Spole says where the film stopped; Home shows it at once, before the server has it. */
class SpolePlaybackTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun accounts() = MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),
        MediaAccountsFile.serializer(), MediaAccountsFile(), UnconfinedTestDispatcher()), InMemorySecretStore())
    private val server = ServerInfo(ServerKind.JELLYFIN, "s", "Midttunet", "v", "http://demo.invalid")
    private fun film(a: MediaAccount, id: String, progress: Float? = null) = MediaItem(id, a.id, ItemType.MOVIE, id, progress = progress)

    @Test fun onlyAWellFormedReportIsRead() {
        assertEquals(SpolePlayback(ServerKind.JELLYFIN, "u", "m1", 30_000, 60_000, false),
            SpolePlayback.parse("jellyfin", "u", "m1", 30_000, 60_000, false))
        assertEquals(ServerKind.EMBY, SpolePlayback.parse("emby", "u", "m1", 1, 2, false)?.kind)
        assertNull(SpolePlayback.parse("plex", "u", "m1", 1, 2, false))
        assertNull(SpolePlayback.parse("jellyfin", "u/../x", "m1", 1, 2, false))
        assertNull(SpolePlayback.parse("jellyfin", "u", null, 1, 2, false))
        assertNull(SpolePlayback.parse("jellyfin", "u", "m1", -1, 2, false))
        assertEquals(0.5f, SpolePlayback.parse("jellyfin", "u", "m1", 30_000, 60_000, false)!!.progress)
        assertTrue("past nine tenths counts as seen, as on the server", SpolePlayback.parse("jellyfin", "u", "m1", 55_000, 60_000, false)!!.finished)
        assertTrue(SpolePlayback.parse("jellyfin", "u", "m1", 0, 0, true)!!.finished)
    }

    @Test fun homeShowsWhereTheFilmStoppedAtOnceAndKeepsItUntilTheServerHasIt() = runTest {
        val accounts = accounts(); val a = accounts.add("p", server, AuthResult("t", "u", "Øyvind"))
        val other = accounts.add("p", server.copy(id = "s2", baseUrl = "http://other.invalid"), AuthResult("t", "someone-else", "Kari"))
        var now = 1_000L
        val stale = mapOf(a.id to listOf(film(a, "a", 0.2f), film(a, "b", 0.5f)), other.id to listOf(film(other, "b", 0.1f)))
        val latest = mapOf(a.id to listOf(film(a, "c")), other.id to emptyList())
        var asked = 0
        val repo = MediaRepository(accounts, { account -> object : MediaServerClient {
            override suspend fun resume(): List<MediaItem> { asked++; return stale.getValue(account.id) }
            override suspend fun nextUp() = emptyList<MediaItem>()
            override suspend fun latest(endpoint: Endpoint) = if (endpoint == Endpoint.LATEST_MOVIES) latest.getValue(account.id) else emptyList()
        } }, tmp.root.resolve("snapshots"), backgroundScope, UnconfinedTestDispatcher(testScheduler), clock = { now })
        repo.rows("p").first(); repo.refresh("p")
        suspend fun row(kind: MediaRowKind, account: MediaAccount) = runCurrentRows(repo).firstOrNull { it.kind == kind && it.account.id == account.id }

        repo.applyPlayback(SpolePlayback(ServerKind.JELLYFIN, "u", "b", 42_000, 60_000, false))
        assertEquals(listOf("b" to 0.7f, "a" to 0.2f), row(MediaRowKind.CONTINUE, a)!!.items.map { it.id to it.progress })
        assertEquals("another user's rows are left alone", listOf(0.1f), row(MediaRowKind.CONTINUE, other)!!.items.map { it.progress })

        repo.applyPlayback(SpolePlayback(ServerKind.JELLYFIN, "u", "c", 18_000, 60_000, false))
        assertEquals(listOf("c", "b", "a"), row(MediaRowKind.CONTINUE, a)!!.items.map { it.id })
        assertEquals(0.3f, row(MediaRowKind.LATEST_MOVIES, a)!!.items.single().progress)

        repo.applyPlayback(SpolePlayback(ServerKind.JELLYFIN, "u", "a", 0, 0, true))
        assertEquals("a film seen to the end leaves Continue", listOf("c", "b"), row(MediaRowKind.CONTINUE, a)!!.items.map { it.id })

        // The server has not heard yet: its old answer does not undo what Spole said.
        val before = asked
        advanceTimeBy(PLAYBACK_REFRESH_DELAY_MILLIS + 1)
        assertTrue("the server is asked again soon after", asked > before)
        assertEquals(listOf("c", "b"), row(MediaRowKind.CONTINUE, a)!!.items.map { it.id })
        assertEquals(0.7f, row(MediaRowKind.CONTINUE, a)!!.items.first { it.id == "b" }.progress)

        // Later the server is the truth again.
        now += PLAYBACK_HOLD_MILLIS + 1
        repo.refresh("p", minAgeMillis = 0)
        assertEquals(listOf("a" to 0.2f, "b" to 0.5f), row(MediaRowKind.CONTINUE, a)!!.items.map { it.id to it.progress })
    }

    private suspend fun runCurrentRows(repo: MediaRepository): List<HomeRowUi.Media> = repo.rows("p").first()
}
