package app.stova.media
import app.stova.core.storage.JsonFileStore
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import app.stova.profiles.Season
import app.stova.profiles.SpotlightSource
import app.stova.profiles.BackgroundTitle
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
/** Spotlight and season rows query the whole library; they get longer than the rows, and their own clock. */
const val EXTRA_TIMEOUT_MILLIS = 20_000L

class MediaRepository(private val accounts: MediaAccountRepository,private val clientFor: suspend (MediaAccount) -> MediaServerClient?,private val snapshotDir: File,private val scope: CoroutineScope,private val io: CoroutineDispatcher=Dispatchers.IO,private val clock: () -> Long=System::currentTimeMillis) {
 private class Slot(val store: JsonFileStore<MediaSnapshot>) {
  var season: Season = Season.NONE
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
 fun rows(profileId:String, season: Season = Season.NONE): Flow<List<HomeRowUi.Media>> = flow {
  val slot=loaded(profileId)
  slot.season=season
  emit(visible(slot.rows.value.orEmpty(),accounts.forProfile(profileId).first()).filter { it.kind.season == Season.NONE || it.kind.season == season })
  val refresh=scope.launch { refresh(profileId) }
  try { emitAll(combine(slot.rows.filterNotNull(),accounts.forProfile(profileId)) { rows,active -> visible(rows,active).filter { it.kind.season == Season.NONE || it.kind.season == season } }) }
  finally { refresh.cancel() }
 }.distinctUntilChanged()
 /** The hero's titles; served from the snapshot at once, refreshed alongside the rows. */
 fun spotlight(profileId:String, season: Season = Season.NONE, source: SpotlightSource = SpotlightSource.LIBRARY): Flow<List<SpotlightEntry>> = flow {
  val slot=loaded(profileId)
  val pool: Flow<List<SpotlightEntry>> = if(source==SpotlightSource.LIBRARY) slot.spotlight else flow {
   emit(slot.spotlight.value.filter { acceptsSpotlight(it.item,source) })
   while(true) {
    val selected=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }
    val result=supervisorScope { selected.map { account -> async {
     withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) {
      clientFor(account)?.spotlight(source)?.filter { acceptsSpotlight(it,source) }?.map { item ->
       SpotlightEntry(item,account,slot.spotlight.value.any { it.account.id==account.id && it.item.id==item.id && it.newSeason })
      }
     } }
    } }.awaitAll() }
    if(result.any { it!=null }) emit(result.filterNotNull().flatten())
    delay(SPOTLIGHT_REFRESH_MILLIS)
   }
  }
  emitAll(combine(pool,slot.rows.filterNotNull(),accounts.forProfile(profileId)) { entries,rows,active ->
   val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
   seasonalSpotlight(entries.mapNotNull { entry -> valid[entry.account.id]?.let { entry.copy(account=it) } },
    visible(rows,active).map { it.copy(items=it.items.filter { item -> acceptsSpotlight(item,source) }) },season)
  })
 }.distinctUntilChanged()

 /** Search all connected libraries, with the snapshot available when a server is offline. */
 fun cachedPreview(profileId:String): Flow<SpotlightEntry?> = flow {
  val slot=loaded(profileId)
  emitAll(combine(slot.spotlight,accounts.forProfile(profileId)) { entries,active ->
   entries.firstOrNull { entry -> active.any { it.id==entry.account.id && !it.needsSignIn } }
  })
 }

 suspend fun searchBackgrounds(profileId:String,query:String): List<SpotlightEntry> {
  val slot=loaded(profileId)
  val active=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }
  val cached=slot.spotlight.value+slot.rows.value.orEmpty().flatMap { row -> row.items.map { SpotlightEntry(it,row.account,false) } }
  val remote=supervisorScope { active.map { account -> async {
   withTimeoutOrNull(10_000) { orElse(emptyList()) { clientFor(account)?.searchTitles(query).orEmpty().map { SpotlightEntry(it,account,false) } } }.orEmpty()
  } }.awaitAll().flatten() }
  return (remote+cached).filter { it.account.id in active.map { a -> a.id } && it.item.art.backdrop!=null &&
   (it.item.type==ItemType.MOVIE || it.item.type==ItemType.SERIES) && it.item.title.contains(query.trim(),ignoreCase=true) }
   .distinctBy { it.account.id to it.item.id }.sortedBy { it.item.title.lowercase() }
 }

 suspend fun rememberBackground(profileId:String,entry:SpotlightEntry) {
  val slot=loaded(profileId)
  slot.store.update { it.copy(backgrounds=(it.backgrounds.filterNot { saved -> saved.accountId==entry.account.id && saved.item.id==entry.item.id }+
   SnapshotSpotlight(entry.account.id,entry.item,false)).takeLast(12)) }
 }

 @OptIn(ExperimentalCoroutinesApi::class)
 fun fixedBackground(profileId:String,title:BackgroundTitle?): Flow<SpotlightEntry?> = flow {
  if(title==null) { emit(null);return@flow }
  val slot=loaded(profileId)
  emitAll(accounts.forProfile(profileId).mapLatest { active ->
   val account=active.firstOrNull { it.id==title.accountId && !it.needsSignIn } ?: return@mapLatest null
   val cached=slot.store.current().backgrounds.firstOrNull { it.accountId==account.id && it.item.id==title.itemId }?.item
    ?: slot.spotlight.value.firstOrNull { it.account.id==account.id && it.item.id==title.itemId }?.item
   if(cached!=null) SpotlightEntry(cached,account,false) else withTimeoutOrNull(10_000) { orElse(null) {
    clientFor(account)?.items(listOf(title.itemId))?.firstOrNull()?.let { SpotlightEntry(it,account,false) }
   } }
  })
 }
 private fun visible(rows:List<HomeRowUi.Media>,active:List<MediaAccount>): List<HomeRowUi.Media> {
  val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
  return rows.mapNotNull { row -> valid[row.account.id]?.let { row.copy(account=it) } }.filter { it.items.isNotEmpty() }
 }
 private suspend fun <T> orElse(fallback: T,block: suspend () -> T): T = try { block() } catch(e: CancellationException) { throw e } catch(_: Exception) { fallback }
 suspend fun refresh(profileId:String) {
  val slot=loaded(profileId)
  slot.mutex.withLock {
   val season=slot.season
   val list=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }.sortedWith(compareBy({it.kind},{it.serverName}))
   // Never fetched, stale, or a clock that went backwards: fetch the spotlight again.
   val now=clock();val spotlightDue=slot.spotlightAt==0L || now-slot.spotlightAt !in 0 until SPOTLIGHT_REFRESH_MILLIS
   val today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
   val fetched=supervisorScope { list.map { account -> async {
    val cached=slot.rows.value.orEmpty().filter { it.account.id==account.id }.map { it.copy(offline=true) }
    val cachedSpotlight=slot.spotlight.value.filter { it.account.id==account.id }
    try {
     val client=clientFor(account) ?: throw MediaError.Unauthorized()
     coroutineScope {
      // Extras run beside the rows under their own limits: a slow whole-library query must never
      // turn the account's rows "Offline".
      val seasonalRows=async {
       if(season==Season.NONE) emptyList() else withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) {
        listOf(HomeRowUi.Media(HomeRowKey.seasonal(account.id,season),account,if(season==Season.HALLOWEEN) MediaRowKind.HALLOWEEN else MediaRowKind.CHRISTMAS,client.seasonal(season),false))
       } } ?: cached.filter { it.kind.season==season }
      }
      // A spotlight failure never costs the rows; the cached titles stay until the next try.
      val spotlightJob=async {
       if(!spotlightDue) null else withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) {
        val seasons=async { orElse(emptyList()) { client.latest(Endpoint.NEW_SEASONS) } }
        val pool=async { client.latest(Endpoint.SPOTLIGHT) }
        val series=orElse(emptyList()) { client.items(newSeasonSeriesIds(seasons.await(),today)) }
        spotlightEntries(account,pool.await(),seasons.await(),today,series)
       } }
      }
      val core=withTimeoutOrNull(10_000) {
       val resume=async { client.resume() };val next=async { client.nextUp() }
       val movies=async { client.latest(Endpoint.LATEST_MOVIES) };val series=async { client.latest(Endpoint.LATEST_SERIES) }
       listOf(
        HomeRowUi.Media(HomeRowKey.continueWatching(account.id),account,MediaRowKind.CONTINUE,mergeContinue(resume.await(),next.await()),false),
        HomeRowUi.Media(HomeRowKey.latestMovies(account.id),account,MediaRowKind.LATEST_MOVIES,movies.await(),false),
        HomeRowUi.Media(HomeRowKey.latestSeries(account.id),account,MediaRowKind.LATEST_SERIES,series.await(),false))
      }
      val spotlight=spotlightJob.await()
      Fetched(((core ?: cached.filter { it.kind.season==Season.NONE })+seasonalRows.await()).filter { it.items.isNotEmpty() },
       spotlight ?: cachedSpotlight,spotlight!=null)
     }
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
   slot.store.update { it.copy(rows=current.map { row -> SnapshotRow(row.key,row.account.id,row.kind,row.items) },savedAt=now,
    spotlight=spotlight.map { entry -> SnapshotSpotlight(entry.account.id,entry.item,entry.newSeason) },spotlightAt=slot.spotlightAt) }
  }
 }
}
