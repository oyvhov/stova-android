@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media
import app.stova.core.storage.*
import app.stova.core.security.*
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class MediaRepositoryTest {
 @get:Rule val tmp=TemporaryFolder()
 private fun accounts()=MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),MediaAccountsFile.serializer(),MediaAccountsFile(),UnconfinedTestDispatcher()),InMemorySecretStore())
 private suspend fun add(r:MediaAccountRepository,kind:ServerKind)=r.add("p",ServerInfo(kind,kind.name,kind.name,"v","http://demo.invalid"),AuthResult("secret","u","Demo"))
 private fun item(a:MediaAccount)=MediaItem("m",a.id,ItemType.MOVIE,"Demo")
 private fun client(a:MediaAccount,fail:MediaError?=null)=object:MediaServerClient {
  override suspend fun resume():List<MediaItem> { fail?.let { throw it };return listOf(item(a)) }
  override suspend fun nextUp()=emptyList<MediaItem>()
  override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
 }
 private suspend fun cached(a:MediaAccount) { JsonFileStore(tmp.root.resolve("snapshots/p.json"),MediaSnapshot.serializer(),MediaSnapshot(),UnconfinedTestDispatcher()).update { MediaSnapshot(listOf(SnapshotRow(HomeRowKey.continueWatching(a.id),a.id,MediaRowKind.CONTINUE,listOf(item(a))))) } }
 @Test fun snapshotEmittedBeforeNetwork()=runTest {
  val a=accounts();val j=add(a,ServerKind.JELLYFIN);cached(j)
  val r=MediaRepository(a,{ awaitCancellation() },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  assertEquals(listOf(item(j)),r.rows("p").first().single().items)
 }
 @Test fun sourcesStaySeparate()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);add(a,ServerKind.EMBY)
  val r=MediaRepository(a,{ client(it) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  r.refresh("p");val rows=r.rows("p").first();assertEquals(2,rows.size);assertEquals(2,rows.map { it.account.id }.distinct().size)
 }
 @Test fun unreachableKeepsCachedRowsMarkedOffline()=runTest {
  val a=accounts();val j=add(a,ServerKind.JELLYFIN);cached(j)
  val r=MediaRepository(a,{ client(it,MediaError.Unreachable(Exception())) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  r.refresh("p");assertTrue(r.rows("p").first().single().offline);assertEquals(listOf(item(j)),r.rows("p").first().single().items)
 }
 @Test fun failedRowsRetryAfterThirtySecondsAndRecoveryRestoresTheNormalCache()=runTest {
  assertFailedRefreshRetries(cachedRows=true)
 }
 @Test fun aFailureWithoutCachedRowsAlsoRetriesSoon()=runTest {
  assertFailedRefreshRetries(cachedRows=false)
 }
 @Test fun aCoreTimeoutAlsoRetriesSoon()=runTest {
  assertFailedRefreshRetries(cachedRows=true,timeout=true)
 }
 @Test fun aHealthyAccountDoesNotHideAnotherAccountsFailure()=runTest {
  assertFailedRefreshRetries(cachedRows=true,healthyAccount=true)
 }
 private suspend fun TestScope.assertFailedRefreshRetries(cachedRows:Boolean,timeout:Boolean=false,healthyAccount:Boolean=false) {
  val a=accounts();val account=add(a,ServerKind.JELLYFIN)
  if(healthyAccount) add(a,ServerKind.EMBY)
  if(cachedRows) cached(account)
  var now=1_000L;var failing=true;var calls=0
  val server=object:MediaServerClient {
   override suspend fun resume():List<MediaItem> {
    calls++
    if(failing) {
     if(timeout) awaitCancellation() else throw MediaError.Unreachable(Exception())
    }
    return listOf(item(account))
   }
   override suspend fun nextUp()=emptyList<MediaItem>()
   override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
  }
  val r=MediaRepository(a,{ if(it.id==account.id) server else client(it) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  r.refresh("p")
  val offline=r.rows("p").first().filter { it.account.id==account.id }
  assertEquals(if(cachedRows) 1 else 0,offline.size)
  assertTrue(offline.all { it.offline })
  failing=false
  now+=5_000L;r.refresh("p")
  assertEquals("a quick return must not hammer the server",1,calls)
  now+=26_000L;r.refresh("p")
  assertEquals("a failed attempt must not block recovery for five minutes",2,calls)
  assertTrue(r.rows("p").first().all { !it.offline })
  now+=31_000L;r.refresh("p")
  assertEquals("recovered rows keep the normal five-minute cache",2,calls)
  now+=REFRESH_MIN_AGE_MILLIS-31_000L;r.refresh("p")
  assertEquals(3,calls)
 }
 @Test fun unauthorizedMarksAccountAndDropsRows()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);val e=add(a,ServerKind.EMBY);cached(e)
  val r=MediaRepository(a,{ client(it,if(it.kind==ServerKind.EMBY) MediaError.Unauthorized() else null) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  r.refresh("p");assertTrue(a.forProfile("p").first().first { it.id==e.id }.needsSignIn)
  assertEquals(listOf(ServerKind.JELLYFIN),r.rows("p").first().map { it.account.kind })
 }
 @Test fun emptyRowsAreNotEmitted()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN)
  val r=MediaRepository(a,{ client(it) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler));r.refresh("p")
  assertTrue(r.rows("p").first().all { it.kind==MediaRowKind.CONTINUE })
 }
 @Test fun playbackReturnUpdatesYoungRowsOnceAndKeepsSpotlightCached()=runTest {
  val a=accounts();val account=add(a,ServerKind.JELLYFIN)
  var progress=0.1f;var resumeCalls=0;var spotlightCalls=0
  val server=object:MediaServerClient {
   override suspend fun resume():List<MediaItem> { resumeCalls++;return listOf(item(account).copy(progress=progress)) }
   override suspend fun nextUp()=emptyList<MediaItem>()
   override suspend fun latest(endpoint:Endpoint):List<MediaItem> { if(endpoint==Endpoint.SPOTLIGHT) spotlightCalls++;return emptyList() }
  }
  val r=MediaRepository(a,{ server },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ 1_000L })
  r.refresh("p")
  progress=0.2f
  r.refresh("p")
  assertEquals(1,resumeCalls)
  r.playbackOpened("another-profile")
  r.refresh("p")
  assertEquals("another profile cannot invalidate these rows",1,resumeCalls)
  r.playbackOpened("p")
  r.refresh("p")
  assertEquals(0.2f,r.rows("p").first().single().items.single().progress)
  assertEquals(2,resumeCalls)
  assertEquals(1,spotlightCalls)
  r.refresh("p")
  assertEquals("ordinary returns still use the cache",2,resumeCalls)
 }
 private fun spot(a:MediaAccount)=MediaItem("f1",a.id,ItemType.MOVIE,"Fjordlys",art=ItemArt(backdrop="b",logo="l"))
 private fun spotClient(a:MediaAccount,calls:MutableList<Endpoint>,failSpotlight:Boolean=false)=object:MediaServerClient {
  override suspend fun resume()=listOf(item(a))
  override suspend fun nextUp()=emptyList<MediaItem>()
  override suspend fun latest(endpoint:Endpoint):List<MediaItem> {
   calls+=endpoint
   if(endpoint==Endpoint.SPOTLIGHT && failSpotlight) throw MediaError.Server(500)
   return if(endpoint==Endpoint.SPOTLIGHT) listOf(spot(a)) else emptyList()
  }
 }
 @Test fun spotlightIsFetchedCachedAndServedFromDiskOnColdStart()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);val calls=mutableListOf<Endpoint>();val io=UnconfinedTestDispatcher(testScheduler)
  val r=MediaRepository(a,{ spotClient(it,calls) },tmp.root.resolve("snapshots"),backgroundScope,io)
  r.refresh("p");assertEquals(listOf("f1"),r.spotlight("p").first().map { it.item.id })
  val cold=MediaRepository(a,{ awaitCancellation() },tmp.root.resolve("snapshots"),backgroundScope,io)
  assertEquals(listOf("f1"),cold.spotlight("p").first().map { it.item.id })
 }
 @Test fun spotlightRefreshesAtMostEveryTwoHours()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);val calls=mutableListOf<Endpoint>();var now=1_000L
  val r=MediaRepository(a,{ spotClient(it,calls) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  r.refresh("p");r.refresh("p");assertEquals(1,calls.count { it==Endpoint.SPOTLIGHT })
  now+=2*60*60*1000L+1;r.refresh("p");assertEquals(2,calls.count { it==Endpoint.SPOTLIGHT })
 }
 @Test fun aFailingSpotlightKeepsTheRowsAndTheCachedSpotlight()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);val calls=mutableListOf<Endpoint>();var now=1_000L;var fail=false
  val r=MediaRepository(a,{ spotClient(it,calls,fail) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  r.refresh("p");fail=true;now+=3*60*60*1000L;r.refresh("p")
  val row=r.rows("p").first().single()
  assertEquals(listOf(item(a.forProfile("p").first().single())),row.items)
  // Review finding 2: a failing async spotlight child cancelled the whole account and the rows went Offline.
  assertFalse(row.offline)
  assertEquals(listOf("f1"),r.spotlight("p").first().map { it.item.id })
 }
 // Review finding 10: two whole-library Halloween queries ran on every 5-minute refresh.
 @Test fun seasonRowsAreFetchedAtMostHourly()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);var seasonalCalls=0;var now=1_000L
  val client=object:MediaServerClient {
   override suspend fun resume()=listOf(item(a.forProfile("p").first().single()))
   override suspend fun nextUp()=emptyList<MediaItem>()
   override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
   override suspend fun seasonal(season:app.stova.profiles.Season):List<MediaItem> { seasonalCalls++;return listOf(MediaItem("h",a.forProfile("p").first().single().id,ItemType.MOVIE,"Hocus")) }
  }
  val r=MediaRepository(a,{ client },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  r.rows("p",app.stova.profiles.Season.HALLOWEEN).first()
  r.refresh("p");r.refresh("p")
  assertEquals(1,seasonalCalls)
  val halloween=r.rows("p",app.stova.profiles.Season.HALLOWEEN).first().single { it.kind==MediaRowKind.HALLOWEEN }
  assertFalse(halloween.offline)
  now+=61*60*1000L;r.refresh("p")
  assertEquals(2,seasonalCalls)
 }
 @Test fun newSeasonsCarryTheirSeriesStory()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN)
  val noon=java.time.LocalDate.of(2026,10,25).atTime(12,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
  val season=MediaItem("se3",a.forProfile("p").first().single().id,ItemType.SEASON,"Sesong 3",seriesName="Kystvakta",seriesId="s1",season=3,premiered="2026-10-10",art=ItemArt(backdrop="b"))
  val client=object:MediaServerClient {
   override suspend fun resume()=emptyList<MediaItem>()
   override suspend fun nextUp()=emptyList<MediaItem>()
   override suspend fun latest(endpoint:Endpoint)=if(endpoint==Endpoint.NEW_SEASONS) listOf(season) else emptyList()
   override suspend fun items(ids:List<String>)=ids.map { MediaItem(it,season.accountId,ItemType.SERIES,"Kystvakta",overview="Livet langs kysten.",art=ItemArt(backdrop="sb")) }
  }
  val r=MediaRepository(a,{ client },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ noon })
  r.refresh("p");val entry=r.spotlight("p").first().single()
  assertTrue(entry.newSeason);assertEquals("Livet langs kysten.",entry.item.overview)
 }
 // Real Jellyfin: whole-library spotlight and Halloween queries took the account past its 10 s,
 // and every row came back "Offline" on each refresh.
 @Test fun slowSpotlightNeverMarksTheRowsOffline()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN)
  val slow=object:MediaServerClient {
   override suspend fun resume():List<MediaItem> { delay(2_000);return listOf(item(a.forProfile("p").first().single())) }
   override suspend fun nextUp():List<MediaItem> { delay(2_000);return emptyList() }
   override suspend fun latest(endpoint:Endpoint):List<MediaItem> { delay(if(endpoint==Endpoint.SPOTLIGHT||endpoint==Endpoint.NEW_SEASONS) 9_000 else 2_000);return emptyList() }
  }
  val r=MediaRepository(a,{ slow },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  r.refresh("p")
  val row=r.rows("p").first().single()
  assertFalse(row.offline);assertEquals("m",row.items.single().id)
 }
 @Test fun removedAccountDropsCachedRows()=runTest {
  val a=accounts();val j=add(a,ServerKind.JELLYFIN);cached(j)
  val r=MediaRepository(a,{client(it)},tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  assertEquals(1,r.rows("p").first().size);a.remove(j.id);assertTrue(r.rows("p").first().isEmpty())
 }
 @Test fun fixedBackgroundSurvivesColdStartAndRefreshButNotAccountRemoval()=runTest {
  val a=accounts();val account=add(a,ServerKind.JELLYFIN);val io=UnconfinedTestDispatcher(testScheduler)
  val r=MediaRepository(a,{client(it)},tmp.root.resolve("snapshots"),backgroundScope,io)
  val entry=SpotlightEntry(spot(account),account,false)
  val title=app.stova.profiles.BackgroundTitle(account.id,entry.item.id,entry.item.title)
  r.rememberBackground("p",entry);r.refresh("p")
  val cold=MediaRepository(a,{awaitCancellation()},tmp.root.resolve("snapshots"),backgroundScope,io)
  assertEquals(entry,cold.fixedBackground("p",title).first())
  a.remove(account.id)
  assertNull(cold.fixedBackground("p",title).first())
 }
 @Test fun favoritesSourceQueriesServerWithoutLosingSeasonMetadata()=runTest {
  val a=accounts();val account=add(a,ServerKind.JELLYFIN);var requested:app.stova.profiles.SpotlightSource?=null
  val favorite=spot(account).copy(favorite=true)
  val client=object:MediaServerClient {
   override suspend fun resume()=emptyList<MediaItem>()
   override suspend fun nextUp()=emptyList<MediaItem>()
   override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
   override suspend fun spotlight(source:app.stova.profiles.SpotlightSource):List<MediaItem> { requested=source;return listOf(favorite,spot(account).copy(id="other")) }
  }
  val r=MediaRepository(a,{client},tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  val pool=r.spotlight("p",source=app.stova.profiles.SpotlightSource.FAVORITES).first { it.isNotEmpty() }
  assertEquals(app.stova.profiles.SpotlightSource.FAVORITES,requested)
  assertEquals(listOf(favorite),pool.map { it.item })
 }
 // Lean core 1: every return to Home refetched all four requests per account, and a cold start did it twice.
 private fun countingClient(a:MediaAccount,fetches:MutableList<String>)=object:MediaServerClient {
  override suspend fun resume():List<MediaItem> { fetches+=a.id;return listOf(item(a)) }
  override suspend fun nextUp()=emptyList<MediaItem>()
  override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
 }
 @Test fun returningToHomeWithinTheRefreshAgeFetchesNothing()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);val fetches=mutableListOf<String>();var now=1_000L
  val r=MediaRepository(a,{ countingClient(it,fetches) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  backgroundScope.launch { r.rows("p").collect {} };testScheduler.runCurrent()
  r.refresh("p");r.refresh("p")
  assertEquals("Home's rows and two returns to Home",1,fetches.size)
  now+=REFRESH_MIN_AGE_MILLIS;r.refresh("p");assertEquals(2,fetches.size)
  r.refresh("p",minAgeMillis=0);assertEquals("an explicit refresh never waits",3,fetches.size)
 }
 @Test fun aNewAccountIsFetchedWithoutWaiting()=runTest {
  val a=accounts();val j=add(a,ServerKind.JELLYFIN);val fetches=mutableListOf<String>()
  val r=MediaRepository(a,{ countingClient(it,fetches) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ 1_000L })
  r.refresh("p");val e=add(a,ServerKind.EMBY);r.refresh("p")
  assertEquals(listOf(j.id),fetches.take(1));assertEquals(setOf(j.id,e.id),fetches.drop(1).toSet());assertEquals(3,fetches.size)
 }
 @Test fun aSeasonSwitchIsFetchedWithoutWaiting()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);var seasonalCalls=0
  val client=object:MediaServerClient {
   override suspend fun resume()=emptyList<MediaItem>();override suspend fun nextUp()=emptyList<MediaItem>();override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
   override suspend fun seasonal(season:app.stova.profiles.Season):List<MediaItem> { seasonalCalls++;return emptyList() }
  }
  val r=MediaRepository(a,{ client },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ 1_000L })
  r.refresh("p")
  r.rows("p",app.stova.profiles.Season.HALLOWEEN).first();r.refresh("p")
  assertEquals(1,seasonalCalls)
 }
 // Lean core 2: a narrower hero pool ran its whole-library query on every entry to Home.
 @Test fun aFavouritesPoolIsQueriedOncePerTwoHoursHoweverOftenHomeRestarts()=runTest {
  val a=accounts();val account=add(a,ServerKind.JELLYFIN);var queries=0;var now=1_000L
  val client=object:MediaServerClient {
   override suspend fun resume()=emptyList<MediaItem>();override suspend fun nextUp()=emptyList<MediaItem>();override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
   override suspend fun spotlight(source:app.stova.profiles.SpotlightSource):List<MediaItem> { queries++;return listOf(spot(account).copy(favorite=true)) }
  }
  val r=MediaRepository(a,{ client },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  val favorites=app.stova.profiles.SpotlightSource.FAVORITES
  val seen=mutableListOf<List<String>>()
  fun home()=backgroundScope.launch { r.spotlight("p",source=favorites).collect { seen+=it.map { e -> e.item.id } } }
  home().also { testScheduler.runCurrent() }.cancel()
  home().also { testScheduler.runCurrent() }.cancel()
  assertEquals(1,queries)
  assertEquals("the second entry is served from the pool at once",listOf("f1"),seen.last())
  now+=SPOTLIGHT_REFRESH_MILLIS+1
  home().also { testScheduler.runCurrent() }.cancel()
  assertEquals(2,queries)
 }
 // Lean core 3: without an account, or with nothing new, every 5-minute refresh rewrote snapshots/<id>.json.
 @Test fun noAccountsWritesNoSnapshot()=runTest {
  val r=MediaRepository(accounts(),{ client(it) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  r.refresh("p")
  assertFalse(tmp.root.resolve("snapshots/p.json").exists())
 }
 @Test fun anUnchangedRefreshLeavesTheSnapshotAlone()=runTest {
  val a=accounts();add(a,ServerKind.JELLYFIN);var now=1_000L
  val r=MediaRepository(a,{ client(it) },tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler),clock={ now })
  r.refresh("p");val file=tmp.root.resolve("snapshots/p.json");assertTrue(file.exists())
  // The store keeps its value in memory, so a word written from outside only survives if nothing is written back.
  file.writeText("untouched")
  now+=REFRESH_MIN_AGE_MILLIS;r.refresh("p")
  assertEquals("untouched",file.readText())
 }
}
