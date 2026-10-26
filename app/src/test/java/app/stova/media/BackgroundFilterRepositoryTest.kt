@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media

import app.stova.core.storage.JsonFileStore
import app.stova.core.security.InMemorySecretStore
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.profiles.*
import app.stova.home.HomeRowKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class BackgroundFilterRepositoryTest {
 @get:Rule val tmp=TemporaryFolder()
 private fun accounts()=MediaAccountRepository(JsonFileStore(tmp.root.resolve("accounts.json"),MediaAccountsFile.serializer(),MediaAccountsFile(),UnconfinedTestDispatcher()),InMemorySecretStore())
 private fun film(a:MediaAccount,id:String,age:String?,library:String?=null)=MediaItem(id,a.id,ItemType.MOVIE,id,
  ratings=Ratings(official=age),art=ItemArt(backdrop="art-$id"),favorite=true,backgroundLibraryIds=setOfNotNull(library))
 private open class Server:MediaServerClient {
  override suspend fun resume()=emptyList<MediaItem>()
  override suspend fun nextUp()=emptyList<MediaItem>()
  override suspend fun latest(endpoint:Endpoint)=emptyList<MediaItem>()
 }
 @Test fun overlappingViewsRemainSearchableAndUsableAsFixedArtAfterOfflineColdStart()=runTest {
  val accounts=accounts();val a=accounts.add("p",ServerInfo(ServerKind.JELLYFIN,"s","Server","v","http://demo.invalid"),AuthResult("secret","u","Demo"))
  val refs=setOf(BackgroundLibraryRef(a.id,"one"),BackgroundLibraryRef(a.id,"two"))
  val online=MediaRepository(accounts,{object:Server(){
   override suspend fun backgrounds(source:SpotlightSource,libraryId:String?,query:String?,ids:List<String>?)=listOf(film(a,"shared","12",libraryId))
  }},tmp.root.resolve("snap"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  assertEquals(setOf("one","two"),online.spotlight("p",filter=BackgroundFilter(refs)).first {it.isNotEmpty()}.single().item.backgroundLibraryIds)
  val offline=MediaRepository(accounts,{object:Server(){
   override suspend fun backgrounds(source:SpotlightSource,libraryId:String?,query:String?,ids:List<String>?):List<MediaItem> = throw MediaError.Server(500)
  }},tmp.root.resolve("snap"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  val filter=BackgroundFilter(setOf(BackgroundLibraryRef(a.id,"two")))
  val result=offline.searchBackgrounds("p","shared",filter).single()
  offline.rememberBackground("p",result)
  assertEquals("shared",offline.fixedBackground("p",BackgroundTitle(a.id,"shared","Shared"),filter).first()?.item?.id)
  assertTrue(offline.searchBackgrounds("p","shared",filter.copy(libraries=emptySet())).isEmpty())
 }
 @Test fun cachedSeasonalFavoritesFixedAndSearchCannotBypassAgeLimit()=runTest {
  val accounts=accounts();val a=accounts.add("p",ServerInfo(ServerKind.JELLYFIN,"s","Server","v","http://demo.invalid"),AuthResult("secret","u","Demo"))
  val safe=film(a,"safe","12");val adult=film(a,"adult","18");val unknown=film(a,"unknown",null)
  val all=listOf(adult,unknown,safe)
  JsonFileStore(tmp.root.resolve("snap/p.json"),MediaSnapshot.serializer(),MediaSnapshot(),UnconfinedTestDispatcher()).update {
   MediaSnapshot(spotlight=all.map {SnapshotSpotlight(a.id,it,false)},backgrounds=listOf(SnapshotSpotlight(a.id,adult,false)),
    rows=listOf(SnapshotRow(HomeRowKey.seasonal(a.id,Season.HALLOWEEN),a.id,MediaRowKind.HALLOWEEN,all)))
  }
  val r=MediaRepository(accounts,{object:Server(){
   override suspend fun spotlight(source:SpotlightSource)=all
   override suspend fun items(ids:List<String>)=all.filter {it.id in ids}
   override suspend fun searchTitles(query:String)=all
  }},tmp.root.resolve("snap"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  val filter=BackgroundFilter()
  assertEquals(listOf("safe"),r.spotlight("p",Season.HALLOWEEN,SpotlightSource.FAVORITES,filter).first().map {it.item.id})
  assertEquals("safe",r.cachedPreview("p",filter).first()?.item?.id)
  assertEquals(listOf("safe"),r.searchBackgrounds("p","",filter).map {it.item.id})
  assertNull(r.fixedBackground("p",BackgroundTitle(a.id,"adult","Adult"),filter).first())
  assertEquals(3,r.rows("p",Season.HALLOWEEN).first().single().items.size)
 }
 @Test fun selectedLibraryFetchesImmediatelyCachesSafelyAndNeverWidensOnFailure()=runTest {
  val accounts=accounts();val a=accounts.add("p",ServerInfo(ServerKind.JELLYFIN,"s","Server","v","http://demo.invalid"),AuthResult("secret","u","Demo"))
  val calls=mutableListOf<String?>();var fail=false
  val server=object:Server(){override suspend fun backgrounds(source:SpotlightSource,libraryId:String?,query:String?,ids:List<String>?):List<MediaItem> {
   calls+=libraryId
   if(fail) throw MediaError.Server(500)
   return listOf(film(a,"$libraryId-safe","12",libraryId),film(a,"adult","18",libraryId))
  }}
  fun repo()=MediaRepository(accounts,{server},tmp.root.resolve("snap"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  val r=repo()
  fun filter(id:String)=BackgroundFilter(setOf(BackgroundLibraryRef(a.id,id)))
  assertEquals("family-safe",r.spotlight("p",filter=filter("family")).first {it.isNotEmpty()}.single().item.id)
  assertEquals("series-safe",r.spotlight("p",filter=filter("series")).first {it.isNotEmpty()}.single().item.id)
  assertEquals(listOf("family","series"),calls)
  assertEquals("series-safe",repo().spotlight("p",filter=filter("series")).first().single().item.id)
  fail=true
  val seen=mutableListOf<List<SpotlightEntry>>()
  val job=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {repo().spotlight("p",filter=filter("missing")).collect {seen+=it}}
  runCurrent();assertTrue(seen.isNotEmpty());assertTrue(seen.all {it.isEmpty()});job.cancel()
  assertTrue(r.spotlight("p",filter=BackgroundFilter(emptySet())).first().isEmpty())
  assertEquals(listOf("family","series","series","missing"),calls)
 }
}
