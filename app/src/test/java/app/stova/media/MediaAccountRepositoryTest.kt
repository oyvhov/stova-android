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
    private fun ref(account: MediaAccount, library: String) = app.stova.profiles.BackgroundLibraryRef(account.id, library)
    @Test fun aChildBorrowsAGrownUpsAccountForTheChosenLibrariesOnly() = runTest {
        val r = repo(); val adult = r.add("p", srv, AuthResult("t", "u", "Øyvind", "pic"))
        r.shareLibraries("kid", setOf(ref(adult, "barnefilmar"), ref(adult, "barneseriar")))
        val shared = r.forProfile("kid").first().single()
        assertEquals(adult.id, shared.sharedFrom)
        assertEquals(listOf("barnefilmar", "barneseriar"), shared.libraries)
        assertEquals("the grown-up's sign-in, never a copy", "t", r.token(shared.id))
        assertNull(secrets.read(MediaSecrets.tokenKey(shared.id)))
        assertEquals(setOf(ref(adult, "barnefilmar"), ref(adult, "barneseriar")), r.sharedLibraries("kid").first())
        assertFalse("the child never wears the grown-up's picture", "kid" in r.pictures.first())
        r.shareLibraries("kid", setOf(ref(adult, "barnefilmar")))
        assertEquals(listOf(shared.id to listOf("barnefilmar")), r.forProfile("kid").first().map { it.id to it.libraries })
        r.shareLibraries("kid", emptySet())
        assertTrue(r.forProfile("kid").first().isEmpty())
        assertEquals("t", r.token(adult.id))
    }
    @Test fun theBorrowedLibrariesGoWithTheGrownUpsAccountAndComeBackWithItsSignIn() = runTest {
        val r = repo(); val adult = r.add("p", srv, AuthResult("t", "u", "Øyvind"))
        r.shareLibraries("kid", setOf(ref(adult, "barnefilmar")))
        val shared = r.forProfile("kid").first().single()
        r.markNeedsSignIn(shared.id)
        r.add("p", srv, AuthResult("t2", "u", "Øyvind"))
        assertFalse(r.forProfile("kid").first().single().needsSignIn)
        assertEquals("t2", r.token(shared.id))
        r.remove(adult.id)
        assertTrue(r.forProfile("kid").first().isEmpty())
        val again = r.add("p", srv, AuthResult("t3", "u", "Øyvind")); r.shareLibraries("kid", setOf(ref(again, "barnefilmar")))
        r.shareLibraries("other", setOf(ref(r.forProfile("kid").first().single(), "barnefilmar")))
        assertTrue("a borrowed account is never lent on", r.forProfile("other").first().isEmpty())
    }
    @Test fun tokenNeverInFile() = runTest {
        repo().add("p", srv, AuthResult("hemmeleg-token", "u", "n")); assertFalse(tmp.root.resolve("a.json").readText().contains("hemmeleg-token")) }
}
