package app.stova.media.net
import app.stova.media.model.*
import org.junit.*
import org.junit.Assert.*
import okhttp3.*
import okhttp3.mockwebserver.*
import kotlinx.coroutines.test.runTest
class JellyfinEmbyClientTest {
    @get:Rule val server = MockWebServer()
    private fun client(kind: ServerKind = ServerKind.JELLYFIN) = JellyfinEmbyClient(
        MediaAccount("a", "p", kind, "s", "S", server.url("/").toString().trimEnd('/'), "u", "n"), "tok", OkHttpClient(), ClientIdentity("d", "i", "v"))

    @Test fun listsOnlyVideoLibrariesAndScopesCandidatesByOneParentWithoutAnAgeScore() = runTest {
        server.enqueue(MockResponse().setBody("""{"Items":[{"Id":"family","Name":"Familie","CollectionType":"movies"},{"Id":"series","Name":"Seriar","CollectionType":"tvshows"},{"Id":"music","Name":"Musikk","CollectionType":"music"}]}"""))
        assertEquals(listOf("family","series"),client().backgroundLibraries().map {it.ref.libraryId})
        assertEquals("/Users/u/Views",server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"Items":[{"Id":"film","Name":"Film","Type":"Movie","OfficialRating":"NO-12"}]}"""))
        val item=client().backgrounds(app.stova.profiles.SpotlightSource.FAVORITES,"family & kids").single()
        assertEquals(setOf("family & kids"),item.backgroundLibraryIds)
        val request=server.takeRequest().requestUrl!!
        assertEquals("family & kids",request.queryParameter("ParentId"))
        assertEquals("true",request.queryParameter("Recursive"))
        assertEquals("IsFavorite",request.queryParameter("Filters"))
        assertNull(request.queryParameter("MaxParentalRating"))
    }
    // A favourite library tile shows the library's own image, the one Spole and Jellyfin show.
    @Test fun librariesCarryTheirOwnImage() = runTest {
        server.enqueue(MockResponse().setBody("""{"Items":[{"Id":"film","Name":"Filmar","CollectionType":"movies","ImageTags":{"Primary":"t1"}},{"Id":"tv","Name":"Seriar","CollectionType":"tvshows"}]}"""))
        val libraries = client().backgroundLibraries()
        val base = server.url("/").toString().trimEnd('/')
        assertEquals("$base/Items/film/Images/Primary?maxWidth=640&tag=t1", libraries.first().image)
        assertNull(libraries.last().image)
    }
    @Test fun readsTheTagOfTheUsersPicture() = runTest {
        server.enqueue(MockResponse().setBody("""{"Id":"u","Name":"n","PrimaryImageTag":"t9"}"""))
        assertEquals("t9", client().userImageTag())
        assertEquals("/Users/u", server.takeRequest().path)
        server.enqueue(MockResponse().setBody("""{"Id":"u","Name":"n"}"""))
        assertNull(client(ServerKind.EMBY).userImageTag())
    }
    @Test fun fallsBackOn404() = runTest {
        server.enqueue(MockResponse().setResponseCode(404)); server.enqueue(MockResponse().setBody("""{"Items":[]}"""))
        assertEquals(emptyList<MediaItem>(), client().resume())
        assertTrue(server.takeRequest().path!!.startsWith("/UserItems/Resume")); assertTrue(server.takeRequest().path!!.startsWith("/Users/u/Items/Resume"))
    }
    @Test(expected = MediaError.Unauthorized::class) fun unauthorizedDoesNotFallBack() = runTest {
        server.enqueue(MockResponse().setResponseCode(401)); client().resume(); Unit }
    @Test fun embyTokenDoesNotFollowForeignRedirect() = runTest {
        MockWebServer().use { other ->
            other.start()
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", other.url("/redirected")))
            other.enqueue(MockResponse().setBody("[]"))
            val account = MediaAccount("a", "p", ServerKind.EMBY, "s", "Demo", server.url("/").toString().trimEnd('/'), "u", "Demo")
            JellyfinEmbyClient(account, "secret", mediaHttp(), ClientIdentity("d", "i", "v")).resume()
            assertNull(other.takeRequest().getHeader("X-Emby-Authorization"))
        }
    }
    @Test fun itemsByIdAsksOnceAndSkipsAnEmptyList() = runTest {
        assertEquals(emptyList<MediaItem>(), client().items(emptyList()))
        server.enqueue(MockResponse().setBody("""{"Items":[{"Id":"s1","Type":"Series","Name":"Kystvakta"}]}"""))
        assertEquals(listOf("s1"), client().items(listOf("s1")).map { it.id })
        assertTrue(server.takeRequest().path!!.startsWith("/Items?userId=u&Ids=s1&"))
        assertEquals(1, server.requestCount)
    }
    @Test fun sendsTokenHeader() = runTest {
        server.enqueue(MockResponse().setBody("[]")); client(ServerKind.EMBY).latest(Endpoint.LATEST_MOVIES)
        assertTrue(server.takeRequest().getHeader("X-Emby-Authorization")!!.contains("Token=\"tok\""))
    }
}
