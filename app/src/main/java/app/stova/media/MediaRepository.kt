package app.stova.media
import app.stova.core.storage.JsonFileStore
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaRepository(private val accounts: MediaAccountRepository,private val clientFor: suspend (MediaAccount) -> MediaServerClient?,private val snapshotDir: File,private val scope: CoroutineScope,private val io: CoroutineDispatcher=Dispatchers.IO) {
 private class Slot(val store: JsonFileStore<MediaSnapshot>) {
  val mutex=Mutex();val rows=MutableStateFlow<List<HomeRowUi.Media>?>(null)
 }
 private val slots=ConcurrentHashMap<String,Slot>()
 private fun slot(id: String): Slot { require(id.matches(Regex("[A-Za-z0-9_-]{1,128}"))); return slots.getOrPut(id) { Slot(JsonFileStore(File(snapshotDir,"$id.json"),MediaSnapshot.serializer(),MediaSnapshot(),io)) } }
 private suspend fun loaded(id:String): Slot {
  val slot=slot(id)
  slot.mutex.withLock {
   if(slot.rows.value==null) {
    val byId=accounts.forProfile(id).first().filterNot { it.needsSignIn }.associateBy { it.id }
    slot.rows.value=slot.store.current().rows.mapNotNull { row -> byId[row.accountId]?.let { HomeRowUi.Media(row.key,it,row.kind,row.items,true) } }
   }
  };return slot
 }
 fun rows(profileId:String): Flow<List<HomeRowUi.Media>> = flow {
  val slot=loaded(profileId)
  emit(visible(slot.rows.value.orEmpty(),accounts.forProfile(profileId).first()))
  val refresh=scope.launch { refresh(profileId) }
  try { emitAll(combine(slot.rows.filterNotNull(),accounts.forProfile(profileId)) { rows,active -> visible(rows,active) }) }
  finally { refresh.cancel() }
 }.distinctUntilChanged()
 private fun visible(rows:List<HomeRowUi.Media>,active:List<MediaAccount>): List<HomeRowUi.Media> {
  val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
  return rows.mapNotNull { row -> valid[row.account.id]?.let { row.copy(account=it) } }.filter { it.items.isNotEmpty() }
 }
 suspend fun refresh(profileId:String) {
  val slot=loaded(profileId)
  slot.mutex.withLock {
   val list=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }.sortedWith(compareBy({it.kind},{it.serverName}))
   val rows=supervisorScope { list.map { account -> async {
    val cached=slot.rows.value.orEmpty().filter { it.account.id==account.id }.map { it.copy(offline=true) }
    try {
     withTimeoutOrNull(10_000) {
      val client=clientFor(account) ?: throw MediaError.Unauthorized()
      val resume=client.resume();val next=client.nextUp()
      listOf(
       HomeRowUi.Media(HomeRowKey.continueWatching(account.id),account,MediaRowKind.CONTINUE,mergeContinue(resume,next),false),
       HomeRowUi.Media(HomeRowKey.latestMovies(account.id),account,MediaRowKind.LATEST_MOVIES,client.latest(Endpoint.LATEST_MOVIES),false),
       HomeRowUi.Media(HomeRowKey.latestSeries(account.id),account,MediaRowKind.LATEST_SERIES,client.latest(Endpoint.LATEST_SERIES),false)
      ).filter { it.items.isNotEmpty() }
     } ?: cached
    } catch(_: MediaError.Unauthorized) { accounts.markNeedsSignIn(account.id);emptyList() }
      catch(e: CancellationException) { throw e }
      catch(_: Exception) { cached }
   } }.awaitAll().flatten() }
   val current=visible(rows,accounts.forProfile(profileId).first())
   slot.rows.value=current
   slot.store.update { MediaSnapshot(current.map { SnapshotRow(it.key,it.account.id,it.kind,it.items) },System.currentTimeMillis()) }
  }
 }
}
