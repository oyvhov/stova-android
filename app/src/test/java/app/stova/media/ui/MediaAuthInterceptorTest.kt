package app.stova.media.ui
import app.stova.media.model.*
import app.stova.media.net.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.util.Locale
class MediaAuthInterceptorTest {
    @get:Rule val server = MockWebServer()
    @Test fun addsTokenOnlyForAccountHost() {
        val host = "${server.hostName}:${server.port}"
        val http = OkHttpClient.Builder().addInterceptor(MediaAuthInterceptor({ mapOf(host to (ServerKind.JELLYFIN to "t")) }, ClientIdentity("d", "i", "v"))).build()
        server.enqueue(MockResponse()); http.newCall(Request.Builder().url(server.url("/Items/x/Images/Primary")).build()).execute().close()
        assertTrue(server.takeRequest().getHeader("Authorization")!!.contains("Token=\"t\""))
        val other = OkHttpClient.Builder().addInterceptor(MediaAuthInterceptor({ emptyMap() }, ClientIdentity("d", "i", "v"))).build()
        server.enqueue(MockResponse()); other.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
class RatingTextTest {
    @Test fun nynorskUsesComma() = assertEquals("7,8", communityText(7.84, Locale.forLanguageTag("nn")))
    @Test fun englishUsesDot() = assertEquals("7.8", communityText(7.84, Locale.ENGLISH))
    @Test fun criticPercentHasNumber() = assertTrue(criticText(91, Locale.forLanguageTag("nn")).startsWith("91"))
}
