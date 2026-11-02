package app.stova.discover

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.assumeScreensEnabled
import app.stova.core.theme.StovaTheme
import app.stova.saveScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Oppdag pages drawn to `app/build/screens/` with `-Pstova.screens=true`; they render, they do not assert. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiscoverScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun title(type: DiscoverType, tmdbId: Int, name: String, year: Int, status: SeerrStatus = SeerrStatus.UNKNOWN,
        community: Double? = null, genres: List<String> = emptyList(), overview: String? = null) =
        DiscoverTitle(type, tmdbId, name, year, overview, null, null, status, community, genres)

    private val rows = listOf(
        DiscoverRow(DiscoverRowKind.TRENDING, listOf(
            title(DiscoverType.MOVIE, 693134, "Dune: Part Two", 2024, SeerrStatus.AVAILABLE, 8.2, listOf("Science fiction", "Eventyr", "Drama"),
                "Paul Atreides går saman med Chani og fremenane for å hemne seg på dei som øydela familien hans."),
            title(DiscoverType.SERIES, 1399, "Game of Thrones", 2011, SeerrStatus.PARTLY, 8.5),
            title(DiscoverType.MOVIE, 949, "Heat", 1995, SeerrStatus.PENDING, 8.3),
            title(DiscoverType.MOVIE, 603, "The Matrix", 1999, community = 8.2),
            title(DiscoverType.MOVIE, 157336, "Interstellar", 2014, community = 8.4),
            title(DiscoverType.MOVIE, 27205, "Inception", 2010, community = 8.4),
        )),
        DiscoverRow(DiscoverRowKind.UPCOMING_MOVIES, listOf(
            title(DiscoverType.MOVIE, 696506, "Mickey 17", 2025), title(DiscoverType.MOVIE, 950396, "The Gorge", 2025),
            title(DiscoverType.MOVIE, 1100782, "Smile 2", 2025), title(DiscoverType.MOVIE, 822119, "Captain America", 2025),
        )),
        DiscoverRow(DiscoverRowKind.UPCOMING_SERIES, listOf(
            title(DiscoverType.SERIES, 95396, "Severance", 2025), title(DiscoverType.SERIES, 100088, "The Last of Us", 2025),
        )),
    )

    private fun request(type: DiscoverType, tmdbId: Int, name: String, stage: RequestStage, percent: Int? = null) =
        DiscoverRequest(type, tmdbId, name, null, null, stage, percent, emptyList(), tmdbId, is4k = false)

    private val requests = listOf(
        request(DiscoverType.MOVIE, 950396, "The Gorge", RequestStage.DOWNLOADING, 42),
        request(DiscoverType.SERIES, 83867, "Andor", RequestStage.REQUESTED),
        request(DiscoverType.MOVIE, 426063, "Nosferatu", RequestStage.IMPORTING),
        request(DiscoverType.MOVIE, 1064213, "Anora", RequestStage.AVAILABLE),
    )

    @Test fun discoverRows() {
        assumeScreensEnabled()
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                DiscoverContent(DiscoverState(SeerrAccess.PERSONAL, rows, requests, loading = false, failed = false), {}, {}, {}, {})
            }
        }
        compose.saveScreen("discover-rows")
    }

    // Scrolled to «Dine førespurnader»: each card says how far it has come, and only the download draws a bar.
    // Focus does not reach the window in this graphics mode, so the rows are scrolled instead of walked.
    @Test fun discoverRequests() {
        assumeScreensEnabled()
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                DiscoverContent(DiscoverState(SeerrAccess.PERSONAL, rows, requests, loading = false, failed = false), {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("discover-rows").performScrollToIndex(1)
        compose.saveScreen("discover-requests")
    }

    @Test fun discoverTitle() {
        assumeScreensEnabled()
        val heat = title(DiscoverType.MOVIE, 949, "Heat", 1995, community = 8.3, genres = listOf("Action", "Krim", "Drama"),
            overview = "Ein meistertjuv og gjengen hans planlegg eit siste stort kupp i Los Angeles, medan ein besett politimann " +
                "kjem stadig nærare. Dei to mennene er like på fleire måtar enn nokon av dei vil innrømme.")
        val details = DiscoverDetails(heat, "Ein Los Angeles-kriminalsaga.", 170, 88, canRequest = true, requestId = null,
            requestStage = null, trailer = null, seasons = emptyList())
        val related = listOf(
            DiscoverRow(DiscoverRowKind.SIMILAR, listOf(title(DiscoverType.MOVIE, 8195, "Ronin", 1998), title(DiscoverType.MOVIE, 949001, "Collateral", 2004),
                title(DiscoverType.MOVIE, 1422, "The Departed", 2006), title(DiscoverType.MOVIE, 155, "The Dark Knight", 2008))),
            DiscoverRow(DiscoverRowKind.RECOMMENDED, listOf(title(DiscoverType.MOVIE, 680, "Pulp Fiction", 1994),
                title(DiscoverType.MOVIE, 769, "GoodFellas", 1990))),
        )
        compose.setContent {
            StovaTheme(reduceMotion = true) { DiscoverTitleContent(heat, details, related, SeerrAccess.PERSONAL, busy = false, {}, {}, {}, {}) }
        }
        compose.saveScreen("discover-title")
    }

    // A series' seasons to ask for: the two Seerr can still get chosen, the one in the library saying so, the specials last.
    @Test fun discoverRequestSheet() {
        assumeScreensEnabled()
        val severance = title(DiscoverType.SERIES, 95396, "Severance", 2022, SeerrStatus.PARTLY, 8.4)
        val seasons = listOf(DiscoverSeason(0, "Spesialar", 3, SeerrStatus.UNKNOWN, null), DiscoverSeason(1, "Sesong 1", 9, SeerrStatus.AVAILABLE, null),
            DiscoverSeason(2, "Sesong 2", 10, SeerrStatus.UNKNOWN, null), DiscoverSeason(3, "Sesong 3", 10, SeerrStatus.UNKNOWN, null))
        val details = DiscoverDetails(severance, null, null, null, canRequest = true, requestId = null, requestStage = null, trailer = null, seasons = seasons)
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                DiscoverTitleContent(severance, details, emptyList(), SeerrAccess.PERSONAL, busy = false, {}, {}, {}, {})
                RequestSheet(details, onSend = {}, onDismiss = {})
            }
        }
        compose.saveScreen("discover-request-sheet", android.view.inspector.WindowInspector.getGlobalWindowViews().last())
    }

    // What Seerr found for «the», six posters across, with «Hent fleire» last as Seerr has another page. The page draws
    // no background of its own: it stands on the shell's, as Apps does.
    @Test fun discoverSearch() {
        assumeScreensEnabled()
        val found = rows.flatMap { it.items }.take(9)
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                    DiscoverSearchContent("the", DiscoverPage(found, 1, hasMore = true), loading = false, {}, {}, {})
                }
            }
        }
        compose.saveScreen("discover-search")
    }

    @Test fun discoverWithdrawSheet() {
        assumeScreensEnabled()
        compose.setContent { StovaTheme(reduceMotion = true) { WithdrawSheet("The Gorge", onConfirm = {}, onDismiss = {}) } }
        compose.saveScreen("discover-withdraw-sheet", android.view.inspector.WindowInspector.getGlobalWindowViews().last())
    }
}
