package app.stova.sonos

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.InstalledApp
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.home.HomeContent
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.profiles.AppearancePrefs
import app.stova.profiles.HomeRows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SonosHomeWidgetFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun widgetSitsAboveAppsAndDpadVisitsItsTwoControlsInScrollingHome() = checkWidget(HomeRows.SCROLLING)
    @Test fun widgetSitsAboveAppsAndDpadVisitsItsTwoControlsInDeckHome() = checkWidget(HomeRows.ONE)

    private fun checkWidget(mode: HomeRows) {
        val room = sonosPreviewRooms().first()
        val app = InstalledApp("demo/.Main", "demo", ".Main", "Min app", true)
        var show by mutableStateOf(true)
        compose.setContent { StovaTheme(reduceMotion = true, appearance = AppearancePrefs(homeRows = mode)) {
            val context = LocalContext.current
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            HomeContent(listOf(HomeRowUi.Apps(HomeRowKey.APPS, listOf(app))), null, HomeRowKey.APPS,
                AppArtCache(context), PackageManagerAppCatalog(context, emptySet()), {}, {}, {}, {},
                aboveApps = if (!show) null else { modifier -> SonosNowPlaying(room, room, false, false, {}, {}, modifier) })
        } }
        val tile = compose.onNodeWithContentDescription("Min app")
        val title = compose.onNode(hasText(room.title!!) and hasClickAction())
        assertTrue(title.getUnclippedBoundsInRoot().bottom < tile.getUnclippedBoundsInRoot().top)
        tile.requestFocus()
        key(Key.DirectionUp)
        title.assertIsFocused()
        key(Key.DirectionRight)
        compose.onNodeWithContentDescription("Pause").assertIsFocused()
        key(Key.DirectionDown)
        tile.assertIsFocused()
        key(Key.DirectionUp)
        title.assertIsFocused()
        compose.runOnIdle { show = false }
        title.assertDoesNotExist()
        tile.assertIsFocused()
    }

    private fun key(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }
}
