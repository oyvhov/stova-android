package app.stova.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeReadyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeAppMenuKeyCallsTheSameAppActionAsLongOk() {
        val app = app.stova.apps.InstalledApp("demo/.Main", "demo", ".Main", "Demo", true)
        var selected: app.stova.apps.InstalledApp? = null
        compose.setContent {
            val context = LocalContext.current
            StovaTheme(reduceMotion = true) {
                HomeContent(listOf(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app))), null, HomeRowKey.APPS,
                    AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), {}, {}, {}, {},
                    onAppLongClick = { selected = it })
            }
        }
        compose.onNodeWithText("Demo").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Demo").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Menu) }
        org.junit.Assert.assertEquals(app, selected)
    }

    @Test fun readyAppearsWithRenderedRowsAndDisappearsWhileLoading() {
        val rows = mutableStateOf<List<HomeRowUi>?>(null)
        compose.setContent {
            val context = LocalContext.current
            StovaTheme {
                HomeContent(rows.value, null, null, AppArtCache(context), PackageManagerAppCatalog(context, emptySet()),
                    {}, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("home-ready").assertDoesNotExist()
        val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "Demo")
        compose.runOnIdle {
            rows.value = listOf(HomeRowUi.Media(HomeRowKey.latestMovies("a"), account,
                MediaRowKind.LATEST_MOVIES, listOf(MediaItem("m", "a", ItemType.MOVIE, "Testfilm")), false))
        }
        compose.onNodeWithContentDescription("home-ready").assertIsDisplayed()
        compose.onNodeWithText("Testfilm").assertIsDisplayed()
        compose.runOnIdle { rows.value = null }
        compose.onNodeWithContentDescription("home-ready").assertDoesNotExist()
        // A fully loaded empty home is also ready; it must not hang automation forever.
        compose.runOnIdle { rows.value = emptyList() }
        compose.onNodeWithContentDescription("home-ready").assertIsDisplayed()
    }
}

