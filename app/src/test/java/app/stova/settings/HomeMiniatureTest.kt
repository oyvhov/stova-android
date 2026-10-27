package app.stova.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.*
import app.stova.core.theme.AvatarColor
import app.stova.core.theme.StovaTheme
import app.stova.core.ui.StovaButton
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.media.MediaRowKind
import app.stova.media.model.*
import app.stova.profiles.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeMiniatureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val account = MediaAccount("a", "p", ServerKind.JELLYFIN, "s", "Demo", "https://demo.invalid", "u", "U")
    private val rows = listOf(
        HomeRowUi.Apps(HomeRowKey.APPS, listOf(InstalledApp("demo/.Main", "demo", ".Main", "Min app", true))),
        HomeRowUi.Media(HomeRowKey.latestMovies("a"), account, MediaRowKind.LATEST_MOVIES,
            listOf(MediaItem("f", "a", ItemType.MOVIE, "Film")), false),
    )
    private var shown by mutableStateOf(true)

    private fun render() {
        compose.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            StovaTheme {
                val context = LocalContext.current
                Column {
                    StovaButton("Kontroll", {})
                    if (shown) HomeMiniature(AppearancePrefs(), Profile("p", "A", AvatarColor.LIME), rows, HomeRowKey.APPS, null,
                        AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), Modifier.width(320.dp), smartHome = true)
                }
            }
        }
    }

    @Test fun theSmallHomeIsTheWholeTvScreenScaledDown() {
        render()
        compose.onNodeWithTag("home-preview").assertWidthIsEqualTo(320.dp).assertHeightIsEqualTo(180.dp)
    }

    @Test fun focusNeverEntersTheSmallHome() {
        shown = false
        render()
        compose.onNodeWithText("Kontroll").performSemanticsAction(SemanticsActions.RequestFocus)
        // Home places focus on its start row as soon as its rows are laid out; this one must not.
        compose.runOnIdle { shown = true }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Kontroll").assertIsFocused()
        for (key in listOf(Key.DirectionDown, Key.DirectionRight)) {
            compose.onNodeWithText("Kontroll").performKeyInput { pressKey(key) }
            compose.onNodeWithText("Kontroll").assertIsFocused()
        }
    }

    @Test fun aScreenReaderHearsOnePictureNotEveryCard() {
        render()
        compose.onNodeWithContentDescription("Min app").assertDoesNotExist()
        compose.onNodeWithTag("home-preview").assertContentDescriptionEquals("Førehandsvising av Heim")
    }
}
