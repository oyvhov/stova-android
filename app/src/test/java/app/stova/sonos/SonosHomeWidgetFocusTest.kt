package app.stova.sonos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.TV_QUALIFIERS
import app.stova.apps.AppArtCache
import app.stova.apps.InstalledApp
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.theme.StovaTheme
import app.stova.core.theme.stovaMotion
import app.stova.home.HomeContent
import app.stova.home.HomeRowKey
import app.stova.home.HomeRowUi
import app.stova.navigation.Destination
import app.stova.navigation.LocalPageOpenContext
import app.stova.navigation.LocalPageReturnFocus
import app.stova.navigation.PageOpenContext
import app.stova.navigation.PageTrail
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

    @Test fun openingFromWidgetCapturesItsPositionAndBackRestoresFocus() {
        val room = sonosPreviewRooms().first()
        val openContext = PageOpenContext()
        var trail by mutableStateOf(PageTrail())
        compose.setContent {
            StovaTheme(reduceMotion = true) {
                val input = androidx.compose.ui.platform.LocalInputModeManager.current
                SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                CompositionLocalProvider(LocalPageOpenContext provides openContext, LocalPageReturnFocus provides trail.returnFocus) {
                    if (trail.current.destination == Destination.HOME) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                            SonosNowPlaying(room, room, false, false,
                                { trail = trail.open(Destination.SONOS, openContext.opening) }, {})
                        }
                    }
                }
            }
        }
        val title = compose.onNode(hasText(room.title!!) and hasClickAction())
        title.requestFocus()
        key(Key.DirectionCenter)
        compose.runOnIdle {
            assertTrue(trail.current.opening.focusKey == "sonos-widget")
            assertTrue(trail.current.opening.origin != TransformOrigin.Center)
            trail = trail.back()
        }
        title.assertIsFocused()
    }

    @Test fun decodedCoverRemainsVisibleAndMovesInBothDirections() {
        val room = sonosPreviewRooms().first()
        val path = checkNotNull(room.artwork)
        val covers = SonosCovers()
        val bitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        covers.store(path, bitmap.asImageBitmap())
        var player by mutableStateOf(false)
        lateinit var window: android.view.View
        compose.setContent {
            val view = LocalView.current
            SideEffect { window = view.rootView }
            StovaTheme {
                val motion = stovaMotion
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    val shared = this
                    AnimatedContent(player, transitionSpec = {
                        (fadeIn(motion.enter()) togetherWith fadeOut(motion.exit())).using(null)
                    }) { showingPlayer ->
                        val visibility = this
                        CompositionLocalProvider(LocalSonosCovers provides covers,
                            LocalSonosCoverModifier provides { shared.sonosCoverMotion(it, visibility) }) {
                            val load: SonosArtLoader = { art, modifier, ready, failed ->
                                SonosCoverImage(art, null, modifier, ready, failed)
                            }
                            Box(Modifier.fillMaxSize(), contentAlignment = if (showingPlayer) Alignment.Center else Alignment.TopStart) {
                                if (showingPlayer) SonosCover(path, Modifier.size(150.dp), load)
                                else SonosNowPlaying(room, room, false, false, {}, {}, loadArtwork = load)
                            }
                        }
                    }
                }
            }
        }
        fun coverX(): Float = compose.runOnIdle {
            val pixels = android.graphics.Bitmap.createBitmap(window.width, window.height, android.graphics.Bitmap.Config.ARGB_8888)
            window.draw(android.graphics.Canvas(pixels))
            var count = 0
            var total = 0L
            for (y in 0 until pixels.height step 6) for (x in 0 until pixels.width step 6) {
                val pixel = pixels.getPixel(x, y)
                if (pixel == android.graphics.Color.MAGENTA) { count++; total += x }
            }
            pixels.recycle()
            assertTrue("The decoded cover is visible", count > 0)
            total.toFloat() / count
        }
        val start = coverX()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { player = true }
        compose.mainClock.advanceTimeBy(96)
        val opening = coverX()
        compose.mainClock.advanceTimeBy(320)
        val end = coverX()
        assertTrue("The cover travels from the widget to the player", start < opening && opening < end)
        compose.runOnIdle { player = false }
        val closing = List(6) {
            compose.mainClock.advanceTimeBy(32)
            coverX()
        }
        assertTrue("The cover travels back to the widget ($end → $closing → $start)",
            closing.any { start + 10 < it && it < end - 10 })
    }

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
