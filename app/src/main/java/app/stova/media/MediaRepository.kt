package app.stova.media
import app.stova.core.storage.JsonFileStore
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The spotlight pool changes this rarely, so the rotation never jumps on a routine row refresh. */
const val SPOTLIGHT_REFRESH_MILLIS = 2 * 60 * 60 * 1000L

class MediaRepository(private val accounts: MediaAccountRepository,private val clientFor: suspend (MediaAccount) -> MediaServerClient?,private val snapshotDir: File,private val scope: CoroutineScope,private val io: CoroutineDispatcher=Dispatchers.IO,private val clock: () -> Long=System::currentTimeMillis) {
 private class Slot(val store: JsonFileStore<MediaSnapshot>) {
  val mutex=Mutex();val rows=MutableStateFlow<List<HomeRowUi.Media>?>(null)
  val spotlight=MutableStateFlow<List<SpotlightEntry>>(emptyList());var spotlightAt=0L
 }
 private class Fetched(val rows: List<HomeRowUi.Media>,val spotlight: List<SpotlightEntry>,val spotlightOk: Boolean)
 private val slots=ConcurrentHashMap<String,Slot>()
 private fun slot(id: String): Slot { require(id.matches(Regex("[A-Za-z0-9_-]{1,128}"))); return slots.getOrPut(id) { Slot(JsonFileStore(File(snapshotDir,"$id.json"),MediaSnapshot.serializer(),MediaSnapshot(),io)) } }
 private suspend fun loaded(id:String): Slot {
  val slot=slot(id)
  slot.mutex.withLock {
   if(slot.rows.value==null) {
    val byId=accounts.forProfile(id).first().filterNot { it.needsSignIn }.associateBy { it.id }
    val saved=slot.store.current()
    slot.spotlight.value=saved.spotlight.mapNotNull { s -> byId[s.accountId]?.let { SpotlightEntry(s.item,it,s.newSeason) } }
    slot.spotlightAt=saved.spotlightAt
    slot.rows.value=saved.rows.mapNotNull { row -> byId[row.accountId]?.let { HomeRowUi.Media(row.key,it,row.kind,row.items,true) } }
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
 /** The hero's titles; served from the snapshot at once, refreshed alongside the rows. */
 fun spotlight(profileId:String): Flow<List<SpotlightEntry>> = flow {
  val slot=loaded(profileId)
  emitAll(combine(slot.spotlight,accounts.forProfile(profileId)) { entries,active ->
   val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
   entries.mapNotNull { entry -> valid[entry.account.id]?.let { entry.copy(account=it) } }
  })
 }.distinctUntilChanged()
 private fun visible(rows:List<HomeRowUi.Media>,active:List<MediaAccount>): List<HomeRowUi.Media> {
  val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
  return rows.mapNotNull { row -> valid[row.account.id]?.let { row.copy(account=it) } }.filter { it.items.isNotEmpty() }
 }
 private suspend fun <T> orElse(fallback: T,block: suspend () -> T): T = try { block() } catch(e: CancellationException) { throw e } catch(_: Exception) { fallback }
 suspend fun refresh(profileId:String) {
  val slot=loaded(profileId)
  slot.mutex.withLock {
   val list=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }.sortedWith(compareBy({it.kind},{it.serverName}))
   // Never fetched, stale, or a clock that went backwards: fetch the spotlight again.
   val now=clock();val spotlightDue=slot.spotlightAt==0L || now-slot.spotlightAt !in 0 until SPOTLIGHT_REFRESH_MILLIS
   val today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
   val fetched=supervisorScope { list.map { account -> async {
    val cached=slot.rows.value.orEmpty().filter { it.account.id==account.id }.map { it.copy(offline=true) }
    val cachedSpotlight=slot.spotlight.value.filter { it.account.id==account.id }
    try {
     withTimeoutOrNull(10_000) {
      val client=clientFor(account) ?: throw MediaError.Unauthorized()
      val resume=client.resume();val next=client.nextUp()
      val rows=listOf(
       HomeRowUi.Media(HomeRowKey.continueWatching(account.id),account,MediaRowKind.CONTINUE,mergeContinue(resume,next),false),
       HomeRowUi.Media(HomeRowKey.latestMovies(account.id),account,MediaRowKind.LATEST_MOVIES,client.latest(Endpoint.LATEST_MOVIES),false),
       HomeRowUi.Media(HomeRowKey.latestSeries(account.id),account,MediaRowKind.LATEST_SERIES,client.latest(Endpoint.LATEST_SERIES),false)
      ).filter { it.items.isNotEmpty() }
      // A spotlight failure never costs the rows; the cached titles stay until the next try.
      val spotlight=if(!spotlightDue) null else orElse(null) {
       spotlightEntries(account,client.latest(Endpoint.SPOTLIGHT),orElse(emptyList()) { client.latest(Endpoint.NEW_SEASONS) },today)
      }
      Fetched(rows,spotlight ?: cachedSpotlight,spotlight!=null)
     } ?: Fetched(cached,cachedSpotlight,false)
    } catch(_: MediaError.Unauthorized) { accounts.markNeedsSignIn(account.id);Fetched(emptyList(),emptyList(),true) }
      catch(e: CancellationException) { throw e }
      catch(_: Exception) { Fetched(cached,cachedSpotlight,false) }
   } }.awaitAll() }
   val active=accounts.forProfile(profileId).first()
   val current=visible(fetched.flatMap { it.rows },active)
   val valid=active.filterNot { it.needsSignIn }.mapTo(mutableSetOf()) { it.id }
   val spotlight=fetched.flatMap { it.spotlight }.filter { it.account.id in valid }.sortedByDescending { it.newSeason }
   if(spotlightDue && fetched.all { it.spotlightOk }) slot.spotlightAt=now
   slot.rows.value=current;slot.spotlight.value=spotlight
   slot.store.update { MediaSnapshot(current.map { SnapshotRow(it.key,it.account.id,it.kind,it.items) },now,
    spotlight.map { SnapshotSpotlight(it.account.id,it.item,it.newSeason) },slot.spotlightAt) }
  }
 }
}
