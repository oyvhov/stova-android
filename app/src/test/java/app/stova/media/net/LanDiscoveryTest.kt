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
class LanDiscoveryTest {
    @Test fun discoveryKeepsKindFromProbeSocket() = runTest {
        val discovery = LanDiscovery { kind, _, reply ->
            reply("""{"Address":"http://192.168.1.20:8096","Id":"${kind.name}","Name":"Demo"}""", InetAddress.getByName("192.168.1.20"))
        }
        assertEquals(ServerKind.entries.toSet(), discovery.discover().toList().map { it.kind }.toSet())
    }
    @Test fun senderWinsOverVirtualAdapter() {
        val d = parseDiscoveryReply("""{"Address":"http://172.26.192.1:8096","Id":"x","Name":"NAS"}""", InetAddress.getByName("192.168.1.20"), ServerKind.JELLYFIN)
        assertEquals("http://192.168.1.20:8096", d!!.address) }
    @Test fun hostnameKept() {
        val d = parseDiscoveryReply("""{"Address":"https://jf.example.no","Id":"x","Name":"NAS"}""", InetAddress.getByName("192.168.1.20"), ServerKind.JELLYFIN)
        assertEquals("https://jf.example.no", d!!.address) }
}
