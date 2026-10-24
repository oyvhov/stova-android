@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media.net
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
import app.stova.media.*
class ServerProbeTest {
 @get:Rule val server = MockWebServer()
    @Test fun candidates() {
        assertEquals(listOf("http://nas:8096", "https://nas", "http://nas"), addressCandidates("nas"))
        assertEquals(listOf("http://nas:8920", "https://nas:8920"), addressCandidates("nas:8920"))
        assertEquals(listOf("https://jf.example.no"), addressCandidates("https://jf.example.no/"))
    }
    @Test fun detectsEmby() = runTest { server.enqueue(MockResponse().setBody("""{"ServerName":"Stue","Id":"x","Version":"4.9","ProductName":"Emby Server"}""")); assertEquals(ServerKind.EMBY, ServerProbe(OkHttpClient()).probe(server.url("/").toString()).kind) }
}
