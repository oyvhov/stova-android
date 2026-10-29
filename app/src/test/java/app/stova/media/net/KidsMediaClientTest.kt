package app.stova.media.net

import app.stova.media.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class KidsMediaClientTest {
    @get:Rule val server = MockWebServer()
    private fun client(kind: ServerKind, clock: java.time.Clock = java.time.Clock.systemUTC()) = JellyfinEmbyClient(
        MediaAccount("a", "p", kind, "s", "Server", server.url("/").toString().trimEnd('/'), "u", "Barn"),
        "token", OkHttpClient(), ClientIdentity("d", "i", "v"), clock)

    @Test fun favouritesUseUserFilterRecentOrderAndTwentyCardsOnBothServers() = runTest {
        for (kind in ServerKind.entries) {
            if (kind == ServerKind.JELLYFIN) server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""[{"Id":"f","Name":"Film","Type":"Movie","UserData":{"IsFavorite":true}}]"""))
            assertEquals("f", client(kind).latest(Endpoint.FAVORITES).single().id)
            if (kind == ServerKind.JELLYFIN) assertEquals("/Items", server.takeRequest().requestUrl!!.encodedPath)
            val query = server.takeRequest().requestUrl!!
            assertEquals("/Users/u/Items", query.encodedPath)
            assertEquals("Movie,Series", query.queryParameter("IncludeItemTypes"))
            assertEquals("IsFavorite", query.queryParameter("Filters"))
            assertEquals("DatePlayed,DateCreated", query.queryParameter("SortBy"))
            assertEquals("Descending", query.queryParameter("SortOrder"))
            assertEquals("20", query.queryParameter("Limit"))
            assertTrue(query.queryParameter("Fields")!!.split(',').contains("DateCreated"))
        }
    }

    @Test fun allPagesAreAggregatedBeforeRankingAndSeriesUseTheirOwnPosterOnBothServers() = runTest {
        for (kind in ServerKind.entries) {
            // A short first page with a total still requires another page (server-side page caps).
            if (kind == ServerKind.JELLYFIN) server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""{"TotalRecordCount":3,"Items":[
                {"Id":"e1","Type":"Episode","SeriesId":"s1","UserData":{"PlayCount":4}},
                {"Id":"m1","Type":"Movie","UserData":{"PlayCount":7}}]}"""))
            if (kind == ServerKind.JELLYFIN) server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""{"TotalRecordCount":3,"Items":[
                {"Id":"e2","Type":"Episode","SeriesId":"s1","UserData":{"PlayCount":4}}]}"""))
            if (kind == ServerKind.JELLYFIN) server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setBody("""{"Items":[
                {"Id":"m1","Type":"Movie","Name":"Film"},
                {"Id":"s1","Type":"Series","Name":"Serie","ImageTags":{"Primary":"series-poster"}}]}"""))
            val items = client(kind).latest(Endpoint.MOST_PLAYED)
            assertEquals(listOf("s1", "m1"), items.map { it.id })
            assertEquals(ItemType.SERIES, items.first().type)
            assertTrue(items.first().art.poster!!.contains("/Items/s1/Images/Primary?"))
            assertTrue(items.first().art.poster!!.contains("series-poster"))
            for (start in listOf("0", "2")) {
                if (kind == ServerKind.JELLYFIN) assertEquals("/Items", server.takeRequest().requestUrl!!.encodedPath)
                val query = server.takeRequest().requestUrl!!
                assertEquals("/Users/u/Items", query.encodedPath)
                assertEquals("Movie,Episode", query.queryParameter("IncludeItemTypes"))
                assertEquals("IsPlayed", query.queryParameter("Filters"))
                assertEquals("true", query.queryParameter("EnableUserData"))
                assertEquals(start, query.queryParameter("StartIndex"))
            }
            if (kind == ServerKind.JELLYFIN) server.takeRequest()
            assertEquals("s1,m1", server.takeRequest().requestUrl!!.queryParameter("Ids"))
        }
    }

    @Test fun rankingLimitsAfterAggregationIgnoresInvalidCountsAndHydratesOnlyTwentyTitles() = runTest {
        val movies = (1..25).map { """{"Id":"m$it","Type":"Movie","UserData":{"PlayCount":$it}}""" }
        val episodes = (1..22).map { """{"Id":"e$it","Type":"Episode","SeriesId":"s","UserData":{"PlayCount":5}}""" }
        val ignored = listOf(
            """{"Id":"orphan","Type":"Episode","UserData":{"PlayCount":9000}}""",
            """{"Id":"zero","Type":"Movie","UserData":{"PlayCount":0}}""",
            """{"Id":"negative","Type":"Movie","UserData":{"PlayCount":-4}}""",
            """{"Id":"missing","Type":"Movie"}""",
            """{"Id":"folder","Type":"Series","UserData":{"PlayCount":9000}}""",
            """{"Id":"m25","Type":"Movie","UserData":{"PlayCount":9000}}""")
        val records = movies + episodes + ignored
        val expected = listOf("s") + (25 downTo 7).map { "m$it" }
        server.enqueue(MockResponse().setBody("""{"TotalRecordCount":${records.size},"Items":[${records.joinToString()}]}"""))
        val cards = expected.reversed().map { """{"Id":"$it","Name":"$it","Type":"${if (it == "s") "Series" else "Movie"}"}""" }
        server.enqueue(MockResponse().setBody("[${cards.joinToString()}]"))
        assertEquals(expected, client(ServerKind.EMBY).latest(Endpoint.MOST_PLAYED).map { it.id })
        server.takeRequest()
        assertEquals(expected.joinToString(","), server.takeRequest().requestUrl!!.queryParameter("Ids"))
        assertEquals(2, server.requestCount)
    }

    @Test fun missingTotalPaginatesUntilEmptyAndEmptyHistoryNeverFetchesArtwork() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        assertTrue(client(ServerKind.EMBY).latest(Endpoint.MOST_PLAYED).isEmpty())
        assertEquals(1, server.requestCount)
        server.takeRequest()
        server.enqueue(MockResponse().setBody("""[{"Id":"m","Type":"Movie","UserData":{"PlayCount":1}}]"""))
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setBody("""[{"Id":"m","Type":"Movie","Name":"Film"}]"""))
        assertEquals("m", client(ServerKind.EMBY).latest(Endpoint.MOST_PLAYED).single().id)
        server.takeRequest()
        assertEquals("1", server.takeRequest().requestUrl!!.queryParameter("StartIndex"))
        server.takeRequest()
    }

    @Test fun repeatedPagesFailInsteadOfLoopingOrMultiplyingCounts() = runTest {
        val page = """[{"Id":"m","Type":"Movie","UserData":{"PlayCount":1}}]"""
        repeat(2) { server.enqueue(MockResponse().setBody(page)) }
        try {
            client(ServerKind.EMBY).latest(Endpoint.MOST_PLAYED)
            fail("A non-advancing page must fail the optional row")
        } catch (_: IllegalStateException) { }
        assertEquals(2, server.requestCount)
    }

    @Test fun optionalEndpointsOnlyFallBackOn404() = runTest {
        for (endpoint in listOf(Endpoint.FAVORITES, Endpoint.MOST_PLAYED)) {
            for (code in listOf(401, 500)) {
                val before = server.requestCount
                server.enqueue(MockResponse().setResponseCode(code))
                try {
                    client(ServerKind.JELLYFIN).latest(endpoint)
                    fail("HTTP $code must reach the row's failure boundary")
                } catch (error: MediaError) {
                    if (code == 401) assertTrue(error is MediaError.Unauthorized)
                    else assertEquals(500, (error as MediaError.Server).code)
                }
                assertEquals(before + 1, server.requestCount)
            }
        }
    }

    @Test fun clientUsesInjectedClockForNewEpisodes() = runTest {
        server.enqueue(MockResponse().setBody("""[{"Id":"e","Type":"Episode","Name":"Ny","DateCreated":"2026-10-28T12:00:00Z","UserData":{"Played":false}}]"""))
        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-29T12:00:00Z"), java.time.ZoneOffset.UTC)
        assertTrue(client(ServerKind.EMBY, clock).nextUp().single().isNew)
    }

    @Test fun equalPlayCountsUseActualLastPlayedInstantThenStableId() {
        val page = playbackPage("""[
            {"Id":"a","Type":"Movie","UserData":{"PlayCount":2,"LastPlayedDate":"2026-10-29T13:00:00+02:00"}},
            {"Id":"b","Type":"Movie","UserData":{"PlayCount":2,"LastPlayedDate":"2026-10-29T12:00:00Z"}},
            {"Id":"c","Type":"Movie","UserData":{"PlayCount":2,"LastPlayedDate":"2026-10-29T12:00:00Z"}}
        ]""")
        val ranking = PlaybackRanking()
        ranking.add(page.items)
        assertEquals(listOf("b", "c", "a"), ranking.topIds(20))
    }
}
