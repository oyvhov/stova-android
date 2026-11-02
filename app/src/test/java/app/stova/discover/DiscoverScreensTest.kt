package app.stova.discover

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Test fun discoverRows() {
        assumeScreensEnabled()
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                DiscoverContent(DiscoverState(SeerrAccess.PERSONAL, rows, emptyList(), loading = false, failed = false), {}, {}, {}, {})
            }
        }
        compose.saveScreen("discover-rows")
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
}
