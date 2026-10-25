package app.stova.media.net

import app.stova.media.model.*
import app.stova.profiles.Season
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class SeasonalEndpointsTest {
    @get:Rule val server = MockWebServer()
    @Test fun tagsAndGenresAreSeparateOrQueriesForBothServersWithFallback() = runTest {
        for (kind in ServerKind.entries) {
            val client = JellyfinEmbyClient(MediaAccount("a","p",kind,"s","S",server.url("/").toString().trimEnd('/'),"u","U"), "t", OkHttpClient(), ClientIdentity("d","i","v"))
            val body = """{"Items":[{"Id":"m","Type":"Movie","Name":"Night"}]}"""
            if (kind == ServerKind.JELLYFIN) server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody(body)); server.enqueue(MockResponse().setBody(body))
            assertEquals(listOf("m"), client.seasonal(Season.HALLOWEEN).map { it.id })
            if (kind == ServerKind.JELLYFIN) assertTrue(server.takeRequest().path!!.startsWith("/Items?"))
            val tag = server.takeRequest().requestUrl!!
            val genre = server.takeRequest().requestUrl!!
            assertEquals("/Users/u/Items", tag.encodedPath)
            assertEquals("halloween", tag.queryParameter("Tags")); assertNull(tag.queryParameter("Genres"))
            assertEquals("Horror|Skrekk", genre.queryParameter("Genres")); assertNull(genre.queryParameter("Tags"))
            for (url in listOf(tag, genre)) {
                assertEquals("true", url.queryParameter("Recursive"))
                assertEquals("Movie,Series", url.queryParameter("IncludeItemTypes"))
            }
            server.enqueue(MockResponse().setBody("[]")); assertTrue(client.seasonal(Season.CHRISTMAS).isEmpty())
            assertEquals("christmas", server.takeRequest().requestUrl!!.queryParameter("Tags"))
            val before = server.requestCount; assertTrue(client.seasonal(Season.NONE).isEmpty()); assertEquals(before, server.requestCount)
        }
    }
}
