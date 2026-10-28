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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
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
    @Test fun imageCredentialsFollowReplacementToken() = runTest {
        val r = repo(); r.add("p", srv, AuthResult("first", "u", "n"))
        val seen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { r.imageCredentials("p").collect { seen.add(it.values.single().second) } }
        assertEquals(listOf("first"), seen)
        r.add("p", srv, AuthResult("replacement", "u", "n"))
        assertEquals(listOf("first", "replacement"), seen)
        assertTrue(r.imageCredentials("other-profile").first().isEmpty())
    }
    @Test fun aProfileWearsThePictureOfItsFirstAccountWithOne() = runTest {
        val r = repo()
        val emby = r.add("p", srv.copy(kind = ServerKind.EMBY, id = "e", name = "Arkiv"), AuthResult("t", "u2", "n", "e1"))
        val jellyfin = r.add("p", srv, AuthResult("t", "u 1", "n", "j1"))
        r.add("kid", srv, AuthResult("t", "k", "n"))
        assertEquals(mapOf("p" to "http://jf:8096/UserImage?userId=u%201&tag=j1&maxWidth=360"), r.pictures.first())
        r.setImageTag(jellyfin.id, null)
        assertEquals(mapOf("p" to "http://jf:8096/Users/u2/Images/Primary?tag=e1&maxWidth=360"), r.pictures.first())
        r.remove(emby.id)
        assertTrue("a removed account leaves no picture behind", r.pictures.first().isEmpty())
    }
    @Test fun tokenNeverInFile() = runTest {
        repo().add("p", srv, AuthResult("hemmeleg-token", "u", "n")); assertFalse(tmp.root.resolve("a.json").readText().contains("hemmeleg-token")) }
}
