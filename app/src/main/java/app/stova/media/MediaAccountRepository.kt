package app.stova.media
import app.stova.core.storage.JsonFileStore
import app.stova.core.security.SecretStore
import app.stova.media.model.*
import app.stova.media.net.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import okhttp3.HttpUrl.Companion.toHttpUrl
@Serializable data class MediaAccountsFile(val accounts: List<MediaAccount> = emptyList())
/** The user's picture from the profile's first account that has one, Jellyfin before Emby, as Home's rows are ordered. */
fun profilePictures(accounts: List<MediaAccount>): Map<String,String> =
 accounts.filter { it.sharedFrom==null }.sortedWith(compareBy({ it.kind },{ it.serverName })).groupBy { it.profileId }.mapNotNull { (profile,list) ->
  list.firstNotNullOfOrNull { a -> a.imageTag?.let { ImageUrls.user(a.baseUrl,a.kind,a.userId,it) } }?.let { profile to it }
 }.toMap()
object MediaSecrets { fun tokenKey(accountId: String) = "media/$accountId/token" }
class MediaAccountRepository(private val store: JsonFileStore<MediaAccountsFile>, private val secrets: SecretStore, private val newId: () -> String = { UUID.randomUUID().toString() }) {
 private val mutex=Mutex()
 private val credentialRevision=MutableStateFlow(0L)
 val all: Flow<List<MediaAccount>> = store.data.map { it.accounts }
 fun imageCredentials(profileId: String): Flow<Map<String,Pair<ServerKind,String>>> = combine(forProfile(profileId),credentialRevision) { accounts,_ ->
  accounts.filterNot { it.needsSignIn }.mapNotNull { account ->
   token(account.id)?.let { token -> val url=account.baseUrl.toHttpUrl(); "${url.host}:${url.port}" to (account.kind to token) }
  }.toMap()
 }.distinctUntilChanged()
 fun forProfile(profileId: String): Flow<List<MediaAccount>> = all.map { accounts -> accounts.filter { it.profileId==profileId } }
 /** Each profile's picture by profile id; a profile without one keeps its letter. */
 val pictures: Flow<Map<String,String>> = all.map(::profilePictures).distinctUntilChanged()
 suspend fun setImageTag(accountId: String, tag: String?) { store.update { it.copy(accounts=it.accounts.map { a -> if(a.id==accountId) a.copy(imageTag=tag) else a }) } }
 suspend fun add(profileId: String, server: ServerInfo, auth: AuthResult): MediaAccount = mutex.withLock {
  val old=store.current().accounts.firstOrNull { it.profileId==profileId && it.serverId==server.id && it.userId==auth.userId && it.sharedFrom==null }
  val account=MediaAccount(old?.id ?: newId(),profileId,server.kind,server.id,server.name,server.baseUrl,auth.userId,auth.userName,imageTag=auth.imageTag)
  secrets.write(MediaSecrets.tokenKey(account.id),auth.token)
  // A new sign-in also mends the children's libraries borrowed from this account.
  store.update { it.copy(accounts=it.accounts.filterNot { it.id==account.id }.map { a -> if(a.sharedFrom==account.id) a.copy(needsSignIn=false) else a }+account) }
  credentialRevision.value++; account
 }
 /** A borrowed account signs in with the grown-up's token, which is stored once, under that account. */
 suspend fun token(accountId: String) = secrets.read(MediaSecrets.tokenKey(store.current().accounts.firstOrNull { it.id==accountId }?.sharedFrom ?: accountId))
 /** The libraries a child borrows from grown-ups' accounts on this TV. */
 fun sharedLibraries(profileId: String): Flow<Set<app.stova.profiles.BackgroundLibraryRef>> = forProfile(profileId).map { accounts ->
  accounts.flatMap { a -> a.sharedFrom?.let { from -> a.libraries.map { app.stova.profiles.BackgroundLibraryRef(from,it) } }.orEmpty() }.toSet()
 }.distinctUntilChanged()
 /**
  * Lends a child the chosen libraries: one borrowed account per grown-up's account, which keeps its id as the
  * choice changes, so the child's rows and their snapshot stay. A borrowed account is never lent on.
  */
 suspend fun shareLibraries(profileId: String, libraries: Set<app.stova.profiles.BackgroundLibraryRef>) = mutex.withLock {
  store.update { file ->
   val sources=file.accounts.filter { it.sharedFrom==null && it.profileId!=profileId }.associateBy { it.id }
   val chosen=libraries.groupBy { it.accountId }.filterKeys { it in sources }
   val kept=file.accounts.filterNot { it.profileId==profileId && it.sharedFrom!=null }
   val lent=chosen.map { (from,refs) ->
    val source=sources.getValue(from)
    val old=file.accounts.firstOrNull { it.profileId==profileId && it.sharedFrom==from }
    source.copy(id=old?.id ?: newId(),profileId=profileId,imageTag=null,sharedFrom=from,libraries=refs.map { it.libraryId }.distinct().sorted())
   }
   file.copy(accounts=kept+lent)
  }
  credentialRevision.value++
 }
 suspend fun markNeedsSignIn(accountId: String) { store.update { it.copy(accounts=it.accounts.map { a -> if(a.id==accountId) a.copy(needsSignIn=true) else a }) } }
 suspend fun remove(accountId: String) = mutex.withLock { removeLocked(accountId) }
 private suspend fun removeLocked(id: String) { store.update { it.copy(accounts=it.accounts.filterNot { a -> a.id==id || a.sharedFrom==id }) }; secrets.delete(MediaSecrets.tokenKey(id)) }
 suspend fun removeForProfile(profileId: String) = mutex.withLock { store.current().accounts.filter { it.profileId==profileId }.forEach { removeLocked(it.id) } }
}
