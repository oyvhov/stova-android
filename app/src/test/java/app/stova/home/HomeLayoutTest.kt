package app.stova.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.media.MediaRowKind
import app.stova.media.SpotlightEntry
import app.stova.media.model.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real text metrics: the hero's height only shows its effect on the rows with native graphics. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
    private val rows = listOf(HomeRowUi.Media(HomeRowKey.continueWatching("a"), account, MediaRowKind.CONTINUE,
        listOf(MediaItem("Fyrste-id", "a", ItemType.MOVIE, "Fyrste", progress = 0.4f)), false))

    @Test fun rowsStayPutWhenTheSpotlightChanges() {
        val short = SpotlightEntry(MediaItem("f1", "a", ItemType.MOVIE, "Kort"), account, newSeason = false)
        val long = SpotlightEntry(MediaItem("s1", "a", ItemType.SERIES, "Ein lang serietittel som gjerne går over to linjer",
            year = 2022, overview = "Ein lang omtale. ".repeat(20), ratings = Ratings(8.0, 91, "TV-MA")), account, newSeason = true)
        var spotlight by mutableStateOf(short)
        var prefs by mutableStateOf(app.stova.profiles.AppearancePrefs())
        compose.setContent {
            val context = LocalContext.current
            StovaTheme(reduceMotion = true, appearance = prefs) {
                HomeContent(rows, null, rows.first().key, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                    {}, {}, {}, {}, spotlight = spotlight)
            }
        }
        app.stova.profiles.HeroSize.entries.forEach { size ->
            compose.runOnIdle { prefs = prefs.copy(heroSize = size); spotlight = short }
            val before = compose.onNodeWithText("Fyrste").getUnclippedBoundsInRoot().top
            compose.runOnIdle { spotlight = long }
            compose.waitForIdle()
            assertEquals(before, compose.onNodeWithText("Fyrste").getUnclippedBoundsInRoot().top)
        }
    }
}
