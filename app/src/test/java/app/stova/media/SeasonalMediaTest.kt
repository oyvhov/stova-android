@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media

import app.stova.core.security.InMemorySecretStore
import app.stova.core.storage.JsonFileStore
import app.stova.home.*
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.profiles.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class SeasonalMediaTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun seasonalRowsSurviveColdStartOfflineAndCanBeHiddenAndSorted() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val accounts = MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),MediaAccountsFile.serializer(),MediaAccountsFile(),io),InMemorySecretStore())
        val acc = accounts.add("p",ServerInfo(ServerKind.JELLYFIN,"s","S","v","http://jf"),AuthResult("t","u","U"))
        val item = MediaItem("h",acc.id,ItemType.MOVIE,"Night",art=ItemArt(backdrop="b"))
        var fail = false
        var now = 1_000L
        val client = object : MediaServerClient {
            override suspend fun resume() = emptyList<MediaItem>()
            override suspend fun nextUp() = emptyList<MediaItem>()
            override suspend fun latest(endpoint: Endpoint) = emptyList<MediaItem>()
            override suspend fun seasonal(season: Season): List<MediaItem> { if(fail) throw MediaError.Server(500); return listOf(item) }
        }
        val repo = MediaRepository(accounts,{client},tmp.root.resolve("snapshots"),backgroundScope,io,clock={ now })
        repo.rows("p",Season.HALLOWEEN).first(); repo.refresh("p")
        val row = repo.rows("p",Season.HALLOWEEN).first().single()
        assertEquals(MediaRowKind.HALLOWEEN,row.kind)
        val cold = MediaRepository(accounts,{client},tmp.root.resolve("snapshots"),backgroundScope,io)
        assertEquals(listOf(item),cold.rows("p",Season.HALLOWEEN).first().single().items)
        // Season rows refresh hourly; the failing fetch is the one an hour later.
        fail = true; now += 61 * 60 * 1000L; repo.refresh("p")
        assertTrue(repo.rows("p",Season.HALLOWEEN).first().single().offline)
        assertTrue(repo.rows("p",Season.NONE).first().isEmpty())
        assertTrue(repo.rows("p",Season.CHRISTMAS).first().isEmpty())
        assertEquals(listOf(row.key,HomeRowKey.APPS),arrangeRows(listOf(HomeRowKey.APPS,row.key),HomePrefs(rowOrder=listOf(row.key))))
        assertTrue(arrangeRows(listOf(row.key),HomePrefs(hiddenRows=setOf(row.key))).isEmpty())
    }
    @Test fun spotlightLeadsWithSeasonThenKeepsTheRestAndDeduplicatesPerAccount() {
        val acc = MediaAccount("a","p",ServerKind.JELLYFIN,"s","S","http://jf","u","U")
        fun entry(id: String) = SpotlightEntry(MediaItem(id,"a",ItemType.MOVIE,id,art=ItemArt(backdrop="b")),acc,false)
        val rest = listOf(entry("ordinary"),entry("season"),entry("new").copy(newSeason=true))
        val row = HomeRowUi.Media(HomeRowKey.seasonal(acc.id,Season.HALLOWEEN),acc,MediaRowKind.HALLOWEEN,listOf(entry("season").item,entry("no-art").item.copy(art=ItemArt())),false)
        assertEquals(listOf("season","ordinary","new"),seasonalSpotlight(rest,listOf(row),Season.HALLOWEEN).map { it.item.id })
        assertEquals(rest,seasonalSpotlight(rest,listOf(row),Season.NONE))
    }
}
