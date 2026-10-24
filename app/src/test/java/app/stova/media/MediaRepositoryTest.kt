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
class MergeContinueTest {
 private fun ep(id:String,series:String)=MediaItem(id,"a",ItemType.EPISODE,id,seriesId=series)
 @Test fun resumeFirstAndNoSeriesTwice()=assertEquals(listOf("r1","n2"),mergeContinue(listOf(ep("r1","s1")),listOf(ep("n1","s1"),ep("n2","s2"),ep("r1","s1"))).map { it.id })
}
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
 @Test fun removedAccountDropsCachedRows()=runTest {
  val a=accounts();val j=add(a,ServerKind.JELLYFIN);cached(j)
  val r=MediaRepository(a,{client(it)},tmp.root.resolve("snapshots"),backgroundScope,UnconfinedTestDispatcher(testScheduler))
  assertEquals(1,r.rows("p").first().size);a.remove(j.id);assertTrue(r.rows("p").first().isEmpty())
 }
}