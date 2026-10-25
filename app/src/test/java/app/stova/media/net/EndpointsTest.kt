package app.stova.media.net
import app.stova.media.model.*
import org.junit.*
import org.junit.Assert.*
import okhttp3.*
import okhttp3.mockwebserver.*
import kotlinx.coroutines.test.runTest
class EndpointsTest {
    @Test fun jellyfinTriesModernThenLegacy() = assertEquals(
        listOf("UserItems/Resume?userId=u&MediaTypes=Video&$ITEM_QUERY", "Users/u/Items/Resume?MediaTypes=Video&$ITEM_QUERY"),
        endpointCandidates(ServerKind.JELLYFIN, Endpoint.RESUME, "u"))
    @Test fun embyUsesUserInPathOnly() = assertEquals(1, endpointCandidates(ServerKind.EMBY, Endpoint.LATEST_MOVIES, "u").size)
    @Test fun spotlightAsksTheWholeLibraryForFilmsAndSeriesWithArt() = assertEquals(
        listOf("Items?userId=u&IncludeItemTypes=Movie,Series&Recursive=true&SortBy=Random&ImageTypes=Backdrop&$ITEM_FIELDS&Limit=30",
            "Users/u/Items?IncludeItemTypes=Movie,Series&Recursive=true&SortBy=Random&ImageTypes=Backdrop&$ITEM_FIELDS&Limit=30"),
        endpointCandidates(ServerKind.JELLYFIN, Endpoint.SPOTLIGHT, "u"))
    @Test fun newSeasonsAreTheNewestPremieredSeasons() = assertEquals(
        listOf("Users/u/Items?IncludeItemTypes=Season&Recursive=true&SortBy=PremiereDate&SortOrder=Descending&$ITEM_FIELDS&Limit=20"),
        endpointCandidates(ServerKind.EMBY, Endpoint.NEW_SEASONS, "u"))
    @Test fun headerPerKind() {
        val id = ClientIdentity("SHIELD Android TV", "dev1", "0.1.0")
        assertEquals("X-Emby-Authorization", authHeader(ServerKind.EMBY, id, "t").first)
        assertEquals("""MediaBrowser Client="Stova", Device="SHIELD Android TV", DeviceId="dev1", Version="0.1.0", Token="t"""", authHeader(ServerKind.JELLYFIN, id, "t").second)
    }
}
