@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.core.storage.*
import app.stova.core.security.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.mockwebserver.*
import java.net.InetAddress
class MediaAccountRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val secrets = InMemorySecretStore()
    private fun repo() = MediaAccountRepository(JsonFileStore(tmp.root.resolve("a.json"), MediaAccountsFile.serializer(), MediaAccountsFile(), UnconfinedTestDispatcher()), secrets)
    private val srv = ServerInfo(ServerKind.JELLYFIN, "srv", "Heim", "10.10.0", "http://jf:8096")
    @Test fun sameUserOnSameServerReplaces() = runTest {
        val r = repo(); r.add("p", srv, AuthResult("t1", "u", "n")); val b = r.add("p", srv, AuthResult("t2", "u", "n"))
        assertEquals(1, r.forProfile("p").first().size); assertEquals("t2", r.token(b.id)) }
    @Test fun removeDeletesToken() = runTest {
        val r = repo(); val a = r.add("p", srv, AuthResult("t", "u", "n")); r.remove(a.id); assertNull(secrets.read(MediaSecrets.tokenKey(a.id))) }
    @Test fun tokenNeverInFile() = runTest {
        repo().add("p", srv, AuthResult("hemmeleg-token", "u", "n")); assertFalse(tmp.root.resolve("a.json").readText().contains("hemmeleg-token")) }
}
class ServerProbeTest {
 @get:Rule val server = MockWebServer()
    @Test fun candidates() {
        assertEquals(listOf("http://nas:8096", "https://nas", "http://nas"), addressCandidates("nas"))
        assertEquals(listOf("http://nas:8920", "https://nas:8920"), addressCandidates("nas:8920"))
        assertEquals(listOf("https://jf.example.no"), addressCandidates("https://jf.example.no/"))
    }
    @Test fun detectsEmby() = runTest { server.enqueue(MockResponse().setBody("""{"ServerName":"Stue","Id":"x","Version":"4.9","ProductName":"Emby Server"}""")); assertEquals(ServerKind.EMBY, ServerProbe(OkHttpClient()).probe(server.url("/").toString()).kind) }
}
class MediaAuthTest {
 @get:Rule val server = MockWebServer()
    @Test fun quickConnectPollsUntilAuthenticated() = kotlinx.coroutines.runBlocking { server.enqueue(MockResponse().setBody("""{"Code":"123456","Secret":"s"}"""))
 server.enqueue(MockResponse().setBody("""{"Authenticated":false}""")); server.enqueue(MockResponse().setBody("""{"Authenticated":true}"""))
 server.enqueue(MockResponse().setBody("""{"AccessToken":"t","User":{"Id":"u","Name":"n"}}"""))
 val auth=MediaAuth(OkHttpClient(),ClientIdentity("d","i","v")); val srv=ServerInfo(ServerKind.JELLYFIN,"x","S","v",server.url("/").toString().trimEnd('/'))
 val code=auth.quickConnectStart(srv); assertEquals("123456",code.code)
 assertEquals(AuthResult("t","u","n"),auth.quickConnectAwait(srv,code.secret,pollMillis=1))
 repeat(3) { server.takeRequest() }; assertTrue(server.takeRequest().body.readUtf8().contains("s")) }
}
class LanDiscoveryTest {
    @Test fun senderWinsOverVirtualAdapter() {
        val d = parseDiscoveryReply("""{"Address":"http://172.26.192.1:8096","Id":"x","Name":"NAS"}""", InetAddress.getByName("192.168.1.20"), ServerKind.JELLYFIN)
        assertEquals("http://192.168.1.20:8096", d!!.address) }
    @Test fun hostnameKept() {
        val d = parseDiscoveryReply("""{"Address":"https://jf.example.no","Id":"x","Name":"NAS"}""", InetAddress.getByName("192.168.1.20"), ServerKind.JELLYFIN)
        assertEquals("https://jf.example.no", d!!.address) }
}
