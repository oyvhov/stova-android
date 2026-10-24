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
