package app.stova.media
import app.stova.core.storage.JsonFileStore
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import app.stova.profiles.Season
import app.stova.profiles.SpotlightSource
import app.stova.profiles.BackgroundTitle
import app.stova.profiles.BackgroundFilter
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The spotlight pool changes this rarely, so the rotation never jumps on a routine row refresh. */
const val SPOTLIGHT_REFRESH_MILLIS = 2 * 60 * 60 * 1000L
/** Spotlight and season rows query the whole library; they get longer than the rows, and their own clock. */
const val EXTRA_TIMEOUT_MILLIS = 20_000L
const val SEASONAL_REFRESH_MILLIS = 60 * 60 * 1000L
/** Ordinary returns reuse young rows; returning from Spole invalidates them once. */
const val REFRESH_MIN_AGE_MILLIS = 5 * 60 * 1000L
/** Failed rows can recover on the next visit without hammering an unreachable server. */
const val FAILED_REFRESH_RETRY_MILLIS = 30_000L

class MediaRepository(private val accounts: MediaAccountRepository,private val clientFor: suspend (MediaAccount) -> MediaServerClient?,private val snapshotDir: File,private val scope: CoroutineScope,private val io: CoroutineDispatcher=Dispatchers.IO,private val clock: () -> Long=System::currentTimeMillis) {
 private data class BackgroundPoolKey(val source:SpotlightSource,val filter:BackgroundFilter,val accounts:List<String>)
 private class Slot(val store: JsonFileStore<MediaSnapshot>) {
  var season: Season = Season.NONE
  val mutex=Mutex();val rows=MutableStateFlow<List<HomeRowUi.Media>?>(null)
  val spotlight=MutableStateFlow<List<SpotlightEntry>>(emptyList());var spotlightAt=0L
  var seasonalAt=0L;var seasonalFor: Season = Season.NONE
  /** The last attempt, and the season and accounts it was for: a new account or season is never made to wait. */
  var refreshedAt=0L;var refreshedFor: Pair<Season,List<String>>?=null
  var refreshFailed=false
  val playbackRevision=AtomicLong();var refreshedPlaybackRevision=0L
  /** Narrower hero pools (films, series, favourites) and when each is next due; shared by every entry to Home. */
  val extra=MutableStateFlow<Map<SpotlightSource,List<SpotlightEntry>>>(emptyMap());val extraDue=mutableMapOf<SpotlightSource,Long>();val extraMutex=Mutex()
  val restricted=mutableMapOf<BackgroundPoolKey,List<SpotlightEntry>>()
  val restrictedDue=mutableMapOf<BackgroundPoolKey,Long>()
 }
 private class Fetched(val rows: List<HomeRowUi.Media>,val spotlight: List<SpotlightEntry>,val spotlightOk: Boolean,val seasonalOk: Boolean=true,val rowsOk:Boolean=true)
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
  // Off the main thread: parsing and the snapshot write must never stall the screen.
  val refresh=scope.launch(io) { refresh(profileId) }
  try { emitAll(combine(slot.rows.filterNotNull(),accounts.forProfile(profileId)) { rows,active -> visible(rows,active).filter { it.kind.season == Season.NONE || it.kind.season == season } }) }
  finally { refresh.cancel() }
 }.distinctUntilChanged()
 /** The hero's titles; served from the snapshot at once, refreshed alongside the rows. */
 /** A narrow source that leaves fewer than [SPOTLIGHT_MINIMUM] titles is filled from the library under the same filter. */
 @OptIn(ExperimentalCoroutinesApi::class)
 fun spotlight(profileId:String, season: Season = Season.NONE, source: SpotlightSource = SpotlightSource.LIBRARY,
  filter:BackgroundFilter=BackgroundFilter(maxAge=null)): Flow<List<SpotlightEntry>> =
  spotlightOf(profileId,season,source,filter).flatMapLatest { own ->
   if(source==SpotlightSource.LIBRARY || own.size>=SPOTLIGHT_MINIMUM) flowOf(own)
   else spotlightOf(profileId,season,SpotlightSource.LIBRARY,filter).map { fillSpotlight(own,it) }
  }.distinctUntilChanged()

 private fun spotlightOf(profileId:String, season: Season, source: SpotlightSource,
  filter:BackgroundFilter): Flow<List<SpotlightEntry>> = flow {
  val slot=loaded(profileId)
  val pool: Flow<List<SpotlightEntry>> = if(filter.libraries!=null) restrictedSpotlight(profileId,slot,source,filter)
  else if(source==SpotlightSource.LIBRARY) slot.spotlight else flow {
   // Home restarts this flow on every entry; the whole-library query itself runs once per two hours.
   emit(slot.extra.value[source] ?: slot.spotlight.value.filter { acceptsSpotlight(it.item,source) })
   while(true) {
    slot.extraMutex.withLock {
     val now=clock()
     if((slot.extraDue[source] ?: 0L)-now !in 1..SPOTLIGHT_REFRESH_MILLIS) {
      val fetched=fetchSpotlight(profileId,slot,source)
      if(fetched!=null) slot.extra.value+=source to fetched
      // No account answered: try again after a short while, not every time Home comes back.
      slot.extraDue[source]=now+(if(fetched!=null) SPOTLIGHT_REFRESH_MILLIS else REFRESH_MIN_AGE_MILLIS)
     }
    }
    slot.extra.value[source]?.let { emit(it) }
    delay(((slot.extraDue[source] ?: 0L)-clock()).coerceIn(1_000L,SPOTLIGHT_REFRESH_MILLIS))
   }
  }
  emitAll(combine(pool,slot.rows.filterNotNull(),accounts.forProfile(profileId)) { entries,rows,active ->
   val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
   val membership=entries.associateBy { it.account.id to it.item.id }
   seasonalSpotlight(entries.mapNotNull { entry -> valid[entry.account.id]?.let { entry.copy(account=it) } },
    visible(rows,active).map { row->row.copy(items=row.items.map { item ->
     item.copy(backgroundLibraryIds=item.backgroundLibraryIds+membership[row.account.id to item.id]?.item?.backgroundLibraryIds.orEmpty())
    }.filter { acceptsSpotlight(it,source) }) },season)
    .filter { acceptsSpotlight(it.item,source) && acceptsBackground(it.item,filter) }
  })
 }.distinctUntilChanged()

 @OptIn(ExperimentalCoroutinesApi::class)
 private fun restrictedSpotlight(profileId:String,slot:Slot,source:SpotlightSource,filter:BackgroundFilter):Flow<List<SpotlightEntry>> =
  accounts.forProfile(profileId).map { all->all.filterNot { it.needsSignIn } }.distinctUntilChanged().flatMapLatest { active->flow {
   val key=BackgroundPoolKey(source,filter,active.map { it.id }.sorted())
   fun eligible(entries:List<SpotlightEntry>)=entries.filter { entry->active.any { it.id==entry.account.id } && acceptsSpotlight(entry.item,source) && acceptsBackground(entry.item,filter) }
   val byId=active.associateBy { it.id }
   val saved=slot.store.current().backgroundPool.mapNotNull { s->byId[s.accountId]?.let { SpotlightEntry(s.item,it,s.newSeason) } }
   emit(eligible(slot.restricted[key] ?: saved))
   if(filter.libraries.isNullOrEmpty()) return@flow
   while(true) {
    slot.extraMutex.withLock {
     val now=clock()
     if((slot.restrictedDue[key] ?: 0L)-now !in 1..SPOTLIGHT_REFRESH_MILLIS) {
      val previous=slot.restricted[key] ?: saved
      val fetched=supervisorScope { filter.libraries.map { ref->async {
       val account=byId[ref.accountId] ?: return@async (emptyList<SpotlightEntry>() to false)
       val items=withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) { clientFor(account)?.backgrounds(source,ref.libraryId) } }
       if(items==null) previous.filter { it.account.id==ref.accountId && ref.libraryId in it.item.backgroundLibraryIds } to true
       else items.map { SpotlightEntry(it,account,false) } to false
      } }.awaitAll() }
      val selected=eligible(mergeBackgroundMembership(fetched.flatMap { it.first })).take(120)
      if(slot.restricted.size>=12 && key !in slot.restricted) {slot.restricted.clear();slot.restrictedDue.clear()}
      slot.restricted[key]=selected
      slot.restrictedDue[key]=now+if(fetched.any { it.second }) REFRESH_MIN_AGE_MILLIS else SPOTLIGHT_REFRESH_MILLIS
      slot.store.update { old->old.copy(backgroundPool=selected.map { SnapshotSpotlight(it.account.id,it.item,it.newSeason) }) }
     }
    }
    emit(eligible(slot.restricted[key].orEmpty()))
    delay(((slot.restrictedDue[key] ?: 0L)-clock()).coerceIn(1_000L,SPOTLIGHT_REFRESH_MILLIS))
   }
  } }

 suspend fun backgroundLibraries(profileId:String):BackgroundLibraryCatalog = supervisorScope {
  val all=accounts.forProfile(profileId).first()
  val active=all.filterNot { it.needsSignIn }
  val results=active.map { account->async {
   account to withTimeoutOrNull(10_000) { orElse(null) { clientFor(account)?.backgroundLibraries() } }
  } }.awaitAll()
  BackgroundLibraryCatalog(results.flatMap { it.second.orEmpty() }.distinctBy { it.ref },
   results.filter { it.second==null }.map { it.first.serverName }+all.filter {it.needsSignIn}.map {it.serverName},all.isNotEmpty())
 }

 /** One whole-library query per account for a narrower hero pool; null when no account answered. */
 private suspend fun fetchSpotlight(profileId:String,slot:Slot,source:SpotlightSource): List<SpotlightEntry>? {
  val selected=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }
  val result=supervisorScope { selected.map { account -> async {
   withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) {
    clientFor(account)?.spotlight(source)?.filter { acceptsSpotlight(it,source) }?.map { item ->
     SpotlightEntry(item,account,slot.spotlight.value.any { it.account.id==account.id && it.item.id==item.id && it.newSeason })
    }
   } }
  } }.awaitAll() }
  return if(result.any { it!=null }) result.filterNotNull().flatten() else null
 }

 /** Search all connected libraries, with the snapshot available when a server is offline. */
 fun cachedPreview(profileId:String,filter:BackgroundFilter=BackgroundFilter(maxAge=null),source:SpotlightSource=SpotlightSource.LIBRARY): Flow<SpotlightEntry?> = flow {
  val slot=loaded(profileId)
  emitAll(combine(slot.spotlight,slot.store.data,accounts.forProfile(profileId)) { entries,saved,active ->
   val byId=active.filterNot { it.needsSignIn }.associateBy { it.id }
   (entries+saved.backgroundPool.mapNotNull { s->byId[s.accountId]?.let { SpotlightEntry(s.item,it,s.newSeason) } })
    .firstOrNull { entry -> entry.account.id in byId && acceptsBackground(entry.item,filter) && acceptsSpotlight(entry.item,source) }
  })
 }

 suspend fun searchBackgrounds(profileId:String,query:String,filter:BackgroundFilter=BackgroundFilter(maxAge=null)): List<SpotlightEntry> {
  val slot=loaded(profileId)
  val active=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }
  val byId=active.associateBy {it.id}
  val saved=slot.store.current()
  val cached=slot.spotlight.value+slot.rows.value.orEmpty().flatMap { row -> row.items.map { SpotlightEntry(it,row.account,false) } }+
   (saved.backgroundPool+saved.backgrounds).mapNotNull { entry->byId[entry.accountId]?.let {SpotlightEntry(entry.item,it,entry.newSeason)} }
  val remote=supervisorScope { active.map { account -> async {
   withTimeoutOrNull(10_000) { orElse(emptyList()) {
    val server=clientFor(account)
    val libraries=filter.libraries?.filter { it.accountId==account.id }?.map { it.libraryId } ?: listOf(null)
    libraries.flatMap { server?.backgrounds(SpotlightSource.LIBRARY,it,query=query).orEmpty() }.map { SpotlightEntry(it,account,false) }
   } }.orEmpty()
  } }.awaitAll().flatten() }
  return mergeBackgroundMembership(remote+cached).filter { it.account.id in active.map { a -> a.id } && it.item.art.backdrop!=null &&
   (it.item.type==ItemType.MOVIE || it.item.type==ItemType.SERIES) && it.item.title.contains(query.trim(),ignoreCase=true) && acceptsBackground(it.item,filter) }
   .distinctBy { it.account.id to it.item.id }.sortedBy { it.item.title.lowercase() }
 }

 suspend fun rememberBackground(profileId:String,entry:SpotlightEntry) {
  val slot=loaded(profileId)
  slot.store.update { it.copy(backgrounds=(it.backgrounds.filterNot { saved -> saved.accountId==entry.account.id && saved.item.id==entry.item.id }+
   SnapshotSpotlight(entry.account.id,entry.item,false)).takeLast(12)) }
 }

 @OptIn(ExperimentalCoroutinesApi::class)
 fun fixedBackground(profileId:String,title:BackgroundTitle?,filter:BackgroundFilter=BackgroundFilter(maxAge=null)): Flow<SpotlightEntry?> = flow {
  if(title==null) { emit(null);return@flow }
  val slot=loaded(profileId)
  emitAll(accounts.forProfile(profileId).mapLatest { active ->
   val account=active.firstOrNull { it.id==title.accountId && !it.needsSignIn } ?: return@mapLatest null
   val cached=slot.store.current().backgrounds.firstOrNull { it.accountId==account.id && it.item.id==title.itemId }?.item
    ?: slot.spotlight.value.firstOrNull { it.account.id==account.id && it.item.id==title.itemId }?.item
   if(cached!=null && acceptsBackground(cached,filter)) SpotlightEntry(cached,account,false) else withTimeoutOrNull(10_000) { orElse(null) {
    val server=clientFor(account)
    val libraries=filter.libraries?.filter { it.accountId==account.id }?.map { it.libraryId } ?: listOf(null)
    libraries.flatMap { server?.backgrounds(SpotlightSource.LIBRARY,it,ids=listOf(title.itemId)).orEmpty() }
     .firstOrNull { it.id==title.itemId && acceptsBackground(it,filter) }?.let { SpotlightEntry(it,account,false) }
   } }
  })
 }
 private fun visible(rows:List<HomeRowUi.Media>,active:List<MediaAccount>): List<HomeRowUi.Media> {
  val valid=active.filterNot { it.needsSignIn }.associateBy { it.id }
  return rows.mapNotNull { row -> valid[row.account.id]?.let { row.copy(account=it) } }.filter { it.items.isNotEmpty() }
 }
 private suspend fun <T> orElse(fallback: T,block: suspend () -> T): T = try { block() } catch(e: CancellationException) { throw e } catch(_: Exception) { fallback }
 /** Spole may change progress even when opened on its own home. Keep invalidation profile-scoped. */
 fun playbackOpened(profileId:String) { slot(profileId).playbackRevision.incrementAndGet() }
 suspend fun refresh(profileId:String,minAgeMillis:Long=REFRESH_MIN_AGE_MILLIS) {
  val slot=loaded(profileId)
  slot.mutex.withLock {
   val season=slot.season
   val list=accounts.forProfile(profileId).first().filterNot { it.needsSignIn }.sortedWith(compareBy({it.kind},{it.serverName}))
   val now=clock();val key=season to list.map { it.id }
   val playbackRevision=slot.playbackRevision.get()
   // A trip to the player can change progress before the ordinary five-minute interval expires.
   val refreshAge=if(slot.refreshFailed) minOf(minAgeMillis,FAILED_REFRESH_RETRY_MILLIS) else minAgeMillis
   if(slot.refreshedAt!=0L && now-slot.refreshedAt in 0 until refreshAge && slot.refreshedFor==key && slot.refreshedPlaybackRevision==playbackRevision) return
   // Nobody to ask: the rows stay as the accounts filter leaves them, and the snapshot is not touched.
   if(list.isEmpty()) return
   // Never fetched, stale, or a clock that went backwards: fetch the spotlight again.
   val spotlightDue=slot.spotlightAt==0L || now-slot.spotlightAt !in 0 until SPOTLIGHT_REFRESH_MILLIS
   // Season rows query the whole library too: once an hour, and again when the season changes.
   val seasonalDue=season!=Season.NONE && (slot.seasonalFor!=season || slot.seasonalAt==0L || now-slot.seasonalAt !in 0 until SEASONAL_REFRESH_MILLIS)
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
       when {
        season==Season.NONE -> emptyList<HomeRowUi.Media>() to true
        !seasonalDue -> slot.rows.value.orEmpty().filter { it.account.id==account.id && it.kind.season==season } to true
        else -> withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) {
         listOf(HomeRowUi.Media(HomeRowKey.seasonal(account.id,season),account,if(season==Season.HALLOWEEN) MediaRowKind.HALLOWEEN else MediaRowKind.CHRISTMAS,client.seasonal(season),false))
        } }?.let { it to true } ?: (cached.filter { it.kind.season==season } to false)
       }
      }
      // A spotlight failure never costs the rows; the cached titles stay until the next try.
      val spotlightJob=async {
       // supervisorScope: a failing spotlight query is "no spotlight this time", never a cancelled account.
       if(!spotlightDue) null else withTimeoutOrNull(EXTRA_TIMEOUT_MILLIS) { orElse(null) { supervisorScope {
        val seasons=async { orElse(emptyList()) { client.latest(Endpoint.NEW_SEASONS) } }
        val pool=async { client.latest(Endpoint.SPOTLIGHT) }
        val series=orElse(emptyList()) { client.items(newSeasonSeriesIds(seasons.await(),today)) }
        spotlightEntries(account,pool.await(),seasons.await(),today,series)
       } } }
      }
      val core=withTimeoutOrNull(10_000) {
       val resume=async { client.resume() };val next=async { client.nextUp() }
       val movies=async { client.latest(Endpoint.LATEST_MOVIES) };val series=async { client.latest(Endpoint.LATEST_SERIES) }
       listOf(
        HomeRowUi.Media(HomeRowKey.continueWatching(account.id),account,MediaRowKind.CONTINUE,mergeContinue(resume.await(),next.await()),false),
        HomeRowUi.Media(HomeRowKey.latestMovies(account.id),account,MediaRowKind.LATEST_MOVIES,movies.await(),false),
        HomeRowUi.Media(HomeRowKey.latestSeries(account.id),account,MediaRowKind.LATEST_SERIES,series.await(),false))
      }
      val spotlight=spotlightJob.await();val (seasonal,seasonalOk)=seasonalRows.await()
      Fetched(((core ?: cached.filter { it.kind.season==Season.NONE })+seasonal).filter { it.items.isNotEmpty() },
       spotlight ?: cachedSpotlight,spotlight!=null,seasonalOk,rowsOk=core!=null)
     }
    } catch(_: MediaError.Unauthorized) { accounts.markNeedsSignIn(account.id);Fetched(emptyList(),emptyList(),true) }
      catch(e: CancellationException) { throw e }
      catch(_: Exception) { Fetched(cached,cachedSpotlight,false,rowsOk=false) }
   } }.awaitAll() }
   val active=accounts.forProfile(profileId).first()
   val current=visible(fetched.flatMap { it.rows },active)
   val valid=active.filterNot { it.needsSignIn }.mapTo(mutableSetOf()) { it.id }
   val spotlight=fetched.flatMap { it.spotlight }.filter { it.account.id in valid }.sortedByDescending { it.newSeason }
   if(spotlightDue && fetched.all { it.spotlightOk }) slot.spotlightAt=now
   if(seasonalDue && fetched.all { it.seasonalOk }) { slot.seasonalAt=now;slot.seasonalFor=season }
   slot.rows.value=current;slot.spotlight.value=spotlight
   // Most refreshes find nothing new; the snapshot is written only when it would read differently.
   val saved=slot.store.current()
   val next=saved.copy(rows=current.map { row -> SnapshotRow(row.key,row.account.id,row.kind,row.items) },
    spotlight=spotlight.map { entry -> SnapshotSpotlight(entry.account.id,entry.item,entry.newSeason) },spotlightAt=slot.spotlightAt)
   if(next!=saved) slot.store.update { it.copy(rows=next.rows,spotlight=next.spotlight,spotlightAt=next.spotlightAt,savedAt=now) }
   slot.refreshedAt=now;slot.refreshedFor=key
   slot.refreshFailed=fetched.any { !it.rowsOk || it.rows.any { row -> row.offline } }
   slot.refreshedPlaybackRevision=playbackRevision
  }
 }
}
