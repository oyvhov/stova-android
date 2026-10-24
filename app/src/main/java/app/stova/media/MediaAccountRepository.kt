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
 suspend fun add(profileId: String, server: ServerInfo, auth: AuthResult): MediaAccount = mutex.withLock {
  val old=store.current().accounts.firstOrNull { it.profileId==profileId && it.serverId==server.id && it.userId==auth.userId }
  val account=MediaAccount(old?.id ?: newId(),profileId,server.kind,server.id,server.name,server.baseUrl,auth.userId,auth.userName)
  secrets.write(MediaSecrets.tokenKey(account.id),auth.token)
  store.update { it.copy(accounts=it.accounts.filterNot { it.id==account.id }+account) }; credentialRevision.value++; account
 }
 suspend fun token(accountId: String) = secrets.read(MediaSecrets.tokenKey(accountId))
 suspend fun markNeedsSignIn(accountId: String) { store.update { it.copy(accounts=it.accounts.map { a -> if(a.id==accountId) a.copy(needsSignIn=true) else a }) } }
 suspend fun remove(accountId: String) = mutex.withLock { removeLocked(accountId) }
 private suspend fun removeLocked(id: String) { store.update { it.copy(accounts=it.accounts.filterNot { a -> a.id==id }) }; secrets.delete(MediaSecrets.tokenKey(id)) }
 suspend fun removeForProfile(profileId: String) = mutex.withLock { store.current().accounts.filter { it.profileId==profileId }.forEach { removeLocked(it.id) } }
}
