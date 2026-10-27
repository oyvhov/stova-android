package app.stova.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.*
import app.stova.media.*
import app.stova.media.model.*
import app.stova.media.ui.MediaCard
import app.stova.profiles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "U")
    private val artUrl by lazy {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        java.io.File(context.cacheDir, "appearance-card.png").also { file ->
            val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }.absolutePath
    }
    private val film = MediaItem("f", "a", ItemType.MOVIE, "Fjordlys", overview = "Ei ferd langs kysten.",
        genres = listOf("Drama"), art = ItemArt(wide = artUrl, poster = artUrl))

    @Test fun longWatchNextSubtitleStaysInsideItsScaledArtwork() {
        val subtitle = "Episode 12 – Ei svært lang skildring som aldri skal skyve neste kort ut av rada"
        var format by mutableStateOf(RowFormat.SMALL_WIDE)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f),
                LocalRowStyle provides RowStyle(format, ArtworkSize.SMALL), LocalArtworkScale provides .7f) {
                StovaTheme(appearance = AppearancePrefs(showTitles = true)) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { app.stova.watchnext.WatchNextCard(app.stova.watchnext.WatchNextEntry(1, "demo", "Serie", subtitle, artUrl, "", null, 0), {}, {}, Modifier.testTag("watch-art")) }
                        item { Box(Modifier.size(20.dp).testTag("next-art")) }
                    }
                }
            }
        }
        for (choice in listOf(RowFormat.SMALL_WIDE, RowFormat.ICON)) {
            compose.runOnIdle { format = choice }
            val artwork = compose.onNodeWithTag("watch-art").getUnclippedBoundsInRoot()
            val caption = compose.onNodeWithText(subtitle, useUnmergedTree = true).getUnclippedBoundsInRoot()
            val next = compose.onNodeWithTag("next-art").getUnclippedBoundsInRoot()
            assertTrue("Subtitle must end at the artwork edge", caption.right <= artwork.right)
            assertEquals(12f, (next.left - artwork.right).value, .5f)
        }
    }

    @Test fun headingsUseTheirOwnTextAndSourceCanBeHiddenWithoutHidingOfflineStatus() {
        val row=HomeRowUi.Media(HomeRowKey.latestMovies(account.id),account,MediaRowKind.LATEST_MOVIES,listOf(film),true)
        compose.setContent { StovaTheme { Column {
            CompositionLocalProvider(LocalRowStyle provides RowStyle(title="Mine appar",headingSize=HeadingSize.LARGE)) { RowHeader("Favorittappar") }
            CompositionLocalProvider(LocalRowStyle provides RowStyle(title="Filmhylla",showSource=false)) { app.stova.media.ui.MediaRowHeader(row) }
        } } }
        compose.onNodeWithText("Mine appar").assertIsDisplayed()
        compose.onNodeWithText("Filmhylla").assertIsDisplayed()
        compose.onNodeWithText("Demo").assertDoesNotExist()
        compose.onNodeWithText("Fråkopla").assertIsDisplayed()
    }

    @Test fun heroFactsCanBeHiddenIndependentlyOfTheTitleAndGenres() {
        var prefs by mutableStateOf(AppearancePrefs(heroTitle=HeroTitle.TITLE))
        val entry=SpotlightEntry(film.copy(year=2024,ratings=Ratings(8.4,96,"NO15")),account,true)
        compose.setContent { StovaTheme(appearance=prefs) { HeroPanel(entry,Modifier.height(240.dp)) } }
        compose.onNodeWithText("2024").assertExists()
        compose.onNodeWithText("15 år").assertExists()
        compose.onNodeWithText("Ny sesong").assertExists()
        compose.runOnIdle { prefs=prefs.copy(showHeroYear=false,showHeroAge=false,showHeroRatings=false,showNewSeason=false) }
        compose.onNodeWithText("2024").assertDoesNotExist()
        compose.onNodeWithText("15 år").assertDoesNotExist()
        compose.onNodeWithText("Ny sesong").assertDoesNotExist()
        compose.onNodeWithText("96",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Fjordlys").assertExists()
        compose.onNodeWithText("Drama",substring=true).assertExists()
    }

    @Test fun cardSizesTitlesAndMissingArt() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) { Row {
            MediaCard(film, false, {}, {}, {}, Modifier.testTag("wide"))
            MediaCard(film.copy(id = "p", title = "Plakat"), true, {}, {}, {}, Modifier.testTag("poster"))
        } } }
        compose.onNodeWithTag("wide").assertWidthIsEqualTo(224.dp)
        compose.onNodeWithText("Fjordlys").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fjordlys").assertExists()
        compose.runOnIdle { prefs = prefs.copy(cardSize = ArtworkSize.LARGE, showTitles = true) }
        compose.onNodeWithTag("wide").assertWidthIsEqualTo(296.dp)
        compose.onNodeWithTag("poster").assertWidthIsEqualTo((132f * 296 / 248).dp)
        compose.onNodeWithText("Fjordlys").assertExists()
        compose.runOnIdle { prefs = prefs.copy(cardSize = ArtworkSize.SMALL, showTitles = false) }
        compose.onNodeWithTag("wide").assertWidthIsEqualTo(200.dp)
    }

    @Test fun failedArtworkShowsTheTitleInsideThePlaceholder() {
        compose.setContent { StovaTheme(appearance = AppearancePrefs(showTitles = false)) {
            MediaCard(film.copy(art = ItemArt(wide = "file:///missing-stova-art.png")), false, {}, {}, {})
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Fjordlys").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Fjordlys").assertIsDisplayed()
    }

    @Test fun appStyleUsesSquareIconsAndProportionalSizes() {
        var prefs by mutableStateOf(AppearancePrefs(cardSize = ArtworkSize.MEDIUM, showTitles = false))
        compose.setContent { StovaTheme(appearance = prefs) {
            AppTile(InstalledApp("missing/.Main", "missing", ".Main", "App", true), AppArtCache(LocalContext.current), {}, {}, Modifier.testTag("app"))
        } }
        compose.onNodeWithTag("app").assertWidthIsEqualTo(248.dp).assertHeightIsEqualTo(139.5.dp)
        compose.onNodeWithText("App").assertIsDisplayed() // no artwork: title in placeholder
        compose.runOnIdle { prefs = prefs.copy(cardSize = ArtworkSize.SMALL, appStyle = AppStyle.ICON) }
        compose.onNodeWithTag("app").assertWidthIsEqualTo(112.5.dp).assertHeightIsEqualTo(112.5.dp)
    }

    @Test fun heroChoicesControlOverviewAndGenres() {
        var prefs by mutableStateOf(AppearancePrefs())
        compose.setContent { StovaTheme(appearance = prefs) { HeroPanel(SpotlightEntry(film, account, false)) } }
        compose.onNodeWithText("Film · Drama").assertIsDisplayed()
        compose.onNodeWithText(film.overview!!).assertDoesNotExist()
        compose.runOnIdle { prefs = prefs.copy(showOverview = true, showGenres = false) }
        compose.onNodeWithText(film.overview!!).assertIsDisplayed()
        compose.onNodeWithText("Film · Drama").assertDoesNotExist()
    }

    @Test fun homeUsesMovieStyleHeroSizeAndRowSpacing() {
        var prefs by mutableStateOf(AppearancePrefs(homeRows = HomeRows.SCROLLING, cardSize = ArtworkSize.MEDIUM, showTitles = false))
        val row = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, listOf(film), false)
        compose.setContent { StovaTheme(appearance = prefs, reduceMotion = true) {
            val ctx = LocalContext.current
            HomeContent(listOf(row), null, row.key, AppArtCache(ctx), PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {}, spotlight = SpotlightEntry(film, account, false))
        } }
        compose.onNodeWithContentDescription("Fjordlys").assertWidthIsEqualTo(132.dp)
        val before = compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot().top
        compose.runOnIdle { prefs = prefs.copy(movieStyle = MovieStyle.WIDE, heroSize = HeroSize.LARGE, rowSpacing = RowSpacing.AIRY) }
        compose.onNodeWithContentDescription("Fjordlys").assertWidthIsEqualTo(248.dp)
        val after = compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot().top
        assertTrue(after > before)
        val cardBottom = compose.onNodeWithContentDescription("Fjordlys").getUnclippedBoundsInRoot().bottom
        compose.runOnIdle { prefs = prefs.copy(rowSpacing = RowSpacing.TIGHT) }
        // The first row stays at the bottom under the big hero; the spacing it gives up goes to the hero.
        assertEquals(cardBottom.value, compose.onNodeWithContentDescription("Fjordlys").getUnclippedBoundsInRoot().bottom.value, 1f)
        assertEquals(after.value, compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot().top.value, 1f)
    }

    @Test fun largestPosterAndItsTitleFitBelowTheFixedHero() {
        val row = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, listOf(film), false)
        compose.setContent { StovaTheme(appearance = AppearancePrefs(cardSize = ArtworkSize.LARGE, showTitles = true, heroSize = HeroSize.LARGE), reduceMotion = true) {
            val ctx = LocalContext.current
            HomeContent(listOf(row), null, row.key, AppArtCache(ctx), PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {}, spotlight = SpotlightEntry(film, account, false))
        } }
        val card = compose.onNodeWithContentDescription("Fjordlys").getUnclippedBoundsInRoot()
        assertTrue("Full poster and title must fit: $card", card.bottom <= 540.dp)
        val header = compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot()
        assertTrue(compose.onNodeWithText("Film · Drama").getUnclippedBoundsInRoot().bottom <= header.top)
    }

    @Test fun largePosterRowOverrideKeepsTheBottomTvSafeEdge() {
        val row = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, listOf(film), false)
        compose.setContent { StovaTheme(reduceMotion = true, appearance = AppearancePrefs(homeRows = HomeRows.SCROLLING)) {
            val ctx = LocalContext.current
            HomeContent(listOf(row), null, row.key, AppArtCache(ctx), PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {},
                topBar = { app.stova.navigation.TopBar(Profile("p", "Demo", AvatarColor.LIME), {}, {}) },
                spotlight = SpotlightEntry(film, account, false),
                homePrefs = HomePrefs(rowFormats = mapOf(row.key to RowStyle(RowFormat.POSTER, ArtworkSize.LARGE))))
        } }
        val art = compose.onNodeWithContentDescription("Fjordlys").getUnclippedBoundsInRoot()
        assertTrue("Whole large poster must remain inside the 48 dp safe edge: $art", art.bottom <= 492.dp)
        val heading = compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot()
        assertTrue(compose.onNodeWithText("Film · Drama").getUnclippedBoundsInRoot().bottom <= heading.top)
    }

    @Test fun denseHeroWithLongFactsAndOverviewStaysAboveTheRowHeader() {
        val item = film.copy(title = "Ein svært lang tittel om ei ferd gjennom fjordane og eit ukjent landskap langt mot nord",
            year = 2024, ratings = Ratings(7.8, 91, "TV-MA"), genres = listOf("Science fiction", "Drama", "Eventyr", "Thriller", "Mystery", "Adventure"), overview = "Ei lang omtale av ei ferd gjennom fjordar og opne landskap. ".repeat(5))
        val row = HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES, listOf(item), false)
        compose.setContent { StovaTheme(appearance = AppearancePrefs(cardSize = ArtworkSize.LARGE, showTitles = true, heroSize = HeroSize.LARGE, showOverview = true), reduceMotion = true) {
            val ctx = LocalContext.current
            HomeContent(listOf(row), null, row.key, AppArtCache(ctx), PackageManagerAppCatalog(ctx, emptySet()), {}, {}, {}, {},
                topBar = { app.stova.navigation.TopBar(Profile("p", "Demo", AvatarColor.LIME), {}, {}) },
                spotlight = SpotlightEntry(item, account, true))
        } }
        val overviewNode = compose.onNodeWithText(item.overview!!).assertIsDisplayed()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        overviewNode.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        // A large hero tells three lines of the story (a compact one two).
        assertEquals(3, layouts.single().lineCount)
        assertTrue("All three overview lines must fit", layouts.single().getLineBottom(2) <= layouts.single().size.height + 0.5f)
        val overview = overviewNode.getUnclippedBoundsInRoot()
        val header = compose.onNodeWithText("Nyleg lagt til · Filmar").getUnclippedBoundsInRoot()
        assertTrue("Hero overview must not cover row heading: $overview vs $header", overview.bottom <= header.top)
    }

    @Test fun accentAndCornersReachTheTheme() {
        var prefs by mutableStateOf(AppearancePrefs(seasonChoice = SeasonChoice.NONE))
        var primary = Color.Unspecified
        var radius = 0f
        compose.setContent { StovaTheme(appearance = prefs) {
            primary = MaterialTheme.colorScheme.primary
            radius = with(androidx.compose.ui.platform.LocalDensity.current) { MaterialTheme.shapes.medium.topStart.toPx(androidx.compose.ui.geometry.Size(100f,100f), this).toDp().value }
        } }
        Accent.entries.forEach { accent ->
            compose.runOnIdle { prefs = prefs.copy(accent = accent) }
            compose.waitForIdle()
            assertEquals(accent.color, primary)
        }
        assertEquals(20f, radius, 0.01f)
        compose.runOnIdle { prefs = prefs.copy(cornerRadius = CornerRadius.SHARP) }
        compose.waitForIdle()
        assertEquals(4f, radius, 0.01f)
    }
}
